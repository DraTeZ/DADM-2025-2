package com.example.coberturamovil

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.coberturamovil.databinding.ActivityMainBinding
import com.google.android.material.chip.Chip
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONException
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.generationConfig

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var coberturaAdapter: CoberturaAdapter

    private lateinit var generativeModel: GenerativeModel

    private val apiBaseUrl = "https://www.datos.gov.co/resource/9mey-c8s8.json"


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        generativeModel = GenerativeModel(
            modelName = "gemini-2.5-flash",
            apiKey = "AIzaSyBOzmPSFqCKJj-7vaAEDc86m69whQ1Qh3U"
        )

        setupRecyclerView()
        setupListeners()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        coberturaAdapter = CoberturaAdapter()
        binding.recyclerViewResultados.apply {
            adapter = coberturaAdapter
            layoutManager = LinearLayoutManager(this@MainActivity)
        }
    }

    private fun setupListeners() {
        binding.btnBuscar.setOnClickListener {
            val departamento = binding.etDepartamento.text.toString()
            val municipio = binding.etMunicipio.text.toString()
            val operador = binding.etOperador.text.toString()

            val chipIdSeleccionado = binding.chipGroupTecnologia.checkedChipId
            val tecnologia = if (chipIdSeleccionado != View.NO_ID) {
                findViewById<Chip>(chipIdSeleccionado).text.toString()
            } else {
                "Todas"
            }

            if (departamento.isNotEmpty()) {
                buscarCobertura(departamento, municipio, operador, tecnologia)
            } else {
                Toast.makeText(this, "El campo 'Departamento' es obligatorio", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnAbrirChatIA.setOnClickListener {
            showGeminiDialog()
        }
    }

    private fun observeViewModel() {
        binding.btnAbrirChatIA.isEnabled = true
    }

    private fun buscarCobertura(departamento: String, municipio: String, operador: String, tecnologiaFiltro: String) {
        lifecycleScope.launch {
            binding.progressBar.visibility = View.VISIBLE
            binding.tvEstadoResultados.visibility = View.GONE
            coberturaAdapter.submitList(emptyList()) // Limpiar

            try {
                val url = construirUrlQuery(departamento, municipio, operador)
                Log.d("MainActivity", "Consultando: $url")

                val jsonRespuesta = withContext(Dispatchers.IO) {
                    realizarSolicitudHttp(url)
                }

                val listaParseada = withContext(Dispatchers.Default) {
                    parsearRespuestaJson(jsonRespuesta, tecnologiaFiltro)
                }

                coberturaAdapter.submitList(listaParseada)
                binding.progressBar.visibility = View.GONE

                if (listaParseada.isEmpty()) {
                    binding.tvEstadoResultados.text = "No se encontraron resultados."
                    binding.tvEstadoResultados.visibility = View.VISIBLE
                }

            } catch (e: Exception) {
                Log.e("MainActivity", "Error en buscarCobertura", e)
                binding.progressBar.visibility = View.GONE
                binding.tvEstadoResultados.text = "Error al conectar: ${e.message}"
                binding.tvEstadoResultados.visibility = View.VISIBLE
            }
        }
    }

    private fun construirUrlQuery(departamento: String, municipio: String, operador: String): String {
        val whereClauses = mutableListOf<String>()
        fun encode(s: String) = URLEncoder.encode(s.uppercase(), "UTF-8")

        if (departamento.isNotEmpty()) whereClauses.add("contains(departamento, '${departamento.uppercase()}')")
        if (municipio.isNotEmpty()) whereClauses.add("contains(municipio, '${municipio.uppercase()}')")
        if (operador.isNotEmpty()) whereClauses.add("contains(proveedor, '${operador.uppercase()}')")

        val params = mutableListOf<String>()
        if (whereClauses.isNotEmpty()) {
            val whereQuery = whereClauses.joinToString(" AND ")
            params.add("\$where=${URLEncoder.encode(whereQuery, "UTF-8")}")
        }
        params.add("\$limit=500")
        return "$apiBaseUrl?${params.joinToString("&")}"
    }

    private fun realizarSolicitudHttp(urlString: String): String {
        val url = URL(urlString)
        val conexion = url.openConnection() as HttpURLConnection
        conexion.requestMethod = "GET"
        conexion.readTimeout = 15000
        conexion.connectTimeout = 15000

        if (conexion.responseCode == HttpURLConnection.HTTP_OK) {
            val reader = BufferedReader(InputStreamReader(conexion.inputStream))
            val respuesta = StringBuilder()
            var linea: String?
            while (reader.readLine().also { linea = it } != null) {
                respuesta.append(linea)
            }
            reader.close()
            return respuesta.toString()
        } else {
            throw Exception("Error en la solicitud: ${conexion.responseCode}")
        }
    }

    private fun parsearRespuestaJson(jsonString: String, tecnologiaFiltro: String): List<Cobertura> {
        val listaResultados = mutableListOf<Cobertura>()
        try {
            val jsonArray = JSONArray(jsonString)
            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)
                val operador = jsonObject.optString("proveedor", "N/A")
                val municipio = jsonObject.optString("municipio", "N/A")

                val tecnologias = mapOf(
                    "2G" to jsonObject.optString("cobertura_2g", "N/A"),
                    "3G" to jsonObject.optString("cobertura_3g", "N/A"),
                    "4G" to jsonObject.optString("cobertuta_4g", "N/A"),
                    "LTE" to jsonObject.optString("cobertura_lte", "N/A"),
                    "5G" to jsonObject.optString("cobertura_5g", "N/A")
                )

                for ((tecnologia, cobertura) in tecnologias) {
                    val tecCoincide = (tecnologiaFiltro.uppercase() == "TODAS" || tecnologiaFiltro.uppercase() == tecnologia)
                    val tieneCob = cobertura.equals("S", ignoreCase = true)
                    if (tecCoincide && tieneCob) {
                        listaResultados.add(
                            Cobertura(operador, municipio, tecnologia, cobertura)
                        )
                    }
                }
            }
        } catch (e: JSONException) {
            Log.e("MainActivity", "Error parseando JSON", e)
        }
        return listaResultados.toSet().toList()
    }

    private fun showGeminiDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_gemini_chat, null)
        val etPregunta = dialogView.findViewById<EditText>(R.id.etPreguntaIA)
        val btnEnviar = dialogView.findViewById<Button>(R.id.btnEnviarPreguntaIA)
        val tvRespuesta = dialogView.findViewById<TextView>(R.id.tvRespuestaIA)
        val pbCargando = dialogView.findViewById<ProgressBar>(R.id.pbCargandoIA)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setNegativeButton("Cerrar") { d, _ -> d.dismiss() }
            .create()

        btnEnviar.setOnClickListener {
            val pregunta = etPregunta.text.toString()
            if (pregunta.isNotEmpty()) {
                pbCargando.visibility = View.VISIBLE
                btnEnviar.isEnabled = false
                tvRespuesta.text = "Generando respuesta..."

                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        val prompt = """
                            Eres un asistente experto en telecomunicaciones en Colombia.
                            Responde a la siguiente pregunta: $pregunta
                        """.trimIndent()

                        val response = generativeModel.generateContent(prompt)

                        withContext(Dispatchers.Main) {
                            pbCargando.visibility = View.GONE
                            btnEnviar.isEnabled = true
                            tvRespuesta.text = response.text ?: "No se pudo obtener respuesta."
                        }

                    } catch (e: Exception) {
                        Log.e("GeminiError", "Error al generar contenido", e)
                        withContext(Dispatchers.Main) {
                            pbCargando.visibility = View.GONE
                            btnEnviar.isEnabled = true
                            tvRespuesta.text = "Error: ${e.message}"
                        }
                    }
                }
            }
        }
        dialog.show()
    }
}
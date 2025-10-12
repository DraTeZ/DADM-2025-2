package com.example.directorioempresas

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.AdapterView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.directorioempresas.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var dbHelper: DatabaseHelper
    private lateinit var empresaAdapter: EmpresaAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)


        dbHelper = DatabaseHelper(this)


        setupRecyclerView()


        setupListeners()
    }

    private fun setupRecyclerView() {

        empresaAdapter = EmpresaAdapter(
            emptyList(),
            onItemClicked = { empresa ->
                // Acción al hacer clic en un item: abrir AddEditActivity para editar
                val intent = Intent(this, AddEditActivity::class.java).apply {
                    putExtra("EMPRESA_ID", empresa.id) // El error 'id' se soluciona si tu data class es correcta
                }
                startActivity(intent)
            },
            onDeleteClicked = { empresa ->
                // Acción al hacer clic en el botón de borrar: mostrar diálogo
                showDeleteConfirmationDialog(empresa)
            }
        )

        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = empresaAdapter
    }

    private fun setupListeners() {
        // Listener para el botón de añadir una nueva empresa
        binding.addButton.setOnClickListener {
            val intent = Intent(this, AddEditActivity::class.java)
            startActivity(intent)
        }

        // Listener para el campo de búsqueda de texto
        binding.searchEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                loadEmpresas() // Llama a cargar empresas cada vez que el texto cambia
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // Listener para el Spinner de clasificación
        binding.filterSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                loadEmpresas() // Llama a cargar empresas cuando se selecciona un nuevo ítem
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun showDeleteConfirmationDialog(empresa: Empresa) {
        AlertDialog.Builder(this)
            .setTitle("Confirmar Eliminación")
            .setMessage("¿Estás seguro de que quieres eliminar la empresa '${empresa.nombre}'?")
            .setPositiveButton("Sí, Eliminar") { _, _ ->
                deleteEmpresa(empresa)
            }
            .setNegativeButton("No", null)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .show()
    }

    private fun deleteEmpresa(empresa: Empresa) {
        val db = dbHelper.writableDatabase
        db.delete(DatabaseHelper.TABLE_NAME, "${DatabaseHelper.COLUMN_ID} = ?", arrayOf(empresa.id.toString()))
        loadEmpresas() // Recarga la lista para reflejar el cambio
    }

    // Se llama cada vez que la actividad vuelve a estar en primer plano
    override fun onResume() {
        super.onResume()
        loadEmpresas()
    }

    private fun loadEmpresas() {
        // Obtiene los valores actuales de los filtros
        val searchTerm = binding.searchEditText.text.toString()
        val filterClassification = binding.filterSpinner.selectedItem.toString()

        val db = dbHelper.readableDatabase

        // Listas para construir la consulta SQL dinámicamente
        val selectionClauses = mutableListOf<String>()
        val selectionArgs = mutableListOf<String>()

        // Añadir cláusula para el nombre si el término de búsqueda no está vacío
        if (searchTerm.isNotBlank()) {
            selectionClauses.add("${DatabaseHelper.COLUMN_NOMBRE} LIKE ?")
            selectionArgs.add("%$searchTerm%") // El % es un comodín para buscar coincidencias
        }

        // Añadir cláusula para la clasificación si no es "Todas"
        if (filterClassification != "Todas") {
            selectionClauses.add("${DatabaseHelper.COLUMN_CLASIFICACION} LIKE ?")
            selectionArgs.add("%$filterClassification%")
        }

        // Unir las cláusulas con "AND" si hay más de una
        val selection = selectionClauses.joinToString(" AND ")

        // Realizar la consulta a la base de datos
        val cursor = db.query(
            DatabaseHelper.TABLE_NAME, null,
            if (selection.isEmpty()) null else selection,
            if (selectionArgs.isEmpty()) null else selectionArgs.toTypedArray(),
            null, null, DatabaseHelper.COLUMN_NOMBRE // Ordenar por nombre
        )

        val empresas = mutableListOf<Empresa>()
        with(cursor) {
            while (moveToNext()) {
                // *** CORRECCIÓN IMPORTANTE ***
                // Usar getString para columnas de texto y getLong solo para el ID
                val id = getLong(getColumnIndexOrThrow(DatabaseHelper.COLUMN_ID))
                val nombre = getString(getColumnIndexOrThrow(DatabaseHelper.COLUMN_NOMBRE))
                val web = getString(getColumnIndexOrThrow(DatabaseHelper.COLUMN_WEB))
                val telefono = getString(getColumnIndexOrThrow(DatabaseHelper.COLUMN_TELEFONO))
                val email = getString(getColumnIndexOrThrow(DatabaseHelper.COLUMN_EMAIL))
                val productos = getString(getColumnIndexOrThrow(DatabaseHelper.COLUMN_PRODUCTOS))
                val clasificacion = getString(getColumnIndexOrThrow(DatabaseHelper.COLUMN_CLASIFICACION))

                empresas.add(Empresa(id, nombre, web, telefono, email, productos, clasificacion))
            }
        }
        cursor.close()


        empresaAdapter.updateData(empresas)
    }
}
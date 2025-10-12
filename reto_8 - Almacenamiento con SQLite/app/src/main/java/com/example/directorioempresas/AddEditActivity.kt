package com.example.directorioempresas
import android.content.ContentValues
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.directorioempresas.DatabaseHelper
import androidx.activity.addCallback
import androidx.appcompat.app.AlertDialog
import com.example.directorioempresas.databinding.ActivityAddEditBinding
import android.widget.Toast

class AddEditActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAddEditBinding
    private lateinit var dbHelper: DatabaseHelper
    private var empresaId: Long = -1L
    private var initialNombre: String = ""
    private var initialWeb: String = ""
    private var initialTelefono: String = ""
    private var initialEmail: String = ""
    private var initialProductos: String = ""
    private var initialClasificaciones = emptySet<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddEditBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dbHelper = DatabaseHelper(this)
        if (intent.hasExtra("EMPRESA_ID")) {
            empresaId = intent.getLongExtra("EMPRESA_ID", -1L)
            if (empresaId != -1L) {
                loadEmpresaData()
            }
        } else {
            captureInitialState()
        }
        binding.cancelButton.setOnClickListener {
            handleBackPress()
        }


        onBackPressedDispatcher.addCallback(this) {
            handleBackPress()
        }
        binding.saveButton.setOnClickListener {
            saveEmpresa()
        }
    }

    private fun captureInitialState() {
        initialNombre = binding.nombreEditText.text.toString()
        initialWeb = binding.webEditText.text.toString()
        initialTelefono = binding.telefonoEditText.text.toString()
        initialEmail = binding.emailEditText.text.toString()
        initialProductos = binding.productosEditText.text.toString()

        val clasificaciones = mutableSetOf<String>()
        if (binding.consultoriaCheckBox.isChecked) clasificaciones.add("Consultoría")
        if (binding.desarrolloCheckBox.isChecked) clasificaciones.add("Desarrollo a la medida")
        if (binding.fabricaCheckBox.isChecked) clasificaciones.add("Fábrica de software")
        initialClasificaciones = clasificaciones
    }

    private fun loadEmpresaData() {
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            DatabaseHelper.TABLE_NAME, null, "${DatabaseHelper.COLUMN_ID} = ?",
            arrayOf(empresaId.toString()), null, null, null
        )

        if (cursor.moveToFirst()) {
            binding.nombreEditText.setText(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_NOMBRE)))
            binding.webEditText.setText(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_WEB)))
            binding.telefonoEditText.setText(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_TELEFONO)))
            binding.emailEditText.setText(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_EMAIL)))
            binding.productosEditText.setText(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_PRODUCTOS)))

            val clasificaciones = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_CLASIFICACION)).split(", ")
            binding.consultoriaCheckBox.isChecked = "Consultoría" in clasificaciones
            binding.desarrolloCheckBox.isChecked = "Desarrollo a la medida" in clasificaciones
            binding.fabricaCheckBox.isChecked = "Fábrica de software" in clasificaciones
        }
        cursor.close()
        captureInitialState()
    }
    private fun hasChanges(): Boolean {
        val currentClasificaciones = mutableSetOf<String>()
        if (binding.consultoriaCheckBox.isChecked) currentClasificaciones.add("Consultoría")
        if (binding.desarrolloCheckBox.isChecked) currentClasificaciones.add("Desarrollo a la medida")
        if (binding.fabricaCheckBox.isChecked) currentClasificaciones.add("Fábrica de software")

        return binding.nombreEditText.text.toString() != initialNombre ||
                binding.webEditText.text.toString() != initialWeb ||
                binding.telefonoEditText.text.toString() != initialTelefono ||
                binding.emailEditText.text.toString() != initialEmail ||
                binding.productosEditText.text.toString() != initialProductos ||
                currentClasificaciones != initialClasificaciones
    }


    private fun handleBackPress() {
        if (hasChanges()) {
            showUnsavedChangesDialog()
        } else {
            finish()
        }
    }


    private fun showUnsavedChangesDialog() {
        AlertDialog.Builder(this)
            .setTitle("Descartar cambios")
            .setMessage("¿Estás seguro de que deseas descartar los cambios no guardados?")
            .setPositiveButton("Sí, Descartar") { _, _ ->
                finish()
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun saveEmpresa() {
        val nombre = binding.nombreEditText.text.toString().trim()
        if (nombre.isEmpty()) {
            binding.nombreEditText.error = "El nombre de la empresa es obligatorio."
            return
        }
        if (dbHelper.companyExists(nombre, empresaId)) {
            binding.nombreEditText.error = "Una empresa con este nombre ya existe."
            return
        }
        val web = binding.webEditText.text.toString()
        val telefono = binding.telefonoEditText.text.toString()
        val email = binding.emailEditText.text.toString()
        val productos = binding.productosEditText.text.toString()
        val clasificaciones = mutableListOf<String>()
        if (binding.consultoriaCheckBox.isChecked) clasificaciones.add("Consultoría")
        if (binding.desarrolloCheckBox.isChecked) clasificaciones.add("Desarrollo a la medida")
        if (binding.fabricaCheckBox.isChecked) clasificaciones.add("Fábrica de software")
        val clasificacion = clasificaciones.joinToString(", ")

        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(DatabaseHelper.COLUMN_NOMBRE, nombre)
            put(DatabaseHelper.COLUMN_WEB, web)
            put(DatabaseHelper.COLUMN_TELEFONO, telefono)
            put(DatabaseHelper.COLUMN_EMAIL, email)
            put(DatabaseHelper.COLUMN_PRODUCTOS, productos)
            put(DatabaseHelper.COLUMN_CLASIFICACION, clasificacion)
        }

        if (empresaId == -1L) {
            db.insert(DatabaseHelper.TABLE_NAME, null, values)
            Toast.makeText(this, "Empresa guardada", Toast.LENGTH_SHORT).show()
        } else {
            db.update(DatabaseHelper.TABLE_NAME, values, "${DatabaseHelper.COLUMN_ID} = ?", arrayOf(empresaId.toString()))
            Toast.makeText(this, "Empresa actualizada", Toast.LENGTH_SHORT).show()
        }
        finish()
    }
}
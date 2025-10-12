package com.example.directorioempresas
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_VERSION = 1
        private const val DATABASE_NAME = "Empresas.db"
        const val TABLE_NAME = "empresas"
        const val COLUMN_ID = "id"
        const val COLUMN_NOMBRE = "nombre"
        const val COLUMN_WEB = "web"
        const val COLUMN_TELEFONO = "telefono"
        const val COLUMN_EMAIL = "email"
        const val COLUMN_PRODUCTOS = "productos"
        const val COLUMN_CLASIFICACION = "clasificacion"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTable = "CREATE TABLE $TABLE_NAME (" +
                "$COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT," +
                "$COLUMN_NOMBRE TEXT," +
                "$COLUMN_WEB TEXT," +
                "$COLUMN_TELEFONO TEXT," +
                "$COLUMN_EMAIL TEXT," +
                "$COLUMN_PRODUCTOS TEXT," +
                "$COLUMN_CLASIFICACION TEXT)"
        db.execSQL(createTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
        onCreate(db)
    }
    fun companyExists(nombre: String, idAExcluir: Long = -1L): Boolean {
        val db = readableDatabase
        var cursor: android.database.Cursor? = null
        try {

            val selectionArgs = mutableListOf<String>()
            var selection = "$COLUMN_NOMBRE COLLATE NOCASE = ?"
            selectionArgs.add(nombre)


            if (idAExcluir != -1L) {
                selection += " AND $COLUMN_ID != ?"
                selectionArgs.add(idAExcluir.toString())
            }

            cursor = db.query(
                TABLE_NAME,
                arrayOf(COLUMN_ID),
                selection,
                selectionArgs.toTypedArray(), // Convertimos la lista a Array
                null,
                null,
                null
            )
            return cursor.count > 0
        } finally {
            cursor?.close()
        }
    }
}
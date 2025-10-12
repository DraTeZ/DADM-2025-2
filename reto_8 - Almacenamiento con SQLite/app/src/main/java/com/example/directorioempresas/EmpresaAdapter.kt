package com.example.directorioempresas

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import android.widget.ImageButton
import kotlin.collections.get

class EmpresaAdapter(
    private var empresas: List<Empresa>,
    private val onItemClicked: (Empresa) -> Unit,
    private val onDeleteClicked: (Empresa) -> Unit
) : RecyclerView.Adapter<EmpresaAdapter.EmpresaViewHolder>() {
    class EmpresaViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        // Ahora sí encontrará estas referencias porque existen en el XML
        val nombreTextView: TextView = view.findViewById(R.id.nombreTextView)
        val clasificacionTextView: TextView = view.findViewById(R.id.clasificacionTextView)
        val deleteButton: ImageButton = view.findViewById(R.id.deleteButton)

        fun bind(empresa: Empresa, onItemClicked: (Empresa) -> Unit, onDeleteClicked: (Empresa) -> Unit) {
            nombreTextView.text = empresa.nombre
            clasificacionTextView.text = empresa.clasificacion
            itemView.setOnClickListener { onItemClicked(empresa) }
            deleteButton.setOnClickListener { onDeleteClicked(empresa) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EmpresaViewHolder {
        // Ahora sí encontrará "list_item_empresa"
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_item_empresa, parent, false)
        return EmpresaViewHolder(view)
    }

    override fun onBindViewHolder(holder: EmpresaViewHolder, position: Int) {
        holder.bind(empresas[position], onItemClicked, onDeleteClicked)
    }

    override fun getItemCount() = empresas.size

    fun updateData(newEmpresas: List<Empresa>) {
        empresas = newEmpresas
        notifyDataSetChanged()
    }
}


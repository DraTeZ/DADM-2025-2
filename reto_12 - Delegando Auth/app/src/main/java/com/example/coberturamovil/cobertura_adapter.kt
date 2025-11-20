package com.example.coberturamovil

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.coberturamovil.databinding.CoberturaBinding


class CoberturaAdapter : ListAdapter<Cobertura, CoberturaAdapter.CoberturaViewHolder>(CoberturaDiffCallback()) {


    inner class CoberturaViewHolder(private val binding: CoberturaBinding) : RecyclerView.ViewHolder(binding.root) {


        fun bind(item: Cobertura) {
            binding.tvOperadorItem.text = item.operador.uppercase()
            binding.tvMunicipioItem.text = "Municipio: ${item.municipio}"
            binding.tvTecnologiaItem.text = "Tecnología: ${item.tecnologia}"
            binding.tvCoberturaItem.text = "Cobertura: ${item.cobertura}"
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CoberturaViewHolder {
        val binding = CoberturaBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )

        return CoberturaViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CoberturaViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}


class CoberturaDiffCallback : DiffUtil.ItemCallback<Cobertura>() {
    override fun areItemsTheSame(oldItem: Cobertura, newItem: Cobertura): Boolean {

        return oldItem.operador == newItem.operador &&
                oldItem.municipio == newItem.municipio &&
                oldItem.tecnologia == newItem.tecnologia
    }

    override fun areContentsTheSame(oldItem: Cobertura, newItem: Cobertura): Boolean {
        return oldItem == newItem
    }
}
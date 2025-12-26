package com.example.reportes.Adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.reportes.Models.SeccionHome
import com.example.reportes.R

class SeccionHomeAdapter(
    private val items: List<SeccionHome>,
    private val onClick: (SeccionHome) -> Unit
) : RecyclerView.Adapter<SeccionHomeAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val titulo: TextView = view.findViewById(R.id.tvTitulo)
        val estado: TextView = view.findViewById(R.id.tvEstado)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_seccion_home, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]

        holder.titulo.text = item.titulo
        holder.estado.text = if (item.completada) "Completa" else "Pendiente"

        holder.itemView.setOnClickListener {
            onClick(item)
        }

        holder.estado.text = if (item.completada) "Completado" else "Pendiente"
        holder.estado.setTextColor(
            if (item.completada) Color.parseColor("#2E7D32") else Color.RED
        )
    }

    override fun getItemCount() = items.size
}
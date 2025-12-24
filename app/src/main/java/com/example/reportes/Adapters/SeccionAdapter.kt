package com.example.reportes.Adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.reportes.Models.SeccionItem
import com.example.reportes.R

class SeccionAdapter(
    private val items: MutableList<SeccionItem>,
    private val onFotoClick: (Int) -> Unit,
    private val onEliminarClick: (Int) -> Unit
) : RecyclerView.Adapter<SeccionAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imgFoto: ImageView = view.findViewById(R.id.imgFoto)
        val etObs: EditText = view.findViewById(R.id.etObservacion)
        val btnFoto: Button = view.findViewById(R.id.btnFoto)
        val btnEliminar: Button = view.findViewById(R.id.btnEliminar)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_seccion, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]

        if (item.fotoUri.isNotEmpty()) {
            Glide.with(holder.imgFoto.context)
                .load(item.fotoUri)
                .into(holder.imgFoto)
        }

        holder.etObs.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                items[position].observacion = holder.etObs.text.toString()
            }
        }

        holder.btnFoto.setOnClickListener {
            onFotoClick(position)
        }

        holder.btnEliminar.setOnClickListener {
            onEliminarClick(position)
        }
    }

    override fun getItemCount(): Int = items.size
}

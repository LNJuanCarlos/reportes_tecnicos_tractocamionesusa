package com.example.reportes.Adapters

import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.reportes.Models.ItemSeccion
import com.example.reportes.R
import java.io.File

class SeccionAdapter(
    private val items: MutableList<ItemSeccion>,
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

        // FOTO
        when {
            item.fotoLocal.isNotEmpty() -> {
                Glide.with(holder.imgFoto.context)
                    .load(File(item.fotoLocal))
                    .into(holder.imgFoto)
            }

            item.fotoUrl.isNotEmpty() -> {
                Glide.with(holder.imgFoto.context)
                    .load(item.fotoUrl)
                    .into(holder.imgFoto)
            }

            else -> holder.imgFoto.setImageDrawable(null)
        }

        // OBSERVACIÓN
        holder.etObs.setText(item.observacion)

        holder.etObs.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                items[holder.adapterPosition].observacion = s.toString()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        holder.btnFoto.setOnClickListener {
            onFotoClick(holder.adapterPosition)
        }

        holder.btnEliminar.setOnClickListener {
            onEliminarClick(holder.adapterPosition)
        }
    }

    override fun getItemCount(): Int = items.size
}

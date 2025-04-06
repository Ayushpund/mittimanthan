package com.example.pict.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

import com.example.pict.databinding.ItemCartBinding
import com.example.pict.models.Fertilizer

class CartAdapter(
    private val items: List<Fertilizer>,
    private val onRemove: (Fertilizer) -> Unit
) : RecyclerView.Adapter<CartAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemCartBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(fertilizer: Fertilizer, onRemove: (Fertilizer) -> Unit) {
            binding.apply {
                productName.text = fertilizer.name
                productPrice.text = fertilizer.price
                removeButton.setOnClickListener { onRemove(fertilizer) }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemCartBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position], onRemove)
    }

    override fun getItemCount() = items.size
} 
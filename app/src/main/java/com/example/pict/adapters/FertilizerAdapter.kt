package com.example.pict.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

import com.example.pict.databinding.ItemFertilizerBinding
import com.example.pict.models.Fertilizer

class FertilizerAdapter(
    private val fertilizers: List<Fertilizer>,
    private val onItemClick: (Fertilizer) -> Unit,
    private val onAddToCart: (Fertilizer) -> Unit
) : RecyclerView.Adapter<FertilizerAdapter.FertilizerViewHolder>() {

    inner class FertilizerViewHolder(private val binding: ItemFertilizerBinding) :
        RecyclerView.ViewHolder(binding.root) {
        
        fun bind(fertilizer: Fertilizer) {
            binding.apply {
                fertilizerImage.setImageResource(fertilizer.imageId)
                fertilizerName.text = fertilizer.name
                fertilizerPrice.text = fertilizer.price
                fertilizerCategory.text = fertilizer.category

                root.setOnClickListener { onItemClick(fertilizer) }
                addToCartButton.setOnClickListener { onAddToCart(fertilizer) }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FertilizerViewHolder {
        val binding = ItemFertilizerBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return FertilizerViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FertilizerViewHolder, position: Int) {
        holder.bind(fertilizers[position])
    }

    override fun getItemCount() = fertilizers.size
}

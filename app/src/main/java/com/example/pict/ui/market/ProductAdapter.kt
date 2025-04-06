package com.example.pict.ui.market

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.pict.R
import com.example.pict.models.Product
import com.google.android.material.card.MaterialCardView

class ProductAdapter(
    private val context: Context,
    private val productList: List<Product>,
    private val onProductClick: (Product) -> Unit
) : RecyclerView.Adapter<ProductAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val cardView: MaterialCardView = view.findViewById(R.id.productCardView)
        val imageView: ImageView = view.findViewById(R.id.productImage)
        val nameTextView: TextView = view.findViewById(R.id.nameTextView)
        val priceTextView: TextView = view.findViewById(R.id.priceTextView)
        val descriptionTextView: TextView = view.findViewById(R.id.descriptionTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_product_grid, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val product = productList[position]
        
        holder.nameTextView.text = product.name
        holder.priceTextView.text = "₹${product.price}"
        holder.descriptionTextView.text = product.description
        
        try {
            holder.imageView.setImageResource(product.imageResId)
        } catch (e: Exception) {
            Log.e("ProductAdapter", "Error loading image for ${product.name}: ${e.message}")
            holder.imageView.setImageResource(R.drawable.image_placeholder)
        }
        
        // Apply elevation and ripple effect for better UI
        holder.cardView.apply {
            elevation = context.resources.getDimension(R.dimen.card_elevation)
            setOnClickListener { onProductClick(product) }
        }
    }

    override fun getItemCount() = productList.size
}
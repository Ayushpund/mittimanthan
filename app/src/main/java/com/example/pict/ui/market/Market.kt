package com.example.pict.ui.market

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.example.pict.R
import com.example.pict.databinding.FragmentMarketBinding
import com.example.pict.models.Product
import com.example.pict.utils.MarketItemDecoration

class MarketFragment : Fragment() {
    
    private lateinit var binding: FragmentMarketBinding
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMarketBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
    }
    
    private fun setupUI() {
        setupToolbar()
        setupProductList()
        setupSearch()
    }
    
    private fun setupToolbar() {
        binding.topAppBar.apply {
            title = "Agro Market"
            inflateMenu(R.menu.market_menu)
            setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.menu_sort -> {
                        showSortDialog()
                        true
                    }
                    R.id.menu_filter -> {
                        showFilterDialog()
                        true
                    }
                    else -> false
                }
            }
        }
    }
    
    private fun setupProductList() {
        val productList = listOf(
            Product("Soil Test Kit", "Test your soil for nutrients and pH", 499.0, R.drawable.soil_test_kit),
            Product("Garden Soil", "Premium quality garden soil", 299.0, R.drawable.garden_soil),
            Product("Soil Conditioner", "Improves soil structure", 349.0, R.drawable.soil_conditioner),
            Product("Neem Cake", "Organic fertilizer and pest repellent", 199.0, R.drawable.neem_cake),
            Product("Moisture Meter", "Monitor soil moisture levels", 399.0, R.drawable.moisture_meter),
            Product("Urea", "Nitrogen-rich fertilizer", 150.0, R.drawable.urea),
            Product("Bone Meal", "Phosphorus-rich organic fertilizer", 249.0, R.drawable.bone_meal),
            Product("NPK Test Kit", "Test for Nitrogen, Phosphorus, and Potassium", 599.0, R.drawable.npk_test),
            Product("DAP", "Diammonium Phosphate fertilizer", 279.0, R.drawable.dap),
            Product("Soil Microbes", "Beneficial microorganisms for soil health", 349.0, R.drawable.soil_microbes),
            Product("Compost", "Organic compost for gardens", 199.0, R.drawable.compost),
            Product("NPK Fertilizer", "Balanced NPK fertilizer", 299.0, R.drawable.npk)
        )
        
        // Improved adapter setup with item decoration for better spacing
        val adapter = ProductAdapter(requireContext(), productList, onProductClick = { product ->
            showProductDetails(product)
        })
        
        binding.recyclerViewProducts.apply {
            this.adapter = adapter
            layoutManager = GridLayoutManager(requireContext(), 2)
            addItemDecoration(MarketItemDecoration(resources.getDimensionPixelSize(R.dimen.item_spacing)))
        }
    }
    
    private fun setupSearch() {
        binding.searchView.setOnQueryTextListener(object : androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                // Handle search submission
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                // Filter products in real-time
                return true
            }
        })
    }
    
    private fun showSortDialog() {
        // Implementation for sort dialog
    }
    
    private fun showFilterDialog() {
        // Implementation for filter dialog
    }
    
    private fun showProductDetails(product: Product) {
        // Show product details in a bottom sheet or dialog
    }
}
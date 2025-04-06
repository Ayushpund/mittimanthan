package com.example.pict

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.pict.adapters.CartAdapter
import com.example.pict.adapters.FertilizerAdapter
import com.example.pict.databinding.CartBottomSheetBinding
import com.example.pict.databinding.ProductDetailBottomSheetBinding
import com.example.pict.databinding.FragmentMarketBinding
import com.example.pict.models.Fertilizer
import com.google.android.material.badge.BadgeDrawable
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class Market : Fragment() {
    private lateinit var binding: FragmentMarketBinding
    private var filteredProducts = mutableListOf<Fertilizer>()
    private val cart = mutableListOf<Fertilizer>()
    private var currentDialog: AlertDialog? = null
    private var cartBadge: BadgeDrawable? = null

    private val fertilizers = listOf(
        Fertilizer(
            name = "Premium Organic Compost",
            price = "₹499",
            category = "Organic",
            description = "• 100% Natural Organic Compost\n" +
                    "• Rich in nutrients and beneficial microorganisms\n" +
                    "• Improves soil structure and water retention\n" +
                    "• Perfect for all types of plants\n" +
                    "• Pack size: 5 kg",
            imageId = R.drawable.compost
        ),
        Fertilizer(
            name = "NPK Gold 14-14-14",
            price = "₹899",
            category = "Chemical",
            description = "• Balanced NPK ratio for optimal growth\n" +
                    "• Fast-acting and long-lasting\n" +
                    "• Enhanced with micronutrients\n" +
                    "• Suitable for all crops\n" +
                    "• Pack size: 10 kg",
            imageId = R.drawable.npk
        ),
        Fertilizer(
            name = "Premium Vermicompost",
            price = "₹599",
            category = "Organic",
            description = "• High-quality earthworm castings\n" +
                    "• Rich in humus and beneficial enzymes\n" +
                    "• Improves soil fertility naturally\n" +
                    "• Ideal for organic farming\n" +
                    "• Pack size: 5 kg",
            imageId = R.drawable.vermicompost
        ),
        // New soil-related products
        Fertilizer(
            name = "Soil pH Testing Kit",
            price = "₹349",
            category = "Tools",
            description = "• Accurate soil pH measurement\n" +
                    "• Easy to use with color-coded results\n" +
                    "• Includes 50 test strips\n" +
                    "• Helps determine soil amendments needed\n" +
                    "• Suitable for home gardens and farms",
            imageId = R.drawable.soil_test_kit
        ),
        Fertilizer(
            name = "Garden Soil Mix",
            price = "₹399",
            category = "Organic",
            description = "• Premium blend for potted plants\n" +
                    "• Contains coco peat, vermicompost and perlite\n" +
                    "• Excellent drainage and aeration\n" +
                    "• Pre-fertilized for immediate planting\n" +
                    "• Pack size: 10 kg",
            imageId = R.drawable.garden_soil
        ),
        Fertilizer(
            name = "Soil Conditioner",
            price = "₹449",
            category = "Organic",
            description = "• Improves soil structure and fertility\n" +
                    "• Enhances water retention capacity\n" +
                    "• Contains beneficial microorganisms\n" +
                    "• Reduces soil compaction\n" +
                    "• Pack size: 5 kg",
            imageId = R.drawable.soil_conditioner
        ),
        Fertilizer(
            name = "Neem Cake Organic Manure",
            price = "₹299",
            category = "Organic",
            description = "• Natural soil enricher and pest repellent\n" +
                    "• Slow-release nitrogen source\n" +
                    "• Improves soil health and fertility\n" +
                    "• Eco-friendly and chemical-free\n" +
                    "• Pack size: 2 kg",
            imageId = R.drawable.neem_cake
        ),
        Fertilizer(
            name = "Soil Moisture Meter",
            price = "₹599",
            category = "Tools",
            description = "• Measures soil moisture, pH and light levels\n" +
                    "• No batteries required\n" +
                    "• Easy-to-read dial display\n" +
                    "• Helps prevent over/under watering\n" +
                    "• Suitable for indoor and outdoor plants",
            imageId = R.drawable.moisture_meter
        ),
        Fertilizer(
            name = "Urea Fertilizer",
            price = "₹450",
            category = "Chemical",
            description = "• High nitrogen content (46%)\n" +
                    "• Promotes leafy growth and green foliage\n" +
                    "• Fast-acting and water-soluble\n" +
                    "• Suitable for most crops\n" +
                    "• Pack size: 5 kg",
            imageId = R.drawable.urea
        ),
        Fertilizer(
            name = "Bone Meal Fertilizer",
            price = "₹349",
            category = "Organic",
            description = "• Rich in phosphorus and calcium\n" +
                    "• Promotes strong root development\n" +
                    "• Slow-release formula for long-lasting effects\n" +
                    "• Ideal for flowering and fruiting plants\n" +
                    "• Pack size: 2 kg",
            imageId = R.drawable.bone_meal
        ),
        Fertilizer(
            name = "Soil NPK Test Kit",
            price = "₹799",
            category = "Tools",
            description = "• Tests nitrogen, phosphorus and potassium levels\n" +
                    "• Includes 40 tests (10 for each nutrient + pH)\n" +
                    "• Professional-grade accuracy\n" +
                    "• Detailed instructions and color charts included\n" +
                    "• Essential for precision farming",
            imageId = R.drawable.npk_test
        ),
        Fertilizer(
            name = "DAP Fertilizer",
            price = "₹950",
            category = "Chemical",
            description = "• High phosphorus content (18-46-0)\n" +
                    "• Promotes flowering and fruiting\n" +
                    "• Water-soluble and easy to apply\n" +
                    "• Suitable for most crops\n" +
                    "• Pack size: 5 kg",
            imageId = R.drawable.dap
        ),
        Fertilizer(
            name = "Organic Soil Microbes",
            price = "₹599",
            category = "Organic",
            description = "• Contains beneficial bacteria and fungi\n" +
                    "• Enhances nutrient uptake by plants\n" +
                    "• Improves soil structure and health\n" +
                    "• Reduces need for chemical fertilizers\n" +
                    "• Pack size: 500g",
            imageId = R.drawable.soil_microbes
        )
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMarketBinding.inflate(inflater, container, false)
        setupUI()
        return binding.root
    }

    private fun setupUI() {
        setupToolbar()
        setupRecyclerView()
        setupSearch()
        setupCart()
        filteredProducts.addAll(fertilizers)
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



    private fun showSellDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_sell_fertilizer, null)
        val builder = MaterialAlertDialogBuilder(requireContext())
            .setTitle("List Your Product")
            .setView(dialogView)
            .setPositiveButton("List Product") { dialog, _ ->
                Toast.makeText(context, "Product listed successfully!", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)

        currentDialog = builder.create() as AlertDialog

        dialogView.findViewById<MaterialButton>(R.id.uploadImageButton)?.setOnClickListener {
            pickImage.launch("image/*")
        }

        currentDialog?.show()
    }

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            currentDialog?.findViewById<ImageView>(R.id.productImage)?.setImageURI(it)
        }
    }

    private fun showProductDetails(fertilizer: Fertilizer) {
        val bottomSheet = BottomSheetDialog(requireContext())
        val detailBinding = ProductDetailBottomSheetBinding.inflate(layoutInflater)

        detailBinding.apply {
            productImage.setImageResource(fertilizer.imageId)
            productName.text = fertilizer.name
            productPrice.text = fertilizer.price
            productDescription.text = fertilizer.description

            // Add seller info
            sellerInfo.text = "Sold by: Agro Supplies Ltd.\n" +
                    "Rating: ★★★★☆ (4.2)\n" +
                    "Quick Delivery Available"

            // Add delivery estimate
            deliveryInfo.text = "Delivery by ${getDeliveryDate()}"

            // Add quantity selector
            quantitySpinner.setSelection(0) // Default to 1

            buyNowButton.setOnClickListener {
                val quantity = quantitySpinner.selectedItem.toString().toInt()
                initiatePayment(fertilizer.price.replace("₹", "").toDouble() * quantity)
                bottomSheet.dismiss()
            }

            addToCartButton.setOnClickListener {
                val quantity = quantitySpinner.selectedItem.toString().toInt()
                repeat(quantity) { addToCart(fertilizer) }
                bottomSheet.dismiss()
            }
        }

        bottomSheet.setContentView(detailBinding.root)
        bottomSheet.show()
    }

    private fun getDeliveryDate(): String {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_MONTH, 3) // Delivery in 3 days
        val dateFormat = SimpleDateFormat("dd MMM, EEEE", Locale.getDefault())
        return dateFormat.format(calendar.time)
    }

    private fun setupRecyclerView() {
        binding.recyclerViewProducts.apply {
            layoutManager = GridLayoutManager(context, 2)
            adapter = FertilizerAdapter(
                fertilizers = filteredProducts,
                onItemClick = { showProductDetails(it) },
                onAddToCart = { addToCart(it) }
            )
        }
    }

    private fun setupSearch() {
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                filterProducts(query)
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filterProducts(newText)
                return true
            }
        })
    }



    private fun setupCart() {
        binding.cartFab.setOnClickListener {
            showCartBottomSheet()
        }
    }

    private fun addToCart(fertilizer: Fertilizer) {
        cart.add(fertilizer)
        Toast.makeText(context, "${fertilizer.name} added to cart", Toast.LENGTH_SHORT).show()
    }

    private fun showSortDialog() {
        val options = arrayOf("Price: Low to High", "Price: High to Low", "Name: A to Z")
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Sort By")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> sortByPriceLowToHigh()
                    1 -> sortByPriceHighToLow()
                    2 -> sortByName()
                }
            }
            .show()
    }

    private fun showFilterDialog() {
        val categories = listOf("All", "Organic", "Inorganic", "Biofertilizer")
        val checkedItems = BooleanArray(categories.size)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Filter By Category")
            .setMultiChoiceItems(
                categories.toTypedArray(),
                checkedItems
            ) { _, which, isChecked ->
                checkedItems[which] = isChecked
            }
            .setPositiveButton("Apply") { _, _ ->
                applyFilters(categories.filterIndexed { index, _ -> checkedItems[index] })
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun sortByPriceLowToHigh() {
        filteredProducts.sortBy { it.price.replace("₹", "").toDouble() }
        binding.recyclerViewProducts.adapter?.notifyDataSetChanged()
    }

    private fun sortByPriceHighToLow() {
        filteredProducts.sortByDescending { it.price.replace("₹", "").toDouble() }
        binding.recyclerViewProducts.adapter?.notifyDataSetChanged()
    }

    private fun sortByName() {
        filteredProducts.sortBy { it.name }
        binding.recyclerViewProducts.adapter?.notifyDataSetChanged()
    }

    private fun applyFilters(selectedCategories: List<String>) {
        filteredProducts.clear()
        if (selectedCategories.isEmpty() || selectedCategories.contains("All")) {
            filteredProducts.addAll(fertilizers)
        } else {
            filteredProducts.addAll(fertilizers.filter { it.category in selectedCategories })
        }
        binding.recyclerViewProducts.adapter?.notifyDataSetChanged()
    }

    private fun filterProducts(query: String?) {
        if (query.isNullOrBlank()) {
            filteredProducts.clear()
            filteredProducts.addAll(fertilizers)
        } else {
            filteredProducts.clear()
            filteredProducts.addAll(fertilizers.filter {
                it.name.contains(query, ignoreCase = true) ||
                        it.category.contains(query, ignoreCase = true)
            })
        }
        binding.recyclerViewProducts.adapter?.notifyDataSetChanged()
    }

    private fun initiatePayment(amount: Double) {
        try {
            val upiUri = Uri.Builder()
                .scheme("upi")
                .authority("pay")
                .appendQueryParameter("pa", "ayushpund12345@okhdfcbank")
                .appendQueryParameter("pn", "AgroMarket")
                .appendQueryParameter("tn", "Purchase")
                .appendQueryParameter("am", amount.toString())
                .appendQueryParameter("cu", "INR")
                .build()

            val paymentIntent = Intent(Intent.ACTION_VIEW).apply {
                data = upiUri
            }

            startActivityForResult(
                Intent.createChooser(paymentIntent, "Pay with..."),
                PAYMENT_REQUEST_CODE
            )
        } catch (e: Exception) {
            Toast.makeText(context, "No UPI app found!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun filterByCategory(category: String) {
        filteredProducts.clear()
        filteredProducts.addAll(fertilizers.filter { it.category == category })
        binding.recyclerViewProducts.adapter?.notifyDataSetChanged()
    }

    private fun showAllProducts() {
        filteredProducts.clear()
        filteredProducts.addAll(fertilizers)
        binding.recyclerViewProducts.adapter?.notifyDataSetChanged()
    }

    private fun showCartBottomSheet() {
        val bottomSheet = BottomSheetDialog(requireContext())
        val cartBinding = CartBottomSheetBinding.inflate(layoutInflater)

        cartBinding.apply {
            cartRecyclerView.layoutManager = LinearLayoutManager(context)
            cartRecyclerView.adapter = CartAdapter(cart) {
                cart.remove(it)
                updateCartBadge()
                // Recalculate total
                val total = cart.sumOf { item -> item.price.replace("₹", "").toDouble() }
                totalAmount.text = "Total: ₹$total"
            }

            val total = cart.sumOf { it.price.replace("₹", "").toDouble() }
            totalAmount.text = "Total: ₹$total"

            checkoutButton.setOnClickListener {
                if (cart.isNotEmpty()) {
                    initiatePayment(total)
                    bottomSheet.dismiss()
                }
            }
        }

        bottomSheet.setContentView(cartBinding.root)
        bottomSheet.show()
    }

    private fun updateCartBadge() {
        if (!this::binding.isInitialized) return

        if (cartBadge == null) {
            cartBadge = BadgeDrawable.create(requireContext())
        }

        if (cart.isEmpty()) {
            cartBadge?.isVisible = false
        } else {
            cartBadge?.apply {
                isVisible = true
                setNumber(cart.size)
            }
        }
    }

    companion object {
        private const val PAYMENT_REQUEST_CODE = 123
    }
}

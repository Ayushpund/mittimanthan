package com.example.pict

import android.Manifest
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.location.Location
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.pict.databinding.FragmentDashBoardBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class Dashboard : Fragment() {
    private lateinit var binding: FragmentDashBoardBinding
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var currentLocation: Location? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentDashBoardBinding.inflate(inflater, container, false)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        
        // Request location permissions
        requestLocationPermission()
        
        setupClickListeners()
        setupInitialData()
        return binding.root
    }

    private fun requestLocationPermission() {
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Request location permission
            requestPermissions(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        } else {
            fetchCurrentLocation()
        }
    }

    private fun fetchCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location: Location? ->
                    currentLocation = location
                    updateLocationText(location)
                }
                .addOnFailureListener {
                    Log.e("Dashboard", "Failed to get location", it)
                    // Fallback to default location
                    binding.locationText.text = "Farm Location: Nashik"
                }
        }
    }

    private fun updateLocationText(location: Location?) {
        location?.let {
            val locationText = "Location: ${it.latitude}, ${it.longitude}"
            binding.locationText.text = locationText
        }
    }

    // Add this to your setupClickListeners() method
    private fun setupClickListeners() {
        // Quick Report Card click listener
        binding.btnGenerateReport.setOnClickListener {
            try {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Quick Report")
                    .setMessage(generateQuickReport())
                    .setPositiveButton("Download") { _, _ ->
                        createAndDownloadPDF()
                    }
                    .setNegativeButton("Close", null)
                    .show()
            } catch (e: Exception) {
                Toast.makeText(context, "Error generating report", Toast.LENGTH_SHORT).show()
            }
        }

        // Chatbot FAB click listener
        // In the setupClickListeners() method:
        binding.chatbotFab.setOnClickListener {
            try {
                val chatbotFragment = SoilChatbotFragment()
                chatbotFragment.show(parentFragmentManager, "chatbot_dialog")
            } catch (e: Exception) {
                Toast.makeText(
                    context,
                    "Error opening chatbot: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        // Download Report button click listener
        binding.downloadReportButton.setOnClickListener {
            createAndDownloadPDF()
        }

        // Detailed Report button click listener
        binding.btnDetailedReport.setOnClickListener {
            try {
                showDetailedReportDialog()
            } catch (e: Exception) {
                Toast.makeText(context, "Error showing detailed report", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupInitialData() {
        try {
            val database = FirebaseDatabase.getInstance()
            val userRef = database.getReference("users/JOWAJdxsFtQzB32mB7Fqp46ELjt1/SoilData")

            userRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    // Ensure we're on the main thread and fragment is still attached
                    activity?.runOnUiThread {
                        if (!isAdded) return@runOnUiThread

                        // Fetch values from Firebase with default values
                        val temperature = snapshot.child("Temperature").getValue(Float::class.java) ?: 26.0f
                        val rawPH = snapshot.child("pH").getValue(Float::class.java) ?: 6.5f
                        val ph = convertRawPHToActualPH(rawPH)
                        val nitrogen = snapshot.child("Nitrogen").getValue(Int::class.java) ?: 50
                        val phosphorus = snapshot.child("Phosphorus").getValue(Int::class.java) ?: 20
                        val potassium = snapshot.child("Potassium").getValue(Int::class.java) ?: 30
                        val ec = snapshot.child("EC").getValue(Float::class.java) ?: 0.5f

                        // Set current date
                        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                        binding.dateText.text = "Date: ${dateFormat.format(Date())}"

                        // Update soil parameters
                        updateSoilParameters(
                            ph = ph,
                            nutrients = "$nitrogen-$phosphorus-$potassium",
                            moisture = calculateMoistureFromEC(ec),
                            temperature = temperature
                        )

                        // Assess soil health
                        val soilHealthAssessment = assessSoilHealth(
                            ph = ph,
                            nitrogen = nitrogen,
                            phosphorus = phosphorus,
                            potassium = potassium,
                            temperature = temperature,
                            moisture = calculateMoistureFromEC(ec)
                        )

                        // Convert soil health assessment to score for updateSoilHealth
                        val soilHealthScore = convertSoilHealthToScore(soilHealthAssessment)

                        // Update UI
                        binding.phValue.text = String.format("%.1f", ph)
                        binding.temperatureValue.text = String.format("%.1f°C", temperature)
                        binding.nutrientsValue.text = nitrogen.toString()
                        binding.phosphorusValue.text = phosphorus.toString()
                        binding.potassiumValue.text = potassium.toString()

                        // pH status
                        val phStatus = when {
                            ph < 6.0 -> "Acidic"
                            ph > 7.5 -> "Alkaline"
                            else -> "Neutral"
                        }
                        binding.phStatus.text = phStatus

                        // Update soil health
                        updateSoilHealth(soilHealthScore)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    // Log the error and show a toast
                    Log.e("Dashboard", "Database error: ${error.message}")
                    activity?.runOnUiThread {
                        Toast.makeText(
                            context, 
                            "Failed to load soil data", 
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            })
        } catch (e: Exception) {
            Log.e("Dashboard", "Error in setupInitialData", e)
        }
    }

    private fun generateQuickReport(): String {
        return """
            Quick Soil Health Report
            ${binding.dateText.text}
            ${binding.locationText.text}
            
            Overall Health: ${binding.soilHealthStatus.text}
            ${binding.soilHealthDescription.text}
            
            Key Parameters:
            - pH: ${binding.phValue.text}
            - Temperature: ${binding.temperatureValue.text}
            - Moisture: ${binding.moistureValue.text}
        """.trimIndent()
    }

    private fun generateDetailedReport(): String {
        return """
            Detailed Soil Health Report
            ${binding.dateText.text}
            ${binding.locationText.text}
            
            Health Status: ${binding.soilHealthStatus.text}
            ${binding.soilHealthDescription.text}
            
            Parameters:
            - pH Level: ${binding.phValue.text}
            - Temperature: ${binding.temperatureValue.text}
            - Moisture: ${binding.moistureValue.text}
            
            Nutrient Levels:
            - Nitrogen: ${binding.nutrientsValue.text} mg/kg
            - Phosphorus: ${binding.phosphorusValue.text} mg/kg
            - Potassium: ${binding.potassiumValue.text} mg/kg
        """.trimIndent()
    }

    private fun updateSoilHealth(healthScore: Int) {
        binding.soilHealthStatus.text = "Good $healthScore"
        binding.healthIndicator.progress = healthScore
        binding.moistureProgress.progress = healthScore

        val healthDescription = when {
            healthScore >= 80 -> "Excellent soil conditions for crop growth"
            healthScore >= 60 -> "Good soil conditions for crop growth"
            healthScore >= 40 -> "Moderate soil conditions, some improvements needed"
            else -> "Poor soil conditions, immediate attention required"
        }
        binding.soilHealthDescription.text = healthDescription
    }

    private fun updateSoilParameters(
        ph: Float,
        nutrients: String,
        moisture: Int,
        temperature: Float
    ) {
        binding.phValue.text = String.format("%.1f", ph)

        val npkValues = nutrients.split("-")
        if (npkValues.size == 3) {
            binding.nutrientsValue.text = npkValues[0]
            binding.phosphorusValue.text = npkValues[1]
            binding.potassiumValue.text = npkValues[2]
        }

        binding.moistureValue.text = "$moisture%"
        binding.temperatureValue.text = String.format("%.1f°C", temperature)

        val healthScore = calculateHealthScore(ph, nutrients, moisture, temperature)
        updateSoilHealth(healthScore)
    }

    private fun calculateHealthScore(
        ph: Float,
        nutrients: String,
        moisture: Int,
        temperature: Float
    ): Int {
        var score = 0

        score += when {
            ph in 6.0..7.5 -> 25
            ph in 5.5..8.0 -> 15
            else -> 5
        }

        score += when {
            moisture in 60..80 -> 25
            moisture in 40..90 -> 15
            else -> 5
        }

        score += when {
            temperature in 20f..30f -> 25
            temperature in 15f..35f -> 15
            else -> 5
        }

        val npkValues = nutrients.split("-").map { it.toIntOrNull() ?: 0 }
        score += when {
            npkValues.all { it >= 10 } -> 25
            npkValues.all { it >= 5 } -> 15
            else -> 5
        }

        return score
    }

    private fun showDetailedReportDialog() {
        val report = """
            Detailed Analysis:
            
            Soil Health: ${binding.soilHealthStatus.text}
            Description: ${binding.soilHealthDescription.text}
            
            Parameters:
            - pH Level: ${binding.phValue.text}
            - Temperature: ${binding.temperatureValue.text}
            - Moisture: ${binding.moistureValue.text}
            
            Nutrient Levels:
            - Nitrogen: ${binding.nutrientsValue.text} mg/kg
            - Phosphorus: ${binding.phosphorusValue.text} mg/kg
            - Potassium: ${binding.potassiumValue.text} mg/kg
        """.trimIndent()

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Detailed Analysis")
            .setMessage(report)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun downloadAndViewReport(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(url)
                type = "application/pdf"
            }
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Error opening PDF: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun createAndDownloadPDF() {
        try {
            val document = android.graphics.pdf.PdfDocument()
            val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 size
            val page = document.startPage(pageInfo)
            val canvas = page.canvas
            val paint = android.graphics.Paint()

            // Set up text paint
            paint.typeface = android.graphics.Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            paint.textSize = 12f
            paint.color = android.graphics.Color.BLACK

            // Draw border
            paint.style = android.graphics.Paint.Style.STROKE
            paint.strokeWidth = 2f
            canvas.drawRect(30f, 30f, 565f, 812f, paint)

            // Draw header with logo
            paint.style = android.graphics.Paint.Style.FILL
            paint.textSize = 24f
            paint.typeface = android.graphics.Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("Farm Report", 50f, 80f, paint)

            // Draw current date
            paint.textSize = 12f
            paint.typeface = android.graphics.Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            canvas.drawText("Date: ${dateFormat.format(Date())}", 50f, 100f, paint)

            // Draw location
            canvas.drawText("Location: ${binding.locationText.text}", 50f, 120f, paint)

            // Draw content
            paint.textSize = 14f
            val reportContent = generateQuickReport()
            val lines = reportContent.split("\n")
            var yPosition = 160f

            for (line in lines) {
                if (line.trim().isNotEmpty()) {
                    canvas.drawText(line, 50f, yPosition, paint)
                    yPosition += 25f
                }
            }

            // Draw footer with copyright
            paint.textSize = 10f
            canvas.drawText(" ${Calendar.getInstance().get(Calendar.YEAR)} Farm Management System. All rights reserved.", 50f, 780f, paint)

            document.finishPage(page)

            // Save the document
            val fileName = "farm_report_${System.currentTimeMillis()}.pdf"
            val file = File(requireContext().getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName)

            document.writeTo(FileOutputStream(file))
            document.close()

            // Show the PDF
            val uri = FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.provider",
                file
            )

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            }

            startActivity(intent)
            Toast.makeText(context, "Report downloaded successfully!", Toast.LENGTH_SHORT).show()

        } catch (e: Exception) {
            Toast.makeText(context, "Error creating PDF: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun convertRawPHToActualPH(rawPH: Float): Float {
        // This is a simplified conversion based on the Arduino code
        // Adjust the formula based on your specific pH sensor calibration
        // In the Arduino code, pH is calculated as: analogRead(PH_PIN) * (14.0 / 4095.0)
        return when {
            rawPH < 0 -> 7.0f  // Neutral pH as default for invalid readings
            rawPH > 14 -> 7.0f  // Neutral pH as default for out-of-range readings
            else -> rawPH
        }
    }

    private fun calculateMoistureFromEC(ec: Float): Int {
        // This is a simplified conversion. Adjust the formula based on your specific sensor and soil type
        return constrain((ec * 20).toInt(), 0, 100)
    }

    private fun constrain(value: Int, min: Int, max: Int): Int {
        return when {
            value < min -> min
            value > max -> max
            else -> value
        }
    }

    private fun assessSoilHealth(
        ph: Float,
        nitrogen: Int,
        phosphorus: Int,
        potassium: Int,
        temperature: Float,
        moisture: Int
    ): SoilHealthAssessment {
        // Calculate individual scores
        val phScore = when {
            ph < 5.5 || ph > 8.0 -> 0
            ph < 6.0 || ph > 7.5 -> 25
            ph < 6.5 || ph > 7.0 -> 50
            else -> 100
        }

        val nitrogenScore = when {
            nitrogen < 20 -> 0
            nitrogen < 40 -> 25
            nitrogen < 60 -> 50
            nitrogen < 80 -> 75
            else -> 100
        }

        val phosphorusScore = when {
            phosphorus < 10 -> 0
            phosphorus < 20 -> 25
            phosphorus < 40 -> 50
            phosphorus < 60 -> 75
            else -> 100
        }

        val potassiumScore = when {
            potassium < 20 -> 0
            potassium < 40 -> 25
            potassium < 60 -> 50
            potassium < 80 -> 75
            else -> 100
        }

        val temperatureScore = when {
            temperature < 10f || temperature > 40f -> 0
            temperature < 15f || temperature > 35f -> 25
            temperature < 20f || temperature > 30f -> 50
            temperature < 25f || temperature > 28f -> 75
            else -> 100
        }

        val moistureScore = when {
            moisture < 20 -> 0
            moisture < 40 -> 25
            moisture < 60 -> 50
            moisture < 80 -> 75
            else -> 100
        }

        // Calculate total score
        val totalScore = phScore + nitrogenScore + phosphorusScore +
                         potassiumScore + temperatureScore + moistureScore

        // Determine soil health status
        val status = when {
            totalScore < 150 -> SoilHealthAssessment.HealthStatus.POOR
            totalScore < 300 -> SoilHealthAssessment.HealthStatus.MODERATE
            totalScore < 450 -> SoilHealthAssessment.HealthStatus.GOOD
            else -> SoilHealthAssessment.HealthStatus.EXCELLENT
        }

        return SoilHealthAssessment(status, totalScore)
    }

    private fun convertSoilHealthToScore(assessment: SoilHealthAssessment): Int {
        return when (assessment.status) {
            SoilHealthAssessment.HealthStatus.POOR -> 25
            SoilHealthAssessment.HealthStatus.MODERATE -> 50
            SoilHealthAssessment.HealthStatus.GOOD -> 75
            SoilHealthAssessment.HealthStatus.EXCELLENT -> 100
        }
    }

    data class SoilHealthAssessment(
        val status: HealthStatus,
        val score: Int
    ) {
        enum class HealthStatus {
            POOR, MODERATE, GOOD, EXCELLENT
        }
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
    }

    // Handle location permission result
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                fetchCurrentLocation()
            } else {
                // Permission denied, use default location
                binding.locationText.text = "Farm Location: Nashik"
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
    }
}
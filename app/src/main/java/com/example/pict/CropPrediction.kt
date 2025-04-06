import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import com.example.pict.R
import okhttp3.*
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit
import java.util.logging.Level
import java.util.logging.Logger
import android.os.Handler
import android.os.Looper

class CropPrediction : Fragment() {
    private lateinit var etN: EditText
    private lateinit var etP: EditText
    private lateinit var etK: EditText
    private lateinit var etPH: EditText
    private lateinit var etSoilType: EditText
    private lateinit var btnPredict: Button
    private lateinit var tvResult: TextView
    private lateinit var resultCard: CardView
    private lateinit var cropImage: ImageView
    private lateinit var tvPlantingSeason: TextView
    private lateinit var tvWaterRequirement: TextView
    private lateinit var tvSoilType: TextView
    private lateinit var tvNutrients: TextView
    private lateinit var progressBar: ProgressBar  // Add this line
    
    companion object {
        private const val API_URL = "https://mlnew-10.onrender.com/predict"
        private const val BACKUP_API_URL = "https://mlnew-10.onrender.com/predict"
        private const val CACHE_SIZE = 20 * 1024 * 1024L // 20 MB cache
        private const val CONNECT_TIMEOUT = 60L // Increased connect timeout from 30 to 60 seconds
        private const val READ_TIMEOUT = 60L // Increased read timeout from 30 to 60 seconds
        private const val WRITE_TIMEOUT = 60L // Increased write timeout from 30 to 60 seconds
        private const val MAX_RETRIES = 3

        private var client: OkHttpClient? = null

        private fun getClient(context: Context): OkHttpClient {
            if (client == null) {
                val cacheDir = File(context.cacheDir, "http_cache")
                cacheDir.mkdirs()

                client = OkHttpClient.Builder()
                    .cache(Cache(cacheDir, CACHE_SIZE))
                    .connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
                    .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
                    .writeTimeout(WRITE_TIMEOUT, TimeUnit.SECONDS)
                    .addInterceptor { chain ->
                        var request = chain.request()
                        var retryCount = 0
                        var lastException: Exception? = null

                        while (retryCount < MAX_RETRIES) {
                            try {
                                // Try main API
                                val response = chain.proceed(request)
                                if (response.isSuccessful) {
                                    return@addInterceptor response
                                }
                                response.close()

                                // If main API fails, try backup API
                                if (request.url.toString() == API_URL) {
                                    request = request.newBuilder()
                                        .url(BACKUP_API_URL)
                                        .build()
                                    continue
                                }
                            } catch (e: Exception) {
                                lastException = e
                                Logger.getLogger("API_RETRY").log(Level.SEVERE, "Attempt ${retryCount + 1} failed", e)
                            }

                            retryCount++
                            if (retryCount < MAX_RETRIES) {
                                // Exponential backoff
                                Thread.sleep(1000L * (1 shl retryCount))
                            }
                        }

                        throw lastException ?: IOException("Failed after $MAX_RETRIES retries")
                    }
                    .build()
            }
            return client!!
        }

        // Improved cache with expiration
        private data class CacheEntry(
            val result: String,
            val timestamp: Long
        )

        private val predictionCache = object : LinkedHashMap<String, CacheEntry>(100, 0.75f, true) {
            private val MAX_ENTRIES = 100
            private val CACHE_DURATION = TimeUnit.HOURS.toMillis(1)

            override fun removeEldestEntry(eldest: Map.Entry<String, CacheEntry>): Boolean {
                val now = System.currentTimeMillis()
                return size > MAX_ENTRIES || now - eldest.value.timestamp > CACHE_DURATION
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(R.layout.fragment_crop_prediction, container, false)
        initializeViews(rootView)
        setupClickListeners()
        return rootView
    }

    private fun initializeViews(view: View) {
        etN = view.findViewById(R.id.etN)
        etP = view.findViewById(R.id.etP)
        etK = view.findViewById(R.id.etK)
        etPH = view.findViewById(R.id.etPH)
        etSoilType = view.findViewById(R.id.etSoilType)
        btnPredict = view.findViewById(R.id.btnPredict)
        tvResult = view.findViewById(R.id.tvResult)
        resultCard = view.findViewById(R.id.resultCard)
        cropImage = view.findViewById(R.id.cropImage)
        tvPlantingSeason = view.findViewById(R.id.tvPlantingSeason)
        tvWaterRequirement = view.findViewById(R.id.tvWaterRequirement)
        tvSoilType = view.findViewById(R.id.tvSoilType)
        tvNutrients = view.findViewById(R.id.tvNutrients)
        progressBar = view.findViewById(R.id.progressBar)  // Add this line

        // Initially hide the result card
        resultCard.visibility = View.GONE
    }

    private fun setupClickListeners() {
        btnPredict.setOnClickListener {
            if (validateInputs()) {
                predictCrop(
                    etN.text.toString(),
                    etP.text.toString(),
                    etK.text.toString(),
                    etPH.text.toString(),
                    etSoilType.text.toString()
                )
            }
        }
    }

    private fun validateInputs(): Boolean {
        if (etN.text.isBlank() || etP.text.isBlank() ||
            etK.text.isBlank() || etPH.text.isBlank() ||
            etSoilType.text.isBlank()) {
            Toast.makeText(requireContext(), "Please fill in all fields", Toast.LENGTH_SHORT).show()
            return false
        }

        try {
            val n = etN.text.toString().toFloat()
            val p = etP.text.toString().toFloat()
            val k = etK.text.toString().toFloat()
            val ph = etPH.text.toString().toFloat()

            if (n < 0 || p < 0 || k < 0) {
                Toast.makeText(requireContext(), "N, P, K values must be positive", Toast.LENGTH_SHORT).show()
                return false
            }

            if (ph < 0 || ph > 14) {
                Toast.makeText(requireContext(), "pH must be between 0 and 14", Toast.LENGTH_SHORT).show()
                return false
            }
        } catch (e: NumberFormatException) {
            Toast.makeText(requireContext(), "Please enter valid numbers", Toast.LENGTH_SHORT).show()
            return false
        }

        return true
    }

    private fun predictCrop(N: String, P: String, K: String, pH: String, soilType: String) {
        showLoading(true)
        
        // Add a timeout handler to inform the user if the request is taking too long
        val timeoutHandler = Handler(Looper.getMainLooper())
        val timeoutRunnable = Runnable {
            if (tvResult.text == "Predicting...") {
                tvResult.text = "Still predicting... This may take a moment."
                // Show a progress indicator that the request is still processing
                Toast.makeText(
                    requireContext(),
                    "The server is taking longer than expected. Please wait...",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
        
        // Schedule the timeout message after 15 seconds
        timeoutHandler.postDelayed(timeoutRunnable, 15000)
    
        val cacheKey = "$N-$P-$K-$pH-$soilType"
        val startTime = System.currentTimeMillis()
    
        // Check cache first
        predictionCache[cacheKey]?.let { cacheEntry ->
            if (System.currentTimeMillis() - cacheEntry.timestamp <= TimeUnit.HOURS.toMillis(1)) {
                timeoutHandler.removeCallbacks(timeoutRunnable) // Remove the timeout handler
                displayCropResult(cacheEntry.result, N, P, K, pH, soilType)
                showLoading(false)
                Logger.getLogger("PREDICTION_PERFORMANCE").log(Level.INFO, "Cache hit: $cacheKey")
                return
            } else {
                predictionCache.remove(cacheKey)
            }
        }
    
        val formBody = FormBody.Builder()
            .add("N", N)
            .add("P", P)
            .add("K", K)
            .add("pH", pH)
            .add("Soil_type", soilType)
            .build()
    
        val request = Request.Builder()
            .url(API_URL)
            .post(formBody)
            .build()
    
        getClient(requireContext()).newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                timeoutHandler.removeCallbacks(timeoutRunnable) // Remove the timeout handler
                val duration = System.currentTimeMillis() - startTime
                activity?.runOnUiThread {
                    showLoading(false)
                    when (e) {
                        is java.net.SocketTimeoutException -> {
                            // Offer offline prediction as a fallback
                            val offlinePrediction = getOfflinePrediction(N, P, K, pH, soilType)
                            if (offlinePrediction != null) {
                                Toast.makeText(context, "Using offline prediction due to timeout", Toast.LENGTH_LONG).show()
                                displayCropResult(offlinePrediction, N, P, K, pH, soilType)
                            } else {
                                showError("Request timed out. The server might be busy, please try again.")
                            }
                            Logger.getLogger("PREDICTION_PERFORMANCE").log(Level.WARNING, "Timeout after ${duration}ms")
                        }
                        is java.net.UnknownHostException -> {
                            showError("No internet connection. Please check your network and try again.")
                            Logger.getLogger("PREDICTION_PERFORMANCE").log(Level.WARNING, "No internet connection")
                        }
                        else -> {
                            showError("Network error: ${e.message ?: "Unknown error"}. Please try again.")
                            Logger.getLogger("PREDICTION_PERFORMANCE").log(Level.SEVERE, "Network error after ${duration}ms", e)
                        }
                    }
                }
            }
    
            override fun onResponse(call: Call, response: Response) {
                timeoutHandler.removeCallbacks(timeoutRunnable) // Remove the timeout handler
                val duration = System.currentTimeMillis() - startTime
                if (response.isSuccessful) {
                    try {
                        val responseData = response.body?.string()
                        val jsonResponse = JSONObject(responseData!!)
                        val predictedCrop = jsonResponse.getString("PredictedCrop")
    
                        predictionCache[cacheKey] = CacheEntry(
                            result = predictedCrop,
                            timestamp = System.currentTimeMillis()
                        )
    
                        Logger.getLogger("PREDICTION_PERFORMANCE").log(Level.INFO, "Successful prediction in ${duration}ms")
    
                        activity?.runOnUiThread {
                            displayCropResult(predictedCrop, N, P, K, pH, soilType)
                            showLoading(false)
                        }
                    } catch (e: Exception) {
                        Logger.getLogger("PREDICTION_PERFORMANCE").log(Level.SEVERE, "JSON parsing error after ${duration}ms", e)
                        activity?.runOnUiThread {
                            showLoading(false)
                            showError("Error processing response. Please try again.")
                        }
                    }
                } else {
                    Logger.getLogger("PREDICTION_PERFORMANCE").log(Level.WARNING, "Error response in ${duration}ms: ${response.code}")
                    handleErrorResponse(response)
                }
            }
        })
    }

    // Add a simple offline prediction method based on common crop requirements
    private fun getOfflinePrediction(N: String, P: String, K: String, pH: String, soilType: String): String? {
        try {
            val n = N.toFloat()
            val p = P.toFloat()
            val k = K.toFloat()
            val ph = pH.toFloat()
            
            // Very basic offline prediction logic
            return when {
                n > 80 && p > 40 && k > 40 && ph in 5.5f..7.0f -> "rice"
                n in 40f..120f && p in 30f..60f && k in 25f..40f && ph in 6.0f..7.5f -> "wheat"
                n > 80 && p > 50 && k > 30 && ph in 5.5f..7.0f -> "maize"
                n in 50f..120f && p in 25f..60f && k in 50f..70f && ph in 6.0f..8.0f -> "cotton"
                else -> null // Return null if we can't make a confident offline prediction
            }
        } catch (e: Exception) {
            return null
        }
    }

    private fun showLoading(isLoading: Boolean) {
        activity?.runOnUiThread {
            btnPredict.isEnabled = !isLoading
            if (isLoading) {
                tvResult.text = "Predicting..."
                resultCard.visibility = View.GONE
                // Add a progress bar to show the user that something is happening
                progressBar.visibility = View.VISIBLE
            } else {
                progressBar.visibility = View.GONE
            }
        }
    }

    private fun showError(message: String) {
        activity?.runOnUiThread {
            tvResult.text = "Error occurred"
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            btnPredict.isEnabled = true
        }
    }

    private fun handleErrorResponse(response: Response) {
        val errorMessage = try {
            val errorBody = response.body?.string()
            val jsonError = JSONObject(errorBody!!)
            jsonError.getString("error")
        } catch (e: Exception) {
            "Failed to predict crop (${response.code})"
        }

        activity?.runOnUiThread {
            showLoading(false)
            showError(errorMessage)
        }
    }

    private fun displayCropResult(crop: String, N: String, P: String, K: String, pH: String, soilType: String) {
        tvResult.text = "Predicted Crop: $crop"
        resultCard.visibility = View.VISIBLE

        // Set crop image
        cropImage.setImageResource(getCropImageResource(crop))

        // Set crop information
        tvPlantingSeason.text = "Planting Season: ${getCropSeason(crop)}"
        tvWaterRequirement.text = "Water Requirement: ${getCropWaterRequirement(crop)}"
        tvSoilType.text = "Soil Type: $soilType"
        tvNutrients.text = "Nutrients: N=$N, P=$P, K=$K, pH=$pH"
    }

    private fun getCropImageResource(cropName: String): Int {
        return when (cropName.lowercase()) {
            "rice" -> R.drawable.rice
            "wheat" -> R.drawable.wheat
            "maize" -> R.drawable.maize
            "cotton" -> R.drawable.cotton
            "watermelon" -> R.drawable.watermelon
            "banana" -> R.drawable.banana
            "mango" -> R.drawable.mango
            "grapes" -> R.drawable.grapes
            "orange" -> R.drawable.orange
            "apple" -> R.drawable.apple
            "papaya" -> R.drawable.papaya
            "coconut" -> R.drawable.coconut
            "jute" -> R.drawable.jute
            "coffee" -> R.drawable.cofee
            else -> R.drawable.crop // default crop icon
        }
    }

    private fun getCropSeason(cropName: String): String {
        return when (cropName.lowercase()) {
            "rice" -> "June-July (Kharif)"
            "wheat" -> "October-November (Rabi)"
            "maize" -> "June-July (Kharif)"
            "cotton" -> "March-May (Summer)"
            "sugarcane" -> "January-March"
            "banana" -> "June-July"
            "mango" -> "December-January"
            "grapes" -> "January-February"
            "orange" -> "June-July"
            "apple" -> "November-December"
            "papaya" -> "June-July"
            "coconut" -> "May-June"
            "jute" -> "March-April"
            "coffee" -> "November-December"
            else -> "Season varies by region"
        }
    }

    private fun getCropWaterRequirement(cropName: String): String {
        return when (cropName.lowercase()) {
            "rice" -> "High (150-300 cm)"
            "wheat" -> "Medium (45-100 cm)"
            "maize" -> "Medium (50-80 cm)"
            "cotton" -> "Medium (60-100 cm)"
            "sugarcane" -> "High (150-250 cm)"
            "banana" -> "High (120-180 cm)"
            "mango" -> "Medium (100-150 cm)"
            "grapes" -> "Medium (60-80 cm)"
            "orange" -> "Medium (80-120 cm)"
            "apple" -> "Medium (100-120 cm)"
            "papaya" -> "Medium (80-120 cm)"
            "coconut" -> "High (150-250 cm)"
            "jute" -> "High (120-180 cm)"
            "coffee" -> "Medium (150-200 cm)"
            else -> "Requirement varies"
        }
    }
}

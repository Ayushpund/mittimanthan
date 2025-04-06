package com.example.pict

import android.R.layout.simple_spinner_dropdown_item
import android.R.layout.simple_spinner_item
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Spinner
import android.widget.ArrayAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.chip.ChipGroup
import com.google.android.material.button.MaterialButton
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import android.util.Log
import okhttp3.FormBody
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException
import okhttp3.Callback
import java.util.concurrent.TimeUnit
import android.content.Context
import android.os.Bundle
import android.view.Gravity
import android.widget.AdapterView
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import java.io.File
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import com.example.pict.ChatMessage
import org.json.JSONException
import java.util.logging.Logger

class ChatbotFragment : BottomSheetDialogFragment() {
    private lateinit var queryEditText: EditText
    private lateinit var languageSpinner: Spinner
    private lateinit var chatRecyclerView: RecyclerView
    private lateinit var chatAdapter: ChatAdapter
    private lateinit var faqChipGroup: ChipGroup
    private lateinit var sendButton: MaterialButton

    companion object {
        private const val CHAT_API_URL = "https://mlnew-15.onrender.com/chat"
        private const val BACKUP_CHAT_API_URL = "https://mlnew-15.onrender.com/chat"
        private const val CACHE_SIZE = 100 * 1024 * 1024L // 100 MB cache
        private const val TIMEOUT = 15L // Increased timeout
        private const val MAX_RETRIES = 3
        private const val CACHE_DURATION_MINUTES = 30 // Cache responses for 30 minutes

        // Enhanced logging
        private val logger = Logger.getLogger("ChatbotAPI")

        // Improved caching mechanism
        private val chatResponseCache = object : LinkedHashMap<String, CacheEntry>(50, 0.75f, true) {
            override fun removeEldestEntry(eldest: Map.Entry<String, CacheEntry>): Boolean {
                val isOldest = size > 50
                if (isOldest) {
                    logger.info("Removing eldest chat cache entry: ${eldest.key}")
                }
                return isOldest
            }
        }

        // Enhanced cache entry
        private data class CacheEntry(
            val answer: String,
            val timestamp: Long,
            val language: String
        )

        // Helper function to create cache entry
        private fun createCacheEntry(answer: String, language: String): CacheEntry {
            return CacheEntry(
                answer = answer,
                timestamp = System.currentTimeMillis(),
                language = language
            )
        }

        // Enhanced client creation
        private var client: OkHttpClient? = null

        private fun getClient(context: Context): OkHttpClient {
            if (client == null) {
                val cacheDir = File(context.cacheDir, "chatbot_cache")
                cacheDir.mkdirs()

                client = OkHttpClient.Builder()
                    .cache(Cache(cacheDir, CACHE_SIZE))
                    .connectTimeout(TIMEOUT, TimeUnit.SECONDS)
                    .readTimeout(TIMEOUT, TimeUnit.SECONDS)
                    .writeTimeout(TIMEOUT, TimeUnit.SECONDS)
                    .dispatcher(Dispatcher().apply {
                        maxRequestsPerHost = 10
                        maxRequests = 20
                    })
                    .addInterceptor { chain ->
                        val originalRequest = chain.request()
                        var request = originalRequest
                        var response: Response? = null
                        var lastException: Exception? = null

                        // Retry mechanism with exponential backoff
                        for (attempt in 0 until MAX_RETRIES) {
                            try {
                                response = chain.proceed(request)

                                logger.info("Chat API Request: ${request.url}, Attempt: $attempt, Response Code: ${response.code}")

                                if (response.isSuccessful) {
                                    return@addInterceptor response
                                }

                                // Try backup URL on subsequent attempts
                                if (attempt < MAX_RETRIES - 1 && originalRequest.url.toString() == CHAT_API_URL) {
                                    request = originalRequest.newBuilder().url(BACKUP_CHAT_API_URL).build()
                                }

                                // Exponential backoff
                                Thread.sleep(1000L * (attempt + 1))
                            } catch (e: Exception) {
                                lastException = e
                                logger.warning("Chat API Request failed: ${e.message}")

                                // Exponential backoff
                                Thread.sleep(1000L * (attempt + 1))
                            }
                        }

                        lastException?.let { throw it }
                        response ?: throw IOException("No response received")
                    }
                    .build()
            }
            return client!!
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_chatbot, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initializeViews(view)
        setupSpinner()
        setupRecyclerView()
        setupClickListeners()
        setupFAQChips()
        showWelcomeMessage()
    }

    private fun initializeViews(view: View) {
        queryEditText = view.findViewById(R.id.queryEditText)
        languageSpinner = view.findViewById(R.id.languageSpinner)
        chatRecyclerView = view.findViewById(R.id.chatRecyclerView)
        faqChipGroup = view.findViewById(R.id.faqChipGroup)
        sendButton = view.findViewById(R.id.sendButton)
    }

    private fun setupSpinner() {
        val languages = arrayOf("en", "hi", "mr")
        val adapter = ArrayAdapter(requireContext(), simple_spinner_item, languages)
        adapter.setDropDownViewResource(simple_spinner_dropdown_item)
        languageSpinner.adapter = adapter

        languageSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                chatAdapter.clear()
                showWelcomeMessage()
                setupFAQChips()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupRecyclerView() {
        chatAdapter = ChatAdapter()
        chatRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = chatAdapter
        }
    }

    private fun setupClickListeners() {
        sendButton.setOnClickListener {
            val query = queryEditText.text.toString().trim()
            if (query.isNotEmpty()) {
                sendMessage(query)
            }
        }
    }

    private val chipsTranslationMap = mapOf(
        "en" to listOf(
            "Soil Organic" to "How do I measure soil organic carbon content?",
            "Crop Management" to "What are the best practices for crop management?",
            "Fertilizer" to "How to choose the right fertilizer for my crops?",
            "Market Prices" to "How can I get current crop market prices?"
        ),
        "hi" to listOf(
            "मिट्टी जैविक" to "मैं मिट्टी के कार्बनिक कार्बन की मात्रा कैसे मापूं?",
            "फसल प्रबंधन" to "फसल प्रबंधन के लिए सर्वोत्तम प्रथाएं क्या हैं?",
            "उर्वरक" to "अपनी फसलों के लिए सही उर्वरक कैसे चुनें?",
            "बाजार भाव" to "मौजूदा फसल बाजार भाव कैसे प्राप्त करें?"
        ),
        "mr" to listOf(
            "माती जैविक" to "मी मातीचे कार्बनिक कार्बन प्रमाण कसे मोजू?",
            "पीक व्यवस्थापन" to "पीक व्यवस्थापनासाठी सर्वोत्तम पद्धती काय आहेत?",
            "खत" to "माझ्या पिकांसाठी योग्य खत कसे निवडावे?",
            "बाजार भाव" to "सध्याचे पीक बाजार भाव कसे मिळवावेत?"
        )
    )

    private fun setupFAQChips() {
        // Get the current language
        val language = languageSpinner.selectedItem.toString().lowercase()

        // Get the chips for the selected language, default to English
        val currentLanguageChips = chipsTranslationMap[language]
            ?: chipsTranslationMap["en"]!!

        // Clear existing chips
        faqChipGroup.removeAllViews()

        // Create chips for each FAQ
        currentLanguageChips.forEach { (chipLabel, chipQuestion) ->
            val chip = com.google.android.material.chip.Chip(requireContext()).apply {
                text = chipLabel
                isCheckable = false
                setChipBackgroundColorResource(R.color.colorPrimary)
                setTextColor(ContextCompat.getColor(context, android.R.color.white))
                setOnClickListener {
                    queryEditText.setText(chipQuestion)
                    sendMessage(chipQuestion)
                }
            }
            faqChipGroup.addView(chip)
        }
    }

    // Enhanced sendMessage function
    private fun sendMessage(message: String) {
        chatAdapter.addMessage(ChatMessage(message, isBot = false, isLoading = false))
        queryEditText.setText("")
        chatRecyclerView.smoothScrollToPosition(chatAdapter.itemCount - 1)

        val language = languageSpinner.selectedItem.toString().lowercase()

        // Check cache first
        val cacheKey = "$message-$language"
        chatResponseCache[cacheKey]?.let { cacheEntry ->
            val currentTime = System.currentTimeMillis()
            if (currentTime - cacheEntry.timestamp <= TimeUnit.MINUTES.toMillis(
                    CACHE_DURATION_MINUTES.toLong()
                )) {
                logger.info("Chat cache hit for key: $cacheKey")
                chatAdapter.addMessage(ChatMessage(cacheEntry.answer, isBot = true, isLoading = false))
                chatRecyclerView.smoothScrollToPosition(chatAdapter.itemCount - 1)
                return
            }
        }

        chatAdapter.addMessage(ChatMessage("Finding an answer...", isBot = true, isLoading = true))

        val formBody = FormBody.Builder()
            .add("query", message)
            .add("language", language)
            .build()

        val request = Request.Builder()
            .url(CHAT_API_URL)
            .post(formBody)
            .build()

        getClient(requireContext()).newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                activity?.runOnUiThread {
                    chatAdapter.removeLoadingMessages()
                    logger.severe("Chat API call failed: ${e.message}")

                    val errorMessage = when (e) {
                        is SocketTimeoutException -> "Response taking too long. Please try again."
                        is UnknownHostException -> "No internet connection. Please check your network."
                        else -> "Network error. Please try again."
                    }

                    chatAdapter.addMessage(ChatMessage(errorMessage, isBot = true, isLoading = false))
                    chatRecyclerView.smoothScrollToPosition(chatAdapter.itemCount - 1)
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseData = response.body?.string()
                activity?.runOnUiThread {
                    chatAdapter.removeLoadingMessages()

                    try {
                        if (!response.isSuccessful || responseData.isNullOrEmpty()) {
                            throw IOException("Unsuccessful response or empty body")
                        }

                        val jsonResponse = JSONObject(responseData)

                        // More robust response parsing
                        val answer = when {
                            jsonResponse.has("answer") -> jsonResponse.getString("answer")
                            jsonResponse.has("response") -> jsonResponse.getString("response")
                            jsonResponse.has("message") -> jsonResponse.getString("message")
                            else -> throw IllegalArgumentException("No valid response found")
                        }

                        // Cache the response
                        chatResponseCache[cacheKey] = createCacheEntry(
                            answer = answer,
                            language = language
                        )

                        chatAdapter.addMessage(ChatMessage(answer, isBot = true, isLoading = false))
                        chatRecyclerView.smoothScrollToPosition(chatAdapter.itemCount - 1)
                    } catch (e: Exception) {
                        logger.severe("Chat response parsing error: ${e.message}")

                        val errorMessage = when (e) {
                            is JSONException -> "Error processing server response"
                            is IllegalArgumentException -> "Invalid response from server"
                            else -> "Unexpected error occurred"
                        }

                        chatAdapter.addMessage(ChatMessage(errorMessage, isBot = true, isLoading = false))
                        chatRecyclerView.smoothScrollToPosition(chatAdapter.itemCount - 1)
                    }
                }
            }
        })
    }

    private fun showWelcomeMessage() {
        val welcomeMessages = mapOf(
            "hi" to """
                नमस्ते! मैं आपकी मिट्टी मंथन सहायक हूं।
                मैं आपकी निम्नलिखित मदद कर सकता हूं:
                - मिट्टी की गुणवत्ता की जांच
                - उपयुक्त फसलों की सिफारिश
                - उर्वरक सलाह
                नीचे दिए गए सामान्य प्रश्नों को देखें या कुछ भी पूछें!
            """.trimIndent(),

            "mr" to """
                नमस्कार! मी तुमचा मृदा मंथन सहाय्यक आहे.
                मी तुम्हाला खालील बाबतीत मदत करू शकतो:
                - माती परीक्षण
                - योग्य पिकांची शिफारस
                - खत सल्ला
                खाली दिलेले सामान्य प्रश्न पहा किंवा काहीही विचारा!
            """.trimIndent(),

            "en" to """
                Hello! I'm your Mittimanthan Assistant.
                I can help you with:
                - Soil quality analysis
                - Crop recommendations
                - Fertilizer advice
                Check out the common questions below or ask anything!
            """.trimIndent()
        )

        val language = languageSpinner.selectedItem.toString().lowercase()
        val message = welcomeMessages[language] ?: welcomeMessages["en"]!!
        chatAdapter.addMessage(ChatMessage(message, isBot = true, isLoading = false))
        chatRecyclerView.scrollToPosition(chatAdapter.itemCount - 1)
    }

    private inner class ChatAdapter : RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {
        private val messages = mutableListOf<ChatMessage>()

        fun addMessage(message: ChatMessage) {
            messages.add(message)
            notifyItemInserted(messages.size - 1)
        }

        fun clear() {
            val size = messages.size
            messages.clear()
            notifyItemRangeRemoved(0, size)
        }

        fun removeLoadingMessages() {
            messages.removeAll { it.isLoading }
            notifyDataSetChanged()
        }

        fun removeLastMessage() {
            if (messages.isNotEmpty()) {
                messages.removeAt(messages.size - 1)
                notifyItemRemoved(messages.size)
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.chat_message_item, parent, false)
            return ChatViewHolder(view)
        }

        override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
            val message = messages[position]
            holder.bind(message)
        }

        override fun getItemCount() = messages.size

        inner class ChatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val messageText: TextView = itemView.findViewById(R.id.messageText)
            private val messageCard: androidx.cardview.widget.CardView = itemView.findViewById(R.id.messageCard)
            private val loadingIndicator: android.widget.ProgressBar = itemView.findViewById(R.id.loadingIndicator)

            fun bind(chatMessage: ChatMessage) {
                messageText.text = chatMessage.text

                val params = messageCard.layoutParams as FrameLayout.LayoutParams
                if (chatMessage.isBot) {
                    params.gravity = Gravity.START
                    messageCard.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.chatbot_bot_message))
                } else {
                    params.gravity = Gravity.END
                    messageCard.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.chatbot_user_message))
                }
                messageCard.layoutParams = params

                loadingIndicator.visibility = if (chatMessage.isLoading) View.VISIBLE else View.GONE
            }
        }
    }
}

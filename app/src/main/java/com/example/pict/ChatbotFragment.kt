package com.example.pict

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Spinner
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SoilChatbotFragment : BottomSheetDialogFragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var queryEditText: EditText  // Changed from messageInput to queryEditText
    private lateinit var sendButton: ImageButton
    private val chatMessages = mutableListOf<ChatMessage>()
    private lateinit var adapter: ChatAdapter
    private var currentLanguage = "English" // Default language

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_chatbot, container, false)

        recyclerView = view.findViewById(R.id.chatRecyclerView)
        queryEditText =
            view.findViewById(R.id.queryEditText)  // Changed from messageInput to queryEditText
        sendButton = view.findViewById(R.id.sendButton)

        // Initialize the new UI elements
        val languageSpinner = view.findViewById<Spinner>(R.id.languageSpinner)
        val faqChipGroup = view.findViewById<ChipGroup>(R.id.faqChipGroup)

        // Set up language spinner with listener
        val languages = arrayOf("English", "Hindi", "Marathi")
        val spinnerAdapter =
            ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, languages)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        languageSpinner.adapter = spinnerAdapter

        // Add language selection listener
        languageSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                currentLanguage = languages[position]
                // Update welcome message when language changes
                if (chatMessages.isNotEmpty() && !chatMessages[0].isUser) {
                    // Clear previous messages and add new welcome message in selected language
                    chatMessages.clear()
                    addBotMessage(getWelcomeMessage(currentLanguage))
                    adapter.notifyDataSetChanged()
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Do nothing
            }
        }

        // Set up FAQ chip group click listeners
        for (i in 0 until faqChipGroup.childCount) {
            val chip = faqChipGroup.getChildAt(i) as Chip
            chip.setOnClickListener {
                val query = chip.text.toString()
                queryEditText.setText(query)  // Changed from messageInput to queryEditText
            }
        }

        setupRecyclerView()
        setupClickListeners()

        // Add welcome message in default language
        addBotMessage(getWelcomeMessage(currentLanguage))

        return view
    }

    private fun setupRecyclerView() {
        adapter = ChatAdapter(chatMessages)
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = adapter
    }

    private fun setupClickListeners() {
        sendButton.setOnClickListener {
            val message =
                queryEditText.text.toString().trim()  // Changed from messageInput to queryEditText
            if (message.isNotEmpty()) {
                addUserMessage(message)
                queryEditText.text.clear()  // Changed from messageInput to queryEditText

                // Process the message and generate a response in the selected language
                processUserMessage(message, currentLanguage)
            }
        }
    }

    private fun addUserMessage(message: String) {
        chatMessages.add(ChatMessage(message, isUser = true))
        adapter.notifyItemInserted(chatMessages.size - 1)
        recyclerView.scrollToPosition(chatMessages.size - 1)
    }

    private fun addBotMessage(message: String) {
        chatMessages.add(ChatMessage(message, isUser = false))
        adapter.notifyItemInserted(chatMessages.size - 1)
        recyclerView.scrollToPosition(chatMessages.size - 1)
    }

    private fun processUserMessage(message: String, language: String) {
        // Translate user message if needed
        val translatedMessage = translateMessage(message, language, "English")

        // Simple keyword-based responses
        val response = when {
            translatedMessage.contains("ph", ignoreCase = true) ->
                getPhResponse(language)

            translatedMessage.contains("moisture", ignoreCase = true) ->
                getMoistureResponse(language)

            translatedMessage.contains("temperature", ignoreCase = true) ->
                getTemperatureResponse(language)

            translatedMessage.contains("nutrient", ignoreCase = true) ||
                    translatedMessage.contains("fertilizer", ignoreCase = true) ->
                getNutrientResponse(language)

            translatedMessage.contains("crop", ignoreCase = true) ||
                    translatedMessage.contains("plant", ignoreCase = true) ->
                getCropResponse(language)

            translatedMessage.contains("organic", ignoreCase = true) ->
                getOrganicResponse(language)

            translatedMessage.contains("hello", ignoreCase = true) ||
                    translatedMessage.contains("hi", ignoreCase = true) ->
                getGreetingResponse(language)

            translatedMessage.contains("thank", ignoreCase = true) ->
                getThankYouResponse(language)

            else -> getDefaultResponse(language)
        }

        // Add a small delay to make it feel more natural
        recyclerView.postDelayed({
            addBotMessage(response)
        }, 500)
    }

    // Helper methods for multilingual responses

    private fun translateMessage(
        message: String,
        fromLanguage: String,
        toLanguage: String
    ): String {
        // In a real app, you would use a translation API here
        // For this example, we'll just return the original message
        // You could integrate Google Translate API or another translation service
        return message
    }

    private fun getWelcomeMessage(language: String): String {
        return when (language) {
            "Hindi" -> "नमस्ते! मैं आपका मिट्टी स्वास्थ्य सहायक हूं। आज मैं आपकी कैसे मदत कर सकता हूं?"
            "Marathi" -> "नमस्कार! मी तुमचा माती आरोग्य सहाय्यक आहे. आज मी तुम्हाला कशी मदत करू शकतो?"
            else -> "Hello! I'm your Soil Health Assistant. How can I help you today?"
        }
    }

    private fun getPhResponse(language: String): String {
        return when (language) {
            "Hindi" -> "अधिकांश फसलों के लिए आदर्श पीएच 6.0 और 7.5 के बीच होता है। यह रेंज इष्टतम पोषक तत्व उपलब्धता की अनुमति देती है।"
            "Marathi" -> "बहुतेक पिकांसाठी आदर्श पीएच 6.0 आणि 7.5 दरम्यान असतो. हा श्रेणी पोषक तत्वांच्या इष्टतम उपलब्धतेस अनुमती देतो."
            else -> "The ideal pH for most crops is between 6.0 and 7.5. This range allows for optimal nutrient availability."
        }
    }

    private fun getMoistureResponse(language: String): String {
        return when (language) {
            "Hindi" -> "इष्टतम विकास के लिए मिट्टी की नमी आमतौर पर 60-80% के बीच होनी चाहिए। बहुत अधिकाक पानी से जड़ सड़न हो सकती है, जबकि बहुत कम होने से सूखा तनाव हो सकता है।"
            "Marathi" -> "इष्टतम वाढीसाठी मातीतील ओलावा सामान्यतः 60-80% दरम्यान असावा. खूप जास्त पाणी मुळांना सडण्यास कारणीभूत ठरू शकते, तर खूप कमी असल्यास दुष्काळी ताण निर्माण होऊ शकतो."
            else -> "Soil moisture should typically be between 60-80% for optimal growth. Too much water can lead to root rot, while too little can cause drought stress."
        }
    }

    private fun getTemperatureResponse(language: String): String {
        return when (language) {
            "Hindi" -> "मिट्टी का तापमान बीज अंकुरण और जड़ विकास को प्रभावित करता है। अधिकांश फसलें 20-30°C के बीच मिट्टी के तापमान को पसंद करती हैं।"
            "Marathi" -> "मातीचे तापमान बियाणे अंकुरण आणि मुळांच्या वाढीवर परिणाम करते. बहुतेक पिके 20-30°C दरम्यान मातीचे तापमान पसंत करतात."
            else -> "Soil temperature affects seed germination and root growth. Most crops prefer soil temperatures between 20-30°C."
        }
    }

    private fun getNutrientResponse(language: String): String {
        return when (language) {
            "Hindi" -> "पौधों के लिए तीन प्राथमिक पोषक तत्व नाइट्रोजन (N), फॉस्फोरस (P), और पोटैशियम (K) हैं। नाइट्रोजन पत्ती विकास को बढ़ावा देता है, फॉस्फोरस जड़ और फूल विकास का समर्थन करता है, और पोटैशियम समग्र पौधे के स्वास्थ्य को बढ़ाता है।"
            "Marathi" -> "वनस्पतींसाठी तीन प्राथमिक पोषक तत्त्वे नायट्रोजन (N), फॉस्फरस (P) आणि पोटॅशियम (K) आहेत. नायट्रोजन पानांच्या वाढीस प्रोत्साहन देते, फॉस्फरस मुळे आणि फुलांच्या विकासास समर्थन देते आणि पोटॅशियम एकूण वनस्पतीच्या आरोग्यास वाढवते."
            else -> "The three primary nutrients for plants are Nitrogen (N), Phosphorus (P), and Potassium (K). Nitrogen promotes leaf growth, Phosphorus supports root and flower development, and Potassium enhances overall plant health."
        }
    }

    private fun getCropResponse(language: String): String {
        return when (language) {
            "Hindi" -> "विभिन्न फसलों की मिट्टी की आवश्यकताएं अलग-अलग होती हैं। आप किस विशिष्ट फसल को उगाने में रुचि रखते हैं?"
            "Marathi" -> "वेगवेगळ्या पिकांच्या मातीच्या आवश्यकता वेगवेगळ्या असतात. तुम्हाला कोणते विशिष्ट पीक लावण्यात स्वारस्य आहे?"
            else -> "Different crops have different soil requirements. What specific crop are you interested in growing?"
        }
    }

    private fun getOrganicResponse(language: String): String {
        return when (language) {
            "Hindi" -> "जैविक पदार्थ मिट्टी की संरचना, जल धारण क्षमता और पोषक तत्वों की उपलब्धता में सुधार करता है। खाद या गोबर जोड़ने से जैविक सामग्री बढ़ सकती है।"
            "Marathi" -> "सेंद्रिय पदार्थ मातीची रचना, पाणी धारण क्षमता आणि पोषक तत्वांची उपलब्धता सुधारते. कंपोस्ट किंवा शेणखत जोडल्याने सेंद्रिय सामग्री वाढू शकते."
            else -> "Organic matter improves soil structure, water retention, and nutrient availability. Adding compost or manure can increase organic content."
        }
    }

    private fun getGreetingResponse(language: String): String {
        return when (language) {
            "Hindi" -> "नमस्ते! मैं आज आपके मिट्टी स्वास्थ्य प्रश्नों के साथ आपकी कैसे सहायता कर सकता हूं?"
            "Marathi" -> "नमस्कार! मी आज तुमच्या माती आरोग्य प्रश्नांसह तुम्हाला कशी मदत करू शकतो?"
            else -> "Hello! How can I assist you with your soil health questions today?"
        }
    }

    private fun getThankYouResponse(language: String): String {
        return when (language) {
            "Hindi" -> "आपका स्वागत है! यदि आपके पास मिट्टी के स्वास्थ्य के बारे में कोई अन्य प्रश्न हैं तो बेझिझक पूछें।"
            "Marathi" -> "स्वागत आहे! तुम्हाला माती आरोग्याबद्दल इतर काही प्रश्न असल्यास विचारण्यास मोकळे असा."
            else -> "You're welcome! Feel free to ask if you have any other questions about soil health."
        }
    }

    private fun getDefaultResponse(language: String): String {
        return when (language) {
            "Hindi" -> "मुझे इसके बारे में निश्चित नहीं है। क्या आप मिट्टी के पीएच, नमी, तापमान, या पोषक तत्वों के बारे में जानना चाहेंगे?"
            "Marathi" -> "मला याबद्दल खात्री नाही. तुम्हाला माती पीएच, ओलावा, तापमान किंवा पोषक तत्त्वांबद्दल जाणून घ्यायचे आहे का?"
            else -> "I'm not sure about that. Would you like to know about soil pH, moisture, temperature, or nutrients?"
        }
    }

    data class ChatMessage(
        val message: String,
        val isUser: Boolean,
        val timestamp: String = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
    )

    inner class ChatAdapter(private val messages: List<ChatMessage>) :
        RecyclerView.Adapter<ChatAdapter.MessageViewHolder>() {

        inner class MessageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val messageText: TextView = view.findViewById(R.id.messageText)
            val timeText: TextView = view.findViewById(R.id.timeText)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
            val layout = if (viewType == 0) {
                R.layout.item_message_user
            } else {
                R.layout.item_message_bot
            }

            val view = LayoutInflater.from(parent.context)
                .inflate(layout, parent, false)

            return MessageViewHolder(view)
        }

        override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
            val message = messages[position]
            holder.messageText.text = message.message
            holder.timeText.text = message.timestamp
        }

        override fun getItemCount() = messages.size

        override fun getItemViewType(position: Int): Int {
            return if (messages[position].isUser) 0 else 1
        }
    }
}
package com.example.pict

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
// Add these imports if they're missing
import android.content.res.ColorStateList
import android.net.Uri
import androidx.core.content.ContextCompat
import android.net.Uri.parse
import android.os.Bundle
import android.telephony.SmsManager
import android.telephony.SmsMessage
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast

import androidx.activity.result.contract.ActivityResultContracts
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import java.io.File
import java.util.*
import java.text.SimpleDateFormat
import java.util.Locale

data class SoilDoctor(
    val id: Int,
    val name: String,
    val expertise: String,
    val phoneNumber: String,
    val availability: String,
    val fee: Double
)

data class Appointment(
    val doctorId: Int,
    val date: String,
    val time: String,
    val status: String = "PENDING"
)

class SmsReceiver : BroadcastReceiver() {
    var organicFarming: OrganicFarming? = null

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != "android.provider.Telephony.SMS_RECEIVED") return

        val bundle = intent.extras
        if (bundle != null) {
            val pdus = bundle["pdus"] as Array<*>?
            pdus?.let {
                for (pdu in it) {
                    val smsMessage = SmsMessage.createFromPdu(pdu as ByteArray)
                    val phoneNumber = smsMessage.originatingAddress ?: continue
                    val messageBody = smsMessage.messageBody ?: continue

                    // Check if this SMS is from one of our doctors
                    organicFarming?.let { fragment ->
                        fragment.selectedDoctor?.let { doctor ->
                            if (phoneNumber.contains(doctor.phoneNumber.replace("+", ""))) {
                                // Handle the response on the main thread
                                fragment.activity?.runOnUiThread {
                                    fragment.handleSmsResponse(messageBody)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

class OrganicFarming : Fragment() {

    // Inner interface for doctor click events
    interface OnDoctorClickListener {
        fun onDoctorClick(doctor: SoilDoctor)
    }

    // Inner class for DoctorAdapter
    private inner class DoctorAdapter(
        private val doctors: List<SoilDoctor>,
        private val listener: OnDoctorClickListener
    ) : RecyclerView.Adapter<DoctorAdapter.DoctorViewHolder>() {

        private var selectedPosition = -1

        inner class DoctorViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val card: MaterialCardView = view.findViewById(R.id.doctorCard)
            val name: TextView = view.findViewById(R.id.doctorName)
            val expertise: TextView = view.findViewById(R.id.doctorExpertise)
            val experience: TextView = view.findViewById(R.id.doctorExperience)
            val rating: TextView = view.findViewById(R.id.doctorRating)
            val appointmentDate: TextView = view.findViewById(R.id.appointmentDate)
            val consultationFee: TextView = view.findViewById(R.id.consultationFee)
            val confirmationStatus: TextView = view.findViewById(R.id.confirmationStatus)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DoctorViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.doctor_item, parent, false)
            return DoctorViewHolder(view)
        }

        override fun onBindViewHolder(holder: DoctorViewHolder, @SuppressLint("RecyclerView") position: Int) {
            val doctor = doctors[position]

            // Set card background tint based on selection
            holder.card.setCardBackgroundColor(
                if (position == selectedPosition)
                    ContextCompat.getColor(holder.itemView.context, R.color.confirmation_green)
                else
                    ContextCompat.getColor(holder.itemView.context, R.color.white)
            )

            // Set text colors based on selection
            val textColor = if (position == selectedPosition)
                ContextCompat.getColor(holder.itemView.context, R.color.white)
            else
                ContextCompat.getColor(holder.itemView.context, R.color.text_primary)

            // Set doctor details
            holder.name.text = doctor.name
            holder.name.setTextColor(textColor)

            holder.expertise.text = doctor.expertise
            holder.expertise.setTextColor(textColor)

            holder.experience.text = "8 years"
            holder.experience.setTextColor(textColor)

            holder.rating.text = "Rating: 4.3"
            holder.rating.setTextColor(if (position == selectedPosition) textColor else ContextCompat.getColor(holder.itemView.context, R.color.rating_orange))

            // Handle click event
            holder.itemView.setOnClickListener {
                val previousPosition = selectedPosition
                selectedPosition = position
                notifyItemChanged(previousPosition)
                notifyItemChanged(selectedPosition)
                listener.onDoctorClick(doctor)
            }
        }

        override fun getItemCount() = doctors.size
    }

    private val doctors = listOf(
        SoilDoctor(1, "Dr. Sarah Green", "Soil Microbiology", "8446204947", "Mon-Fri, 9AM-5PM", 500.0),
        SoilDoctor(2, "Dr. John Earth", "Soil Chemistry", "+1234567891", "Mon-Wed, 10AM-6PM", 500.0),
        SoilDoctor(3, "Dr. Mike Ground", "Soil Conservation", "+1234567892", "Tue-Sat, 8AM-4PM", 500.0)
    )

    private lateinit var smsReceiver: SmsReceiver
    var selectedDoctor: SoilDoctor? = null
    private var selectedAppointment: Appointment? = null
    private var pdfUri: Uri? = null
    private var appointmentConfirmed = false
    // Add these missing variables
    private var isPdfUploadedAndPaymentPending = false
    private var selectedDoctorForPayment: SoilDoctor? = null

    private lateinit var doctorsRecyclerView: RecyclerView
    private lateinit var appointmentConfirmationCard: CardView
    private lateinit var soilReportCard: CardView
    private lateinit var selectReportButton: MaterialButton
    private lateinit var selectedFileText: TextView
    private lateinit var doctorNameConfirmed: TextView
    private lateinit var appointmentDateTime: TextView
    private lateinit var consultationFee: TextView
    private lateinit var confirmationStatus: TextView
    private lateinit var doctorPhoneNumber: TextView
    private lateinit var stepOneIcon: ImageView
    private lateinit var stepTwoIcon: ImageView
    private lateinit var stepThreeIcon: ImageView
    private var pdfPreviewImage: ImageView? = null

    private val pdfPickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            pdfUri = it
            selectedFileText.text = it.lastPathSegment ?: "Selected PDF"

            // Send PDF to doctor's WhatsApp if appointment is confirmed
            if (appointmentConfirmed) {
                selectedDoctor?.let { doctor ->
                    processPdfSharing()
                }
            }
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            selectedDoctor?.let { doctor ->
                sendAppointmentRequest(doctor)
            }
        } else {
            Toast.makeText(context, "SMS permission is required to book appointments", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        smsReceiver = SmsReceiver().apply {
            organicFarming = this@OrganicFarming
        }
        checkSmsPermission()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Change from token_fragment to fragment_organic_farming
        val view = inflater.inflate(R.layout.token_fragment, container, false)
        
        // Initialize views
        doctorsRecyclerView = view.findViewById(R.id.doctorsRecyclerView)
        appointmentConfirmationCard = view.findViewById<CardView>(R.id.appointmentConfirmationCard) as CardView
        soilReportCard = view.findViewById<CardView>(R.id.soilReportCard) as CardView
        selectReportButton = view.findViewById(R.id.selectReportButton)
        selectedFileText = view.findViewById(R.id.selectedFileText)
        doctorNameConfirmed = view.findViewById(R.id.doctorNameConfirmed)
        appointmentDateTime = view.findViewById(R.id.appointmentDateTime)
        consultationFee = view.findViewById(R.id.consultationFee)
        confirmationStatus = view.findViewById(R.id.confirmationStatus)
        doctorPhoneNumber = view.findViewById(R.id.doctorPhoneNumber)
        stepOneIcon = view.findViewById(R.id.stepOneIcon)
        stepTwoIcon = view.findViewById(R.id.stepTwoIcon)
        stepThreeIcon = view.findViewById(R.id.stepThreeIcon)
        pdfPreviewImage = view.findViewById(R.id.pdfPreviewImage)
        
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initializeViews(view)
        setupRecyclerView()
    }

    private fun initializeViews(view: View) {
        // Null-safe view initialization with fallback values
        appointmentConfirmationCard = view.findViewById(R.id.appointmentConfirmationCard)
            ?: throw IllegalStateException("appointmentConfirmationCard not found")

        doctorNameConfirmed = view.findViewById(R.id.doctorNameConfirmed)
            ?: throw IllegalStateException("doctorNameConfirmed not found")

        appointmentDateTime = view.findViewById(R.id.appointmentDateTime)
            ?: throw IllegalStateException("appointmentDateTime not found")

        consultationFee = view.findViewById(R.id.consultationFee)
            ?: throw IllegalStateException("consultationFee not found")

        confirmationStatus = view.findViewById(R.id.confirmationStatus)
            ?: throw IllegalStateException("confirmationStatus not found")

        doctorPhoneNumber = view.findViewById(R.id.doctorPhoneNumber)
            ?: throw IllegalStateException("doctorPhoneNumber not found")

        // Soil Report Card Views
        soilReportCard = view.findViewById(R.id.soilReportCard)
            ?: throw IllegalStateException("soilReportCard not found")

        selectReportButton = view.findViewById(R.id.selectReportButton)
            ?: throw IllegalStateException("selectReportButton not found")

        selectedFileText = view.findViewById(R.id.selectedFileText)
            ?: throw IllegalStateException("selectedFileText not found")

        // Progress Step Icons
        stepOneIcon = view.findViewById(R.id.stepOneIcon)
            ?: throw IllegalStateException("stepOneIcon not found")

        stepTwoIcon = view.findViewById(R.id.stepTwoIcon)
            ?: throw IllegalStateException("stepTwoIcon not found")

        stepThreeIcon = view.findViewById(R.id.stepThreeIcon)
            ?: throw IllegalStateException("stepThreeIcon not found")

        // Doctors RecyclerView
        doctorsRecyclerView = view.findViewById(R.id.doctorsRecyclerView)
            ?: throw IllegalStateException("doctorsRecyclerView not found")

        // PDF Preview Image
        pdfPreviewImage = view.findViewById(R.id.pdfPreviewImage)

        // Set click listener for report selection
        selectReportButton.setOnClickListener {
            openPdfPicker()
        }
    }

    // Method to open PDF picker
    private fun openPdfPicker() {
        pdfPickerLauncher.launch("application/pdf")
    }

    private fun setupRecyclerView() {
        doctorsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = DoctorAdapter(doctors, object : OnDoctorClickListener {
                override fun onDoctorClick(doctor: SoilDoctor) {
                    selectedDoctor = doctor
                    appointmentConfirmed = false
                    appointmentConfirmationCard.visibility = View.GONE
                    soilReportCard.visibility = View.GONE

                    // Reset step indicators
                    stepTwoIcon.setImageResource(R.drawable.ic_search)
                    stepTwoIcon.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.gray))
                    stepThreeIcon.setImageResource(R.drawable.ic_search)
                    stepThreeIcon.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.gray))

                    // Show date picker for appointment
                    showDatePicker()
                }
            })
        }
    }

    // In the OrganicFarming class, update the showDatePicker method to allow user to select date
    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth)
                // Check if selected date is valid according to doctor's availability
                selectedDoctor?.let { doctor ->
                    if (isDateAvailable(calendar, doctor.availability)) {
                        // Show time picker after date is selected
                        showTimePicker(calendar)
                    } else {
                        Toast.makeText(
                            requireContext(),
                            "Doctor not available on selected date. Please choose another date.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
    
        // Set min date to today
        datePickerDialog.datePicker.minDate = System.currentTimeMillis()
        
        // Set max date to 30 days from now
        val maxDate = Calendar.getInstance()
        maxDate.add(Calendar.DAY_OF_MONTH, 30)
        datePickerDialog.datePicker.maxDate = maxDate.timeInMillis
    
        datePickerDialog.show()
    }
    
    // Update the sendAppointmentRequest method to send SMS to doctor
    private fun sendAppointmentRequest(doctor: SoilDoctor) {
        val smsManager = SmsManager.getDefault()
        
        // Format phone number - remove country code if it's Dr. Sarah Green
        val phoneNumber = if (doctor.name == "Dr. Sarah Green") {
            "8446204947"  // Use direct number without country code
        } else {
            doctor.phoneNumber  // Use the stored phone number for other doctors
        }
        
        val message = """
            New Appointment Request
            
            From: ${requireContext().getString(R.string.user)}
            Date: ${selectedAppointment?.date}
            Time: ${selectedAppointment?.time}
            
            Please reply with exactly:
            'YES' to confirm appointment
            'NO' to decline appointment
        """.trimIndent()
    
        try {
            smsManager.sendTextMessage(
                phoneNumber,
                null,
                message,
                null,
                null
            )
    
            // Show waiting message and update UI
            appointmentConfirmationCard.visibility = View.VISIBLE
            confirmationStatus.text = "⏳ Appointment Request Sent\nWaiting for doctor's confirmation..."
            confirmationStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.grey))
    
            // Call the updateDoctorDetails method (now defined outside)
            updateDoctorDetails(doctor)
    
            // Update step indicators
            stepTwoIcon.setImageResource(R.drawable.ic_clock)
            stepTwoIcon.imageTintList = ColorStateList.valueOf(
                ContextCompat.getColor(requireContext(), R.color.primary)
            )
    
            Toast.makeText(
                context,
                "Request sent to Dr. ${doctor.name}. Waiting for confirmation...",
                Toast.LENGTH_LONG
            ).show()
        } catch (e: Exception) {
            Toast.makeText(
                context,
                "Failed to send appointment request: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
            Log.e("OrganicFarming", "SMS sending failed", e)
        }
    }
    
    // Move the updateDoctorDetails method outside of sendAppointmentRequest
    private fun updateDoctorDetails(doctor: SoilDoctor) {
        doctorNameConfirmed.text = doctor.name
        appointmentDateTime.text = "Date and Time: ${selectedAppointment?.date} at ${selectedAppointment?.time}"
        consultationFee.text = "Consultation Fee: ₹${doctor.fee}"
        doctorPhoneNumber.text = "Phone: ${doctor.phoneNumber}"
        
        // Update step indicators
        stepOneIcon.setImageResource(R.drawable.ic_check)
        stepOneIcon.imageTintList = ColorStateList.valueOf(
            ContextCompat.getColor(requireContext(), R.color.confirmation_green)
        )
    }
    
    // Add or update the handleSmsResponse method to process doctor's response
    fun handleSmsResponse(message: String) {
        // Log the received SMS for debugging
        Log.d("OrganicFarming", "Received SMS: $message")
    
        // Normalize the response for more flexible parsing
        val response = message.trim().uppercase()
    
        // Ensure we have a selected doctor and appointment
        if (selectedDoctor == null || selectedAppointment == null) {
            Log.w("OrganicFarming", "No active appointment to process SMS")
            return
        }
    
        // Process the response
        activity?.runOnUiThread {
            when {
                response.contains("YES") -> {
                    // Appointment confirmed
                    appointmentConfirmed = true
                    
                    // Update UI
                    confirmationStatus.text = "✓ Appointment Confirmed!"
                    confirmationStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.confirmation_green))
                    
                    // Show soil report upload option
                    soilReportCard.visibility = View.VISIBLE
                    
                    // Update step indicators
                    stepTwoIcon.setImageResource(R.drawable.ic_check)
                    stepTwoIcon.imageTintList = ColorStateList.valueOf(
                        ContextCompat.getColor(requireContext(), R.color.confirmation_green)
                    )
                    
                    // Show success message
                    Toast.makeText(
                        context,
                        "Appointment confirmed with Dr. ${selectedDoctor?.name}",
                        Toast.LENGTH_LONG
                    ).show()
                }
                response.contains("NO") -> {
                    // Appointment declined
                    appointmentConfirmed = false
                    
                    // Update UI
                    confirmationStatus.text = "✗ Appointment Declined"
                    confirmationStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.red))
                    
                    // Hide soil report upload
                    soilReportCard.visibility = View.GONE
                    
                    // Reset step indicators
                    stepTwoIcon.setImageResource(R.drawable.ic_close)
                    stepTwoIcon.imageTintList = ColorStateList.valueOf(
                        ContextCompat.getColor(requireContext(), R.color.red)
                    )
                    
                    // Show decline message
                    Toast.makeText(
                        context,
                        "Appointment declined by Dr. ${selectedDoctor?.name}. Please try another time or doctor.",
                        Toast.LENGTH_LONG
                    ).show()
                }
                else -> {
                    // Unclear response
                    Log.w("OrganicFarming", "Unclear response: $message")
                    Toast.makeText(
                        context,
                        "Received unclear response from doctor. Please contact directly.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun isDateAvailable(calendar: Calendar, availability: String?): Boolean {
        if (availability == null) return false

        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        return when (dayOfWeek) {
            Calendar.MONDAY -> availability.contains("Mon")
            Calendar.TUESDAY -> availability.contains("Tue")
            Calendar.WEDNESDAY -> availability.contains("Wed")
            Calendar.THURSDAY -> availability.contains("Thu")
            Calendar.FRIDAY -> availability.contains("Fri")
            Calendar.SATURDAY -> availability.contains("Sat")
            else -> false
        }
    }

    private fun isTimeAvailable(calendar: Calendar, availability: String?): Boolean {
        if (availability == null) return false

        // Extract time range from availability string (e.g., "9AM-5PM")
        val timeRange = availability.substringAfter(", ").split("-")
        if (timeRange.size != 2) return false

        val startTime = parseTime(timeRange[0])
        val endTime = parseTime(timeRange[1])
        val selectedTime = calendar.get(Calendar.HOUR_OF_DAY)

        return selectedTime in startTime until endTime
    }

    private fun parseTime(timeStr: String): Int {
        val hour = timeStr.substring(0, timeStr.length - 2).toInt()
        val isPM = timeStr.endsWith("PM")
        return when {
            isPM && hour != 12 -> hour + 12
            !isPM && hour == 12 -> 0
            else -> hour
        }
    }

    private fun bookAppointment(doctorId: Int, date: String, time: String) {
        selectedAppointment = Appointment(doctorId, date, time)
        selectedDoctor = doctors.find { it.id == doctorId }

        selectedDoctor?.let { doctor ->
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.SEND_SMS
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                sendAppointmentRequest(doctor)
            } else {
                requestPermissionLauncher.launch(Manifest.permission.SEND_SMS)
            }
        }
    }

    private fun checkSmsPermission() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.RECEIVE_SMS
            ) == PackageManager.PERMISSION_GRANTED -> {
                registerSmsReceiver()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.RECEIVE_SMS) -> {
                AlertDialog.Builder(requireContext())
                    .setTitle("SMS Permission Required")
                    .setMessage("This app needs SMS permission to receive doctor confirmations. Would you like to grant permission?")
                    .setPositiveButton("Yes") { _, _ ->
                        requestPermissionLauncher.launch(Manifest.permission.RECEIVE_SMS)
                    }
                    .setNegativeButton("No") { dialog, _ -> dialog.dismiss() }
                    .show()
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.RECEIVE_SMS)
            }
        }
    }

    private fun registerSmsReceiver() {
        try {
            val intentFilter = IntentFilter("android.provider.Telephony.SMS_RECEIVED")
            requireActivity().registerReceiver(smsReceiver, intentFilter)
        } catch (e: Exception) {
            Log.e("OrganicFarming", "Failed to register SMS receiver", e)
        }
    }

    override fun onResume() {
        super.onResume()
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.RECEIVE_SMS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            registerSmsReceiver()
        }

        // If PDF is uploaded and payment is pending, show payment dialog
        if (isPdfUploadedAndPaymentPending) {
            selectedDoctorForPayment?.let { doctor ->
                showPaymentPrompt(doctor)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        try {
            requireActivity().unregisterReceiver(smsReceiver)
        } catch (e: IllegalArgumentException) {
            Log.w("OrganicFarming", "SMS receiver not registered")
        }
    }

    private fun showTimePicker(calendar: Calendar) {
        val timePickerDialog = TimePickerDialog(
            requireContext(),
            { _, hourOfDay, minute ->
                calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                calendar.set(Calendar.MINUTE, minute)
                
                // Check if selected time is valid according to doctor's availability
                selectedDoctor?.let { doctor ->
                    if (isTimeAvailable(calendar, doctor.availability)) {
                        // Format date and time
                        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
                        val date = dateFormat.format(calendar.time)
                        val time = timeFormat.format(calendar.time)
                        
                        // Book appointment
                        bookAppointment(doctor.id, date, time)
                        
                        // Update step indicators
                        stepOneIcon.setImageResource(R.drawable.ic_check)
                        stepOneIcon.imageTintList = ColorStateList.valueOf(
                            ContextCompat.getColor(requireContext(), R.color.confirmation_green)
                        )
                    } else {
                        Toast.makeText(
                            requireContext(),
                            "Doctor not available at selected time. Please choose another time.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            false
        )
        timePickerDialog.show()
    }

    private fun sendSmsReport(doctor: SoilDoctor) {
        try {
            val smsManager = SmsManager.getDefault()
            
            // Use the actual PDF file name and path
            val pdfFileName = pdfUri?.lastPathSegment ?: "soil_report.pdf"
            
            // Prepare PDF file for sharing
            val pdfFile = pdfUri?.let { uri ->
                try {
                    val inputStream = requireContext().contentResolver.openInputStream(uri)
                    val tempFile = File(requireContext().cacheDir, pdfFileName)
                    tempFile.outputStream().use { fileOut ->
                        inputStream?.copyTo(fileOut)
                    }
                    tempFile
                } catch (e: Exception) {
                    Log.e("PDFSharing", "Error preparing PDF file", e)
                    null
                }
            }

            val smsMessage = """
                � Respected Dr. ${doctor.name},

                I hope this message finds you well. I am writing to share a detailed soil analysis report for your professional review.

                🔬 Report Details:
                - Patient Name: ${requireContext().getString(R.string.user)}
                - Date of Analysis: ${selectedAppointment?.date}
                - Time of Analysis: ${selectedAppointment?.time}

                � Attached Report: $pdfFileName

                Consultation Fee: ₹${doctor.fee}

                Your expertise is greatly appreciated. Kindly review the attached report at your convenience.

                Warm regards,
                [Your Name]
            """.trimIndent()

            val parts = smsManager.divideMessage(smsMessage)
            smsManager.sendMultipartTextMessage(
                doctor.phoneNumber,
                null,
                parts,
                null,
                null
            )

            // Optionally, you can send the PDF via another method like email or file sharing
            pdfFile?.let { file ->
                // Example: You might want to implement a method to share the PDF
                // sharePdfWithDoctor(file, doctor)
            }

            // Prompt for payment
            showPaymentPrompt(doctor)
        } catch (e: Exception) {
            Log.e("SMSReportError", "Failed to send SMS report", e)
            Toast.makeText(
                requireContext(), 
                "Could not send SMS. Please check your SMS permissions.", 
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun initiatePayment(doctor: SoilDoctor) {
        try {
            val amount = doctor.fee

            val upiUri = Uri.Builder()
                .scheme("upi")
                .authority("pay")
                .appendQueryParameter("pa", "ayushpund12345@okhdfcbank") // Replace with actual UPI ID
                .appendQueryParameter("pn", "Organic Farming Consultation")
                .appendQueryParameter("tn", "Consultation Fee for Dr. ${doctor.name}")
                .appendQueryParameter("am", amount.toString())
                .appendQueryParameter("cu", "INR")
                .build()

            val paymentIntent = Intent(Intent.ACTION_VIEW).apply {
                data = upiUri
            }

            startActivityForResult(
                Intent.createChooser(paymentIntent, "Pay with..."),
                123
            )
        } catch (e: Exception) {
            Toast.makeText(
                requireContext(), 
                "No UPI app found!", 
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        if (requestCode == 123) {
            when (resultCode) {
                Activity.RESULT_OK -> {
                    // Payment successful
                    Toast.makeText(
                        requireContext(), 
                        "Payment successful. Doctor will contact you soon.", 
                        Toast.LENGTH_LONG
                    ).show()
                    
                    // Reset payment state
                    resetPdfAndPaymentState()
                }
                Activity.RESULT_CANCELED -> {
                    // Payment cancelled
                    Toast.makeText(
                        requireContext(), 
                        "Payment cancelled. Please try again.", 
                        Toast.LENGTH_SHORT
                    ).show()
                }
                else -> {
                    // Payment failed
                    Toast.makeText(
                        requireContext(), 
                        "Payment failed. Please try again.", 
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun showPaymentPrompt(doctor: SoilDoctor) {
        AlertDialog.Builder(requireContext())
            .setTitle("Payment Required")
            .setMessage("Consultation Fee: ₹${doctor.fee}")
            .setPositiveButton("Pay Now") { _, _ -> 
                initiatePayment(doctor)
            }
            .setNegativeButton("Cancel") { dialog, _ -> 
                dialog.dismiss()
                resetPdfAndPaymentState()
            }
            .setCancelable(false)
            .show()
    }

    private fun resetPdfAndPaymentState() {
        isPdfUploadedAndPaymentPending = false
        selectedDoctorForPayment = null
        pdfUri = null
    }

    // Add the processPdfSharing method inside the class
    private fun processPdfSharing() {
        pdfUri?.let { uri ->
            selectedDoctor?.let { doctor ->
                try {
                    // Create WhatsApp intent
                    val intent = Intent(Intent.ACTION_SEND)
                    intent.type = "application/pdf"
                    intent.putExtra(Intent.EXTRA_STREAM, uri)
                    
                    // Add phone number for WhatsApp - handle Dr. Sarah Green specially
                    val phoneNumber = if (doctor.name == "Dr. Sarah Green") {
                        "8446204947"  // Use direct number without country code
                    } else {
                        doctor.phoneNumber.replace("+", "")
                    }
                    
                    intent.putExtra("jid", "$phoneNumber@s.whatsapp.net")
                    
                    // Set package to WhatsApp
                    intent.setPackage("com.whatsapp")
                    
                    // Add message
                    intent.putExtra(
                        Intent.EXTRA_TEXT,
                        "Soil Report for appointment on ${selectedAppointment?.date} at ${selectedAppointment?.time}"
                    )
                    
                    // Start activity
                    startActivity(intent)
                    
                    // Update step indicators
                    stepThreeIcon.setImageResource(R.drawable.ic_check)
                    stepThreeIcon.imageTintList = ColorStateList.valueOf(
                        ContextCompat.getColor(requireContext(), R.color.confirmation_green)
                    )
                    
                    // Show success message
                    Toast.makeText(
                        requireContext(),
                        "Soil report sent to Dr. ${doctor.name}",
                        Toast.LENGTH_LONG
                    ).show()
                    
                } catch (e: Exception) {
                    // Show error message
                    Toast.makeText(
                        requireContext(),
                        "Failed to share PDF: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                    Log.e("OrganicFarming", "PDF sharing failed", e)
                }
            }
        }
    }

    companion object {
        private const val PAYMENT_REQUEST_CODE = 123
    }
}
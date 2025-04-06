package com.example.pict

import android.os.Parcel
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.ProgressBar
import android.view.Gravity
import android.widget.FrameLayout
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

class ChatAdapter : RecyclerView.Adapter<ChatAdapter.MessageViewHolder>() {
    private val messages = mutableListOf<ChatMessage>()

    fun addMessage(message: ChatMessage) {
        messages.add(message)
        notifyItemInserted(messages.size - 1)
    }

    fun removeLoadingMessages() {
        val loadingMessages = messages.filter { it.isLoading }
        loadingMessages.forEach { message ->
            val index = messages.indexOf(message)
            if (index != -1) {
                messages.removeAt(index)
                notifyItemRemoved(index)
            }
        }
    }

    fun removeLastMessage() {
        if (messages.isNotEmpty()) {
            messages.removeAt(messages.size - 1)
            notifyItemRemoved(messages.size - 1) // Fix: changed from messages.size to messages.size - 1
        }
    }

    fun clear() {
        val size = messages.size
        messages.clear()
        notifyItemRangeRemoved(0, size)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.chat_message_item, parent, false)
        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val message = messages[position]
        holder.bind(message)
    }

    override fun getItemCount() = messages.size

    inner class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val messageCard: CardView = itemView.findViewById(R.id.messageCard)
        private val messageText: TextView = itemView.findViewById(R.id.messageText)
        private val loadingIndicator: ProgressBar = itemView.findViewById(R.id.loadingIndicator)
        private val container: FrameLayout = itemView as FrameLayout

        fun bind(message: ChatMessage) {
            messageText.text = message.text
            loadingIndicator.visibility = if (message.isLoading) View.VISIBLE else View.GONE

            // Set message alignment and background color
            val params = messageCard.layoutParams as FrameLayout.LayoutParams
            if (!message.isBot) {
                // User message
                params.gravity = Gravity.END
                messageCard.setCardBackgroundColor(ContextCompat.getColor(itemView.context, R.color.chatbot_user_message))
            } else {
                // Bot message
                params.gravity = Gravity.START
                messageCard.setCardBackgroundColor(ContextCompat.getColor(itemView.context, R.color.chatbot_bot_message))
            }
            messageCard.layoutParams = params
        }
    }
}
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
import com.example.pict.ChatMessage
import com.example.pict.R

class ChatbotAdapter : RecyclerView.Adapter<ChatbotAdapter.MessageViewHolder>() {
    internal val messages = mutableListOf<ChatMessage>()

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

    class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val messageText: TextView = itemView.findViewById(R.id.messageText)
        private val messageCard: CardView = itemView.findViewById(R.id.messageCard)
        private val loadingIndicator: ProgressBar = itemView.findViewById(R.id.loadingIndicator)

        fun bind(message: ChatMessage) {
            messageText.text = message.text
            loadingIndicator.visibility = if (message.isLoading) View.VISIBLE else View.GONE
            messageText.visibility = if (message.isLoading) View.GONE else View.VISIBLE

            val params = messageCard.layoutParams as FrameLayout.LayoutParams
            if (message.isBot) {
                params.gravity = Gravity.START
                messageCard.setCardBackgroundColor(
                    itemView.context.getColor(R.color.message_bot)
                )
            } else {
                params.gravity = Gravity.END
                messageCard.setCardBackgroundColor(
                    itemView.context.getColor(R.color.message_user)
                )
            }
            messageCard.layoutParams = params
        }
    }
} 
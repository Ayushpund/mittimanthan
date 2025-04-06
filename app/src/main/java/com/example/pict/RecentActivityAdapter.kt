package com.example.pict.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.pict.R
import com.example.pict.models.ActivityItem
import com.example.pict.models.ActivityType
import java.text.SimpleDateFormat
import java.util.*

class RecentActivityAdapter(private val activities: List<ActivityItem>) :
    RecyclerView.Adapter<RecentActivityAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val icon: ImageView = view.findViewById(R.id.activityIcon)
        val title: TextView = view.findViewById(R.id.activityTitle)
        val description: TextView = view.findViewById(R.id.activityDescription)
        val timestamp: TextView = view.findViewById(R.id.activityTimestamp)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recent_activity, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val activity = activities[position]
        holder.title.text = activity.title
        holder.description.text = activity.description
        holder.timestamp.text = formatTimestamp(activity.timestamp)

        val iconRes = when (activity.type) {
            ActivityType.SOIL_ANALYSIS -> android.R.drawable.ic_menu_mylocation
            ActivityType.CROP_PREDICTION -> android.R.drawable.ic_menu_crop
            ActivityType.EXPERT_HELP -> android.R.drawable.ic_menu_help
        }
        holder.icon.setImageResource(iconRes)
    }

    override fun getItemCount() = activities.size

    private fun formatTimestamp(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp

        return when {
            diff < 60000 -> "Just now" // Less than 1 minute
            diff < 3600000 -> "${diff / 60000}m ago" // Less than 1 hour
            diff < 86400000 -> "${diff / 3600000}h ago" // Less than 24 hours
            else -> {
                val sdf = SimpleDateFormat("MMM dd", Locale.getDefault())
                sdf.format(Date(timestamp))
            }
        }
    }
}

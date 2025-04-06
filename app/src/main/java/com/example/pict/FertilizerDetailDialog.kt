package com.example.pict

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.example.pict.models.Fertilizer

class FertilizerDetailDialog(private val fertilizer: Fertilizer) : BottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_fertilizer_detail, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(view) {

            findViewById<TextView>(R.id.fertilizerName).text = fertilizer.name
            findViewById<TextView>(R.id.fertilizerDescription).text = fertilizer.description
            findViewById<TextView>(R.id.fertilizerPrice).text = fertilizer.price
            findViewById<TextView>(R.id.fertilizerType).text = "Type: ${fertilizer.category}"


        }
    }
} 
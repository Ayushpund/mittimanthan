package com.example.pict

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.pict.databinding.FragmentGovernmentSchemesBinding

class GovernmentSchemes : Fragment() {
    private var _binding: FragmentGovernmentSchemesBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGovernmentSchemesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupSchemeButtons()
    }

    private fun setupSchemeButtons() {
        binding.apply {
            btnSoilHealthCard.setOnClickListener {
                openWebPage("https://soilhealth.dac.gov.in/")
            }

            btnNMSA.setOnClickListener {
                openWebPage("https://nmsa.dac.gov.in/")
            }

            btnPKVY.setOnClickListener {
                openWebPage("https://pgsindia-ncof.gov.in/pkvy/index.aspx")
            }
        }
    }

    private fun openWebPage(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(url)
            }
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Unable to open website", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
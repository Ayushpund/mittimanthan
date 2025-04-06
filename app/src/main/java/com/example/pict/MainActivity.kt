package com.example.pict

import CropPrediction

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var bottomNav: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bottomNav = findViewById(R.id.nav_view)
        bottomNav.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_home -> {
                    loadFragment(Dashboard())
                    true
                }
                R.id.nav_schemes -> {
                    loadFragment(GovernmentSchemes())
                    true
                }
                R.id.nav_prediction -> {
                    loadFragment(CropPrediction())
                    true
                }
                R.id.nav_organic -> {
                    loadFragment(OrganicFarming())
                    true
                }
                R.id.nav_market -> {
                    loadFragment(Market())
                    true
                }
                else -> false
            }
        }

        // Set default fragment
        if (savedInstanceState == null) {
            bottomNav.selectedItemId = R.id.nav_home
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.container, fragment)
            .commit()
    }

    fun changeLanguage(languageCode: String) {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        
        val configuration = resources.configuration
        configuration.setLocale(locale)
        
        // Update configuration
        resources.updateConfiguration(configuration, resources.displayMetrics)
        
        // Recreate activity to apply language changes
        recreate()
    }
}

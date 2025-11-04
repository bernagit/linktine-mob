package com.linktine

import android.os.Bundle
import android.util.Log // Added for diagnostic logging
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private val TAG = "MainActivity"
    private lateinit var navController: NavController
    // Changed to nullable for safer initialization in case the ID is missing in XML
    private var bottomNav: BottomNavigationView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Crash can still happen here if R.layout.activity_main is missing or invalid.
        setContentView(R.layout.activity_main)

        // 1. Get NavHostFragment (Critical part that can fail if ID is missing)
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as? NavHostFragment

        if (navHostFragment == null) {
            // THIS WILL LOG A CRITICAL ERROR IF THE ID IS WRONG OR MISSING IN activity_main.xml
            Log.e(TAG, "FATAL ERROR: nav_host_fragment not found in activity_main.xml or is not a NavHostFragment.")
            // Stop execution to prevent further NPEs
            return
        }
        navController = navHostFragment.navController


        // 2. Get BottomNavigationView
        bottomNav = findViewById(R.id.bottom_nav_view)

        if (bottomNav == null) {
            Log.w(TAG, "WARNING: bottom_nav_view not found. Bottom navigation will be disabled.")
            // If missing, the app continues but without bottom nav.
        } else {
            // 3. Connect the bottom navigation view with the NavController
            bottomNav?.setupWithNavController(navController)

            // 4. Handle visibility of the bottom bar
            handleBottomNavVisibility()
        }
    }

    private fun handleBottomNavVisibility() {
        // We only proceed if bottomNav is not null
        bottomNav?.let { navView ->
            // Listen for destination changes
            navController.addOnDestinationChangedListener { _, destination, _ ->
                // Ensure R.id.authFragment exists in your navigation graph
                if (destination.id == R.id.authFragment) {
                    navView.visibility = View.GONE
                    Log.d(TAG, "Hiding Bottom Nav for AuthFragment.")
                } else {
                    // Otherwise, show it for the main navigation screens
                    navView.visibility = View.VISIBLE
                    Log.d(TAG, "Showing Bottom Nav for main destination: ${destination.label}")
                }
            }
        }
    }
}

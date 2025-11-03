package com.linktine

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var navController: NavController
    private lateinit var bottomNav: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Find the NavController from the NavHostFragment
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        bottomNav = findViewById(R.id.bottom_nav_view)

        // Connect the bottom navigation view with the NavController
        bottomNav.setupWithNavController(navController)

        // Handle visibility of the bottom bar
        handleBottomNavVisibility()
    }

    private fun handleBottomNavVisibility() {
        // Listen for destination changes
        navController.addOnDestinationChangedListener { _, destination, _ ->
            // If the current destination is the initial AuthFragment, hide the bottom bar
            if (destination.id == R.id.authFragment) {
                bottomNav.visibility = View.GONE
            } else {
                // Otherwise, show it for the main navigation screens
                bottomNav.visibility = View.VISIBLE
            }
        }
    }
}
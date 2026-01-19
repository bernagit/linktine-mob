package com.linktine

import android.os.Bundle
import android.util.Log // Added for diagnostic logging
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private val TAG = "MainActivity"
    private lateinit var navController: NavController
    private var bottomNav: BottomNavigationView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as? NavHostFragment
                ?: return

        navController = navHostFragment.navController

        bottomNav = findViewById(R.id.bottom_nav_view)

        bottomNav?.setupWithNavController(navController)

        handleBottomNavVisibility()
        setupBottomNavLongPress()
    }

    private fun handleBottomNavVisibility() {
        bottomNav?.let { navView ->
            navController.addOnDestinationChangedListener { _, destination, _ ->
                navView.visibility =
                    if (destination.id == R.id.authFragment) View.GONE else View.VISIBLE
            }
        }
    }

    // ---------------- LONG PRESS SUPPORT ----------------

    private fun setupBottomNavLongPress() {
        val menuView = bottomNav?.getChildAt(0) as? ViewGroup ?: return

        for (i in 0 until menuView.childCount) {
            val itemView = menuView.getChildAt(i)
            val menuItem = bottomNav?.menu?.getItem(i)

            if (menuItem?.itemId == R.id.favoritesFragment) {
                itemView.setOnLongClickListener {
                    showProfileSwitchDialog()
                    true
                }
            }
        }
    }

    // ---------------- PROFILE SWITCH ----------------

    private fun showProfileSwitchDialog() {
        val dialog = ProfileSwitchBottomSheet()
        dialog.show(supportFragmentManager, "ProfileSwitch")
    }
}

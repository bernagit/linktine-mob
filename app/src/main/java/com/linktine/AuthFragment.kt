package com.linktine

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.linktine.viewmodel.SettingsEvent
import com.linktine.viewmodel.SettingsViewModel
import com.linktine.viewmodel.SettingsViewModelFactory
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.linktine.R // Import your resources R file
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class AuthFragment : Fragment() {

    // --- MVVM Setup ---
    private val viewModel: SettingsViewModel by viewModels {
        // This is the factory producer lambda
        SettingsViewModelFactory(requireContext().applicationContext)
    }

    // --- View References (using nullable properties is okay here) ---
    private var serverUrlInput: TextInputEditText? = null
    private var tokenInput: TextInputEditText? = null
    private var btnSave: MaterialButton? = null
    private var btnScanQR: MaterialButton? = null

    // --- QR Scanner Launcher (Adapting your Activity code to Fragment) ---
    private val qrScanLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == AppCompatActivity.RESULT_OK) {
            val scanned = result.data?.getStringExtra("scanned_token")

            if (!scanned.isNullOrEmpty()) {
                val parts = scanned.split("|")
                if (parts.size == 2) {
                    // Update input fields with scanned data
                    serverUrlInput?.setText(parts[0].trim())
                    tokenInput?.setText(parts[1].trim())
                    Toast.makeText(requireContext(), "QR parsed successfully", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "Invalid QR format, expected: url|token", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // --- Fragment Lifecycle ---

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the new XML layout
        return inflater.inflate(R.layout.fragment_auth, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize view references (Fixing the "Unresolved reference" issue)
        serverUrlInput = view.findViewById(R.id.editServerUrl)
        tokenInput = view.findViewById(R.id.editToken)
        btnSave = view.findViewById(R.id.btnLogin) // Using btnLogin ID from XML
        btnScanQR = view.findViewById(R.id.btnScanQR)

        setupListeners()
        setupObservers()

        // Load saved values into the input fields (optional, the repository handles persistence check)
        loadSavedValues()
    }

    private fun loadSavedValues() {
        // You would typically load this data from the ViewModel (or Repository via ViewModel)
        // For simplicity *now*, we can skip this as the main check is the navigation bypass.
        // If you want to pre-fill the fields with saved data:
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.areSettingsPresent.collectLatest { isPresent ->
                if (!isPresent) {
                    // Implement logic to load the last entered URL/Token if you want
                    // This often requires the ViewModel to expose the saved ServerInfo
                }
            }
        }
    }


    private fun setupListeners() {
        // 1. Save Button Click
        btnSave?.setOnClickListener {
            val url = serverUrlInput?.text.toString().trim()
            val token = tokenInput?.text.toString().trim()

            // Call the ViewModel's save function
            viewModel.saveSettingsAndLogin(url, token) // We will update the ViewModel for login
        }

        // 2. QR Scan Button Click
        btnScanQR?.setOnClickListener {
            // NOTE: You must have the QrScannerActivity class in your project
            val intent = Intent(requireContext(), QrScannerActivity::class.java)
            qrScanLauncher.launch(intent)
        }
    }

    private fun setupObservers() {
        // 1. Initial check (Immediate bypass if settings are present)
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.areSettingsPresent.collectLatest { isPresent ->
                if (isPresent) {
                    // Navigate immediately if settings are found on app start
                    navigateToHome()
                }
            }
        }

        // 2. Events from ViewModel (Login status, Errors)
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.events.collectLatest { event ->
                when (event) {
                    is SettingsEvent.SettingsSavedSuccess -> {
                        Toast.makeText(requireContext(), event.message, Toast.LENGTH_SHORT).show()
                        navigateToHome() // Navigate upon successful login/save
                    }
                    is SettingsEvent.Error -> {
                        Toast.makeText(requireContext(), event.message, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    private fun navigateToHome() {
        // R.id.homeFragment is the destination ID in your nav_graph.xml
        findNavController().navigate(R.id.homeFragment)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Clear references to prevent memory leaks
        serverUrlInput = null
        tokenInput = null
        btnSave = null
        btnScanQR = null
    }
}
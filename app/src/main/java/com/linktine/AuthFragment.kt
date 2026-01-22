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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.linktine.viewmodel.SettingsEvent
import com.linktine.viewmodel.SettingsViewModel
import com.linktine.viewmodel.SettingsViewModelFactory
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
class AuthFragment : Fragment() {

    private val viewModel: SettingsViewModel by viewModels {
        SettingsViewModelFactory(requireContext().applicationContext)
    }

    private var serverUrlInput: TextInputEditText? = null
    private var tokenInput: TextInputEditText? = null
    private var btnSave: MaterialButton? = null
    private var btnScanQR: MaterialButton? = null

    private val qrScanLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == AppCompatActivity.RESULT_OK) {
            val scanned = result.data?.getStringExtra("scanned_token")
            if (!scanned.isNullOrEmpty()) {
                val parts = scanned.split("|")
                if (parts.size == 2) {
                    val url = parts[0].trim()
                    val token = parts[1].trim()
                    Toast.makeText(requireContext(), "QR parsed successfully", Toast.LENGTH_SHORT).show()
                    viewModel.saveSettingsAndLogin(url, token)
                } else {
                    Toast.makeText(requireContext(), "Invalid QR format, expected: url|token", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_auth, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        serverUrlInput = view.findViewById(R.id.editServerUrl)
        tokenInput = view.findViewById(R.id.editToken)
        btnSave = view.findViewById(R.id.btnLogin)
        btnScanQR = view.findViewById(R.id.btnScanQR)

        setupListeners()
        setupObservers()
    }

    private fun setupListeners() {
        btnSave?.setOnClickListener {
            val url = serverUrlInput?.text.toString().trim()
            val token = tokenInput?.text.toString().trim()
            viewModel.saveSettingsAndLogin(url, token)
        }

        btnScanQR?.setOnClickListener {
            qrScanLauncher.launch(
                Intent(requireContext(), QrScannerActivity::class.java)
            )
        }
    }

    private fun setupObservers() {
        val forceAddAccount = arguments?.getBoolean("forceAddAccount", false) ?: false

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {

                // AUTO LOGIN
                if (!forceAddAccount) {
                    launch {
                        viewModel.areSettingsPresent
                            .distinctUntilChanged()
                            .collectLatest { present ->
                                if (present && viewModel.loginJob?.isActive != true) {
                                    viewModel.validateCurrentSettingsAndLogin()
                                }
                            }
                    }
                }

                // EVENTS
                launch {
                    viewModel.events.collectLatest { event ->
                        when (event) {
                            is SettingsEvent.SettingsSavedSuccess -> {
                                Toast.makeText(requireContext(), event.message, Toast.LENGTH_SHORT).show()
                                navigateToHome()
                            }
                            is SettingsEvent.Error -> {
                                Toast.makeText(requireContext(), event.message, Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun navigateToHome() {
        findNavController().navigate(
            R.id.homeFragment,
            null,
            NavOptions.Builder()
                .setPopUpTo(findNavController().graph.startDestinationId, true)
                .build()
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        serverUrlInput = null
        tokenInput = null
        btnSave = null
        btnScanQR = null
    }
}

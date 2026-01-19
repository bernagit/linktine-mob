package com.linktine

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.linktine.data.SettingsRepository
import com.linktine.ui.profile.ProfileAdapter
import com.linktine.viewmodel.SettingsViewModel
import com.linktine.viewmodel.SettingsViewModelFactory
import kotlinx.coroutines.launch

class ProfileSwitchBottomSheet : BottomSheetDialogFragment() {

    private lateinit var recycler: RecyclerView
    private lateinit var repository: SettingsRepository
    private lateinit var adapter: ProfileAdapter

    private val settingsViewModel: SettingsViewModel by viewModels {
        SettingsViewModelFactory(requireContext().applicationContext)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.sheet_profile_switch, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        repository = SettingsRepository(requireContext().applicationContext)

        recycler = view.findViewById(R.id.profilesRecycler)
        recycler.layoutManager = LinearLayoutManager(requireContext())

        val profiles = repository.getAllProfiles().toMutableList()

        viewLifecycleOwner.lifecycleScope.launch {
            val activeProfileId = repository.getActiveProfileId()

            adapter = ProfileAdapter(
                profiles,
                onClick = { profile ->
                    viewLifecycleOwner.lifecycleScope.launch {
                        settingsViewModel.switchProfile(profile.id)
                        repository.getActiveProfileId()
                        dismiss()

                        // Navigate to HomeFragment
                        if (findNavController().currentDestination?.id != R.id.homeFragment) {
                            findNavController().navigate(R.id.homeFragment)
                        }
                    }
                },
                onDelete = { profile ->
                    viewLifecycleOwner.lifecycleScope.launch {
                        val activeProfile = repository.getActiveProfile()
                        repository.deleteProfile(profile.id)
                        adapter.remove(profile)

                        if (activeProfile.id == profile.id) {
                            val remainingProfiles = repository.getAllProfiles()
                            if (remainingProfiles.isNotEmpty()) {
                                settingsViewModel.switchProfile(remainingProfiles.first().id)
                                adapter.setActiveProfile(remainingProfiles.first().id)
                            } else {
                                dismiss()
                                findNavController().navigate(
                                    R.id.authFragment,
                                    bundleOf("forceAddAccount" to true)
                                )
                            }
                        }
                    }
                },
                activeProfileId = activeProfileId // pass active profile here
            )

            recycler.adapter = adapter
        }

        // Add account button
        view.findViewById<TextView>(R.id.addAccountBtn).setOnClickListener {
            dismiss()
            findNavController().navigate(R.id.authFragment, bundleOf("forceAddAccount" to true))
        }
    }
}

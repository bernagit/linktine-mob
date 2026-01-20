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
import kotlinx.coroutines.flow.first
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

        viewLifecycleOwner.lifecycleScope.launch {

            val profiles = repository.getAllProfiles()
            val activeProfileId = repository.activeProfileFlow.first()

            adapter = ProfileAdapter(
                profiles.toMutableList(),

                onClick = { profile ->
                    viewLifecycleOwner.lifecycleScope.launch {
                        settingsViewModel.switchProfile(profile.id)
                        dismiss()
                    }
                },

                onDelete = { profile ->
                    viewLifecycleOwner.lifecycleScope.launch {
                        val activeProfileIdNow = repository.activeProfileFlow.first()

                        repository.deleteProfile(profile.id)
                        adapter.remove(profile)

                        if (activeProfileIdNow == profile.id) {
                            val remaining = repository.getAllProfiles()
                            if (remaining.isNotEmpty()) {
                                settingsViewModel.switchProfile(remaining.first().id)
                                adapter.setActiveProfile(remaining.first().id)
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

                activeProfileId = activeProfileId
            )

            recycler.adapter = adapter
        }

        view.findViewById<TextView>(R.id.addAccountBtn).setOnClickListener {
            dismiss()
            findNavController().navigate(R.id.authFragment, bundleOf("forceAddAccount" to true))
        }
    }
}

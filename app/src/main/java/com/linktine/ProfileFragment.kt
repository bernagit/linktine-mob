package com.linktine

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Switch
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.linktine.viewmodel.ProfileViewModel

class ProfileFragment : Fragment() {

    private val viewModel: ProfileViewModel by viewModels {
        ProfileViewModel.Factory(requireContext().applicationContext)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    @SuppressLint("UseSwitchCompatOrMaterialCode")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        val name = view.findViewById<TextView>(R.id.text_profile_name)
        val email = view.findViewById<TextView>(R.id.text_profile_email)
        val role = view.findViewById<TextView>(R.id.text_profile_role)
        val darkSwitch = view.findViewById<Switch>(R.id.switch_dark_mode)
        val logoutBtn = view.findViewById<Button>(R.id.btn_logout)

        viewModel.activeProfile.observe(viewLifecycleOwner) { profile ->
            if (profile != null) {
                name.text = profile.name
                email.text = profile.email
                role.text = profile.role
            } else {
                name.text = "No active profile"
            }
        }

        darkSwitch.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setDarkMode(isChecked)
        }

        logoutBtn.setOnClickListener {
            viewModel.logout()
        }
    }
}

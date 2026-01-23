package com.linktine

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputEditText
import com.linktine.viewmodel.ProfileViewModel

class ProfileFragment : Fragment() {

    private val viewModel: ProfileViewModel by viewModels {
        ProfileViewModel.Factory(requireContext().applicationContext)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        val name = view.findViewById<TextView>(R.id.text_profile_name)
        val email = view.findViewById<TextView>(R.id.text_profile_email)
        val role = view.findViewById<TextView>(R.id.text_profile_role)
        val darkSwitch = view.findViewById<SwitchMaterial>(R.id.switch_dark_mode)
        val logoutBtn = view.findViewById<Button>(R.id.btn_logout)
        val profileHeader = view.findViewById<LinearLayout>(R.id.profile_header)

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

        viewModel.currentTheme.observe(viewLifecycleOwner) { theme ->
            // Disable temporary the onChange, otherwise it would be called
            // when forcing the isChecked property
            darkSwitch.setOnCheckedChangeListener(null)
            darkSwitch.isChecked = (theme == "dark")
            darkSwitch.setOnCheckedChangeListener { _, isChecked ->
                viewModel.setDarkMode(isChecked)
            }
        }

        profileHeader.setOnClickListener {
            val view = layoutInflater.inflate(R.layout.dialog_edit_username, null)
            val editText = view.findViewById<TextInputEditText>(R.id.inputNewUsername)
            editText.setText(viewModel.activeProfile.value?.name)

            MaterialAlertDialogBuilder(requireContext())
                .setView(view)
                .setPositiveButton("Save") { _, _ ->
                    val newName = editText.text.toString().trim()
                    if(newName.length >= 3) {
                        viewModel.updateUsername(newName)
                    } else {
                        Snackbar
                            .make(
                                requireView(),
                                "Username must be at least 3 characters",
                                Snackbar.LENGTH_SHORT
                            )
                            .show()
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        viewModel.message.observe(viewLifecycleOwner) { msg ->
            msg?.let {
                Snackbar
                    .make(
                        requireView(),
                        it,
                        Snackbar.LENGTH_SHORT
                    )
                    .show()
            }
        }
    }
}

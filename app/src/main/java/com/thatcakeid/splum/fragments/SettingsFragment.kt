package com.thatcakeid.splum.fragments

import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.thatcakeid.splum.R

class SettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)
        val pref = findPreference<Preference>("licensePref")

        pref!!.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Licenses")
                .setMessage(resources.getString(R.string.settings_licenses_full))
                .setPositiveButton("Ok") { _, _ -> }
                .show()

            return@OnPreferenceClickListener true
        }
    }
}
package com.thatcakeid.splum.fragments

import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.thatcakeid.services.*
import com.thatcakeid.splum.R

class SettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)

        val licensePref = findPreference<Preference>("licensePref")
        val accountPref = findPreference<Preference>("accountPref")
        val syncPref    = findPreference<Preference>("syncPref")
        val copyPref    = findPreference<Preference>("copyPref")

        var copyClicks  = 0

        licensePref!!.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            MaterialAlertDialogBuilder(requireContext(), R.style.Theme_Splum_DialogTheme)
                .setTitle("Licenses")
                .setMessage(resources.getString(R.string.settings_licenses_full))
                .setPositiveButton("Ok") { _, _ -> }
                .show()

            return@OnPreferenceClickListener true
        }

        accountPref!!.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            AccountServices().getAccount(requireContext())

            return@OnPreferenceClickListener true
        }

        syncPref!!.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            StorageServices().requireCloudAccess("Splum", "com.thatcakeid.splum", requireContext())

            return@OnPreferenceClickListener true
        }

        copyPref!!.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            copyClicks++

            if(copyClicks == 5) {
                MaterialAlertDialogBuilder(requireContext(), R.style.Theme_Splum_DialogTheme)
                    .setTitle("Definitely not an easter egg")
                    .setMessage("I know you have been looking in the code to find this easter egg, maybe not but I'm 99% sure you did.\n\n\n\n\n\n\nBtw here is a secret url:\n splum:secreturl")
                    .setPositiveButton("Ok") { _, _ -> }
                    .show()

                copyClicks = 0
            }

            return@OnPreferenceClickListener true
        }
    }
}
package com.thatcakeid.splum.fragments

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import com.thatcakeid.splum.R

class SettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)
    }
}
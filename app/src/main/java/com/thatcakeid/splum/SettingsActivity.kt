package com.thatcakeid.splum

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.thatcakeid.splum.fragments.SettingsFragment


class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        createFragment()
    }

    private fun createFragment() {
        supportFragmentManager
            .beginTransaction()
            .add(R.id.fragmentContainerView, SettingsFragment(), "settingsFragment")
            .commit()
    }
}
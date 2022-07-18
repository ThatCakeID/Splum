package com.thatcakeid.splum

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import com.thatcakeid.splum.fragments.BrowserFragment


class MainActivity : AppCompatActivity() {
    var currentFragment = "browser"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if(intent?.action == Intent.ACTION_VIEW) {
            if (savedInstanceState == null) {
                supportFragmentManager
                    .beginTransaction()
                    .add(R.id.fragmentContainerView, BrowserFragment.newInstance(intent?.data.toString()), "browserFragment")
                    .commit()
            }

        } else {
            val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
            val url: String = if(sharedPreferences.getBoolean("home_page_enabled", false))
                "about:homepage"
            else
                "https://google.com/"

            if (savedInstanceState == null) {
                supportFragmentManager
                    .beginTransaction()
                    .add(
                        R.id.fragmentContainerView,
                        BrowserFragment.newInstance(url),
                        "browserFragment")
                    .commit()
            }
        }
    }

    override fun onBackPressed() {
        if(currentFragment == "browser")
            finishAffinity()
        else {
            currentFragment = "browser"

            val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
            val url: String = if (sharedPreferences.getBoolean("home_page_enabled", false))
                "about:homepage"
            else
                "https://google.com/"

            supportFragmentManager
                .beginTransaction()
                .add(
                    R.id.fragmentContainerView,
                    BrowserFragment.newInstance(url),
                    "browserFragment"
                )
                .commit()
        }
    }

    fun setCurrentFragmentVar(fragmentName: String) {
        this.currentFragment = fragmentName
    }
}

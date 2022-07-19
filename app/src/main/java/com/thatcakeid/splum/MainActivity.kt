package com.thatcakeid.splum

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import com.thatcakeid.splum.fragments.BrowserFragment
import org.mozilla.geckoview.GeckoRuntime


class MainActivity : AppCompatActivity() {
    private var sRuntime: GeckoRuntime? = null
    private var currentFragment = "browser"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (sRuntime == null)
            sRuntime = GeckoRuntime.create(applicationContext)

        if (intent?.action == Intent.ACTION_VIEW) {
            if (savedInstanceState == null) {
                supportFragmentManager
                    .beginTransaction()
                    .addToBackStack("browserFragment")
                    .add(R.id.fragmentContainerView, BrowserFragment.newInstance(intent?.data.toString(), sRuntime), "browserFragment")
                    .commit()
            }

        } else {
            val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
            val url: String = if (sharedPreferences.getBoolean("home_page_enabled", false))
                "about:homepage"
            else
                "https://google.com/"

            if (savedInstanceState == null) {
                supportFragmentManager
                    .beginTransaction()
                    .addToBackStack("browserFragment")
                    .add(
                        R.id.fragmentContainerView,
                        BrowserFragment.newInstance(url, sRuntime),
                        "browserFragment")
                    .commit()
            }
        }
    }

    override fun onBackPressed() {
        if(currentFragment != "browser") {
            val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
            val url: String = if (sharedPreferences.getBoolean("home_page_enabled", false))
                "about:homepage"
            else
                "https://google.com/"

            supportFragmentManager
                .beginTransaction()
                .addToBackStack("browserFragment")
                .replace(
                    R.id.fragmentContainerView,
                    BrowserFragment.newInstance(url, sRuntime),
                    "browserFragment")
                .commit()
        } else
            super.onBackPressed()
    }

    fun setCurrentFragmentVar(fragmentName: String) {
        this.currentFragment = fragmentName
    }
}

package com.thatcakeid.splum

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.preference.PreferenceManager
import com.thatcakeid.splum.fragments.BrowserFragment
import com.thatcakeid.splum.tools.MainRequestInterceptor
import mozilla.components.browser.engine.gecko.GeckoEngine
import mozilla.components.browser.state.engine.EngineMiddleware
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.concept.engine.DefaultSettings
import mozilla.components.concept.engine.Engine
import mozilla.components.feature.tabs.TabsUseCases
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoRuntimeSettings
import org.mozilla.geckoview.GeckoRuntimeSettings.COLOR_SCHEME_SYSTEM

class MainActivity : AppCompatActivity() {
    private var sRuntime: GeckoRuntime? = null
    private var url: String? = null

    public var browserStore: BrowserStore? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val rTimeSettings = GeckoRuntimeSettings.Builder()
            .aboutConfigEnabled(true)
            .preferredColorScheme(COLOR_SCHEME_SYSTEM)
            .consoleOutput(true)
            .build()

        if (sRuntime == null)
            sRuntime = GeckoRuntime.create(applicationContext, rTimeSettings)

        val settings = DefaultSettings().apply {
            userAgentString = "Mozilla/5.0 (Linux; Android ${android.os.Build.VERSION.RELEASE} ${android.os.Build.MODEL}) AppleWebKit/537.36 (KHTML, like Gecko) Splum/100.0.20220425210429 Mobile Safari/537.36"
            requestInterceptor = MainRequestInterceptor(applicationContext)
            allowContentAccess = true
            allowFileAccess = true
        }

        browserStore = BrowserStore(middleware = EngineMiddleware.create(GeckoEngine(this, settings, runtime = sRuntime!!)))

        TabsUseCases(browserStore!!).addTab(url = "https://google.com", contextId = "tab0")

        url = if (savedInstanceState == null && intent?.action == Intent.ACTION_VIEW)
            intent?.data.toString()
        else {
            val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
            if (sharedPreferences.getBoolean("home_page_enabled", false))
                "splum:homepage"
            else
                "https://google.com/"
        }

        supportFragmentManager
            .beginTransaction()
            .addToBackStack("browserFragment")
            .add(
                R.id.fragmentContainerView,
                BrowserFragment.newInstance(url!!, sRuntime!!),
                "browserFragment")
            .commit()
    }
}
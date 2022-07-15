package com.thatcakeid.splum

import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import mozilla.components.browser.menu.BrowserMenuBuilder
import mozilla.components.browser.menu.item.SimpleBrowserMenuItem
import org.mozilla.geckoview.*
import org.mozilla.geckoview.GeckoSession.ContentDelegate
import org.mozilla.geckoview.GeckoSession.ProgressDelegate
import mozilla.components.browser.toolbar.BrowserToolbar
import mozilla.components.concept.toolbar.Toolbar
import mozilla.components.ui.tabcounter.TabCounter
import java.util.*


class MainActivity : AppCompatActivity() {
    private var sRuntime: GeckoRuntime? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val geckoView = findViewById<GeckoView>(R.id.geckoview)
        val toolBar = findViewById<BrowserToolbar>(R.id.toolBar)

        val progressBar = findViewById<ProgressBar>(R.id.progressBar)

        /*
        val editTextSearch = findViewById<EditText>(R.id.editTextTextPersonName)
        val tabIcon = findViewById<TabCounter>(R.id.tabIcon)
        val moreIcon = findViewById<ImageView>(R.id.moreIcon)
        */

        val session = GeckoSession()

        session.contentDelegate = object : ContentDelegate {}
        progressBar.visibility = View.GONE

        toolBar.setBackgroundColor(ContextCompat.getColor(this, R.color.colorPrimary))

        val shareItem = SimpleBrowserMenuItem("Share…") { /* Do nothing */ }
        val homeScreenItem = SimpleBrowserMenuItem("Add to Home screen") { /* Do nothing */ }
        val openItem = SimpleBrowserMenuItem("Open in…") { /* Do nothing */ }
        val settingsItem = SimpleBrowserMenuItem("Settings") { /* Do nothing */ }

        val items = listOf(shareItem, homeScreenItem, openItem, settingsItem)
        toolBar.display.menuBuilder = BrowserMenuBuilder(items)

        toolBar.url = "https://google.com/"

        if (sRuntime == null) {
            sRuntime = GeckoRuntime.create(this)
        }

        val settings = GeckoSessionSettings.Builder()
            .chromeUri("splum")
            .useTrackingProtection(true)
            .userAgentOverride("")
            .build()

        session.open(sRuntime!!)
        geckoView.setSession(session)
        session.loadUri("https://google.com")

        toolBar.setOnUrlCommitListener { url ->
            session.loadUri(url)
            toolBar.url = url

            true
        }

        /*
        editTextSearch.setOnEditorActionListener { _, actionId, _ ->
            session.loadUri(editTextSearch.text.toString())
            true
        }

        tabIcon.setOnClickListener { _ ->

        }

        moreIcon.setOnClickListener { _ ->

        }
        */

        session.progressDelegate = object : ProgressDelegate {
            override fun onPageStart(session: GeckoSession, url: String) {
                toolBar.url = url

                progressBar.visibility = View.VISIBLE
                progressBar.progress = 0
            }
            override fun onPageStop(session: GeckoSession, success: Boolean) {
                progressBar.visibility = View.GONE
            }

            override fun onProgressChange(session: GeckoSession, progress: Int) {
                progressBar.progress = progress
            }

            override fun onSecurityChange(
                session: GeckoSession,
                securityInfo: ProgressDelegate.SecurityInformation
            ) {
                if(securityInfo.isSecure)
                    toolBar.siteSecure = Toolbar.SiteSecurity.SECURE
                else
                    toolBar.siteSecure = Toolbar.SiteSecurity.INSECURE

                super.onSecurityChange(session, securityInfo)
            }
        }
    }
}

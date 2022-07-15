package com.thatcakeid.splum

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import mozilla.components.browser.domains.autocomplete.DomainAutocompleteProvider
import mozilla.components.browser.menu.BrowserMenuBuilder
import org.mozilla.geckoview.*
import org.mozilla.geckoview.GeckoSession.ContentDelegate
import org.mozilla.geckoview.GeckoSession.ProgressDelegate
import mozilla.components.browser.toolbar.BrowserToolbar
import mozilla.components.concept.toolbar.Toolbar
import mozilla.components.ui.tabcounter.TabCounter
import java.util.*
import mozilla.components.browser.domains.autocomplete.CustomDomainsProvider
import mozilla.components.browser.domains.autocomplete.ShippedDomainsProvider
import mozilla.components.browser.menu.item.*
import mozilla.components.feature.toolbar.ToolbarAutocompleteFeature


class MainActivity : AppCompatActivity() {
    private var sRuntime: GeckoRuntime? = null

    private val shippedDomainsProvider = ShippedDomainsProvider()
    private val customDomainsProvider = CustomDomainsProvider()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val geckoView = findViewById<GeckoView>(R.id.geckoview)
        val toolBar = findViewById<BrowserToolbar>(R.id.toolBar)

        val progressBar = findViewById<ProgressBar>(R.id.progressBar)

        val settings = GeckoSessionSettings.Builder()
            .useTrackingProtection(false)
            .userAgentOverride("Mozilla/5.0 (Linux; Android " + android.os.Build.VERSION.RELEASE.toString() + "; " + android.os.Build.MODEL + ") AppleWebKit/537.36 (KHTML, like Gecko) Splum/100.0.20220425210429 Mobile Safari/537.36")
            .build()

        val session = GeckoSession(settings)

        shippedDomainsProvider.initialize(this)
        customDomainsProvider.initialize(this)

        ToolbarAutocompleteFeature(toolBar).apply {
            this.addDomainProvider(shippedDomainsProvider)
            this.addDomainProvider(customDomainsProvider)
        }

        session.contentDelegate = object : ContentDelegate {}
        progressBar.visibility = View.GONE

        toolBar.setBackgroundColor(ContextCompat.getColor(this, R.color.colorPrimary))

        val backIc = BrowserMenuItemToolbar.Button(
            R.drawable.ic_arrow_back,
            "Back",
            isEnabled = { true }
        ) {
            session.goBack()
        }

        val forwardIc = BrowserMenuItemToolbar.Button(R.drawable.ic_arrow_forward, "Forward") {
            session.goForward()
        }

        val reloadIc = BrowserMenuItemToolbar.Button(R.drawable.ic_refresh, "Reload") {
            session.reload()
        }

        val bookmarkIc = BrowserMenuItemToolbar.Button(R.drawable.ic_bookmark_border, "Bookmark") {

        }

        val menuToolbar         = BrowserMenuItemToolbar(listOf(backIc, forwardIc, reloadIc, bookmarkIc))

        val newTabItem          = BrowserMenuImageText("New Tab", R.drawable.ic_add) { /* Do nothing */ }
        val newTabIncognitoItem = BrowserMenuImageText("New Private Tab", R.drawable.ic_add) { /* Do nothing */ }

        val historyItemIc       = BrowserMenuImageText("History", R.drawable.ic_history) { /* Do nothing */ }
        val downloadsItemIc     = BrowserMenuImageText("Downloads", R.drawable.ic_arrow_downward) { /* Do nothing */ }
        val bookmarksItemIc     = BrowserMenuImageText("Bookmarks", R.drawable.ic_bookmarks) { /* Do nothing */ }

        val shareItemIc         = BrowserMenuImageText("Share", R.drawable.ic_share) {
            val sendIntent: Intent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, toolBar.url)
                type = "text/plain"
            }

            val shareIntent = Intent.createChooser(sendIntent, null)
            startActivity(shareIntent)
        }
        val desktopItemIc       = BrowserMenuImageSwitch(R.drawable.ic_desktop, "Desktop View") { /* Do nothing */ }

        val settingsItem        = BrowserMenuImageText("Settings", R.drawable.ic_settings) { /* Do nothing */ }

        val items = listOf(menuToolbar, BrowserMenuDivider(), newTabItem, newTabIncognitoItem, BrowserMenuDivider(), historyItemIc, downloadsItemIc, bookmarksItemIc, BrowserMenuDivider(), shareItemIc, desktopItemIc, BrowserMenuDivider(), settingsItem)
        toolBar.display.menuBuilder = BrowserMenuBuilder(items)

        toolBar.edit.setIcon(ContextCompat.getDrawable(this, R.drawable.ic_close)!!, "Close")

        toolBar.url = "https://google.com/"

        if (sRuntime == null) {
            sRuntime = GeckoRuntime.create(this)
        }

        session.open(sRuntime!!)
        geckoView.setSession(session)
        session.loadUri("https://google.com")

        toolBar.setOnUrlCommitListener { url ->
            session.loadUri(url)
            toolBar.url = url

            true
        }

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

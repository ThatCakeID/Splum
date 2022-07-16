package com.thatcakeid.splum

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import mozilla.components.browser.domains.autocomplete.CustomDomainsProvider
import mozilla.components.browser.domains.autocomplete.ShippedDomainsProvider
import mozilla.components.browser.menu.BrowserMenuBuilder
import mozilla.components.browser.menu.item.BrowserMenuDivider
import mozilla.components.browser.menu.item.BrowserMenuImageSwitch
import mozilla.components.browser.menu.item.BrowserMenuImageText
import mozilla.components.browser.menu.item.BrowserMenuItemToolbar
import mozilla.components.browser.toolbar.BrowserToolbar
import mozilla.components.concept.toolbar.Toolbar
import mozilla.components.feature.toolbar.ToolbarAutocompleteFeature
import mozilla.components.feature.toolbar.WebExtensionToolbarFeature
import mozilla.components.support.base.feature.ViewBoundFeatureWrapper
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoSession.ContentDelegate
import org.mozilla.geckoview.GeckoSession.ProgressDelegate
import org.mozilla.geckoview.GeckoSessionSettings
import org.mozilla.geckoview.GeckoView


class MainActivity : AppCompatActivity() {
    private var sRuntime: GeckoRuntime? = null

    private val shippedDomainsProvider = ShippedDomainsProvider()
    private val customDomainsProvider = CustomDomainsProvider()

    private val webExtToolbarFeature = ViewBoundFeatureWrapper<WebExtensionToolbarFeature>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val geckoView = findViewById<GeckoView>(R.id.geckoview)
        val toolBar = findViewById<BrowserToolbar>(R.id.toolBar)

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

        val extensionsItemIc = BrowserMenuImageText("Extensions", R.drawable.ic_extension) { /* Do nothing */ }

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

        val settingsItem        = BrowserMenuImageText("Settings", R.drawable.ic_settings) {
            val si = Intent(this, SettingsActivity::class.java)

            startActivity(si)
        }

        val items = listOf(menuToolbar, BrowserMenuDivider(), newTabItem, newTabIncognitoItem, BrowserMenuDivider(), extensionsItemIc, BrowserMenuDivider(), historyItemIc, downloadsItemIc, bookmarksItemIc, BrowserMenuDivider(), shareItemIc, desktopItemIc, BrowserMenuDivider(), settingsItem)
        toolBar.display.menuBuilder = BrowserMenuBuilder(items)
        toolBar.elevation = 8F

        if (sRuntime == null) {
            sRuntime = GeckoRuntime.create(this)
        }

        session.open(sRuntime!!)
        geckoView.setSession(session)

        if(intent?.action == Intent.ACTION_VIEW) {
            toolBar.url = intent?.data.toString()
            session.loadUri(intent?.data.toString())
        } else {
            toolBar.url = "https://google.com/"
            session.loadUri("https://google.com")
        }

        toolBar.setOnUrlCommitListener { url ->
            session.loadUri(url)
            toolBar.url = url

            true
        }

        session.progressDelegate = object : ProgressDelegate {
            override fun onPageStart(session: GeckoSession, url: String) {
                toolBar.url = url
            }

            override fun onProgressChange(session: GeckoSession, progress: Int) {
                toolBar.displayProgress(progress)
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

        session.navigationDelegate = object : GeckoSession.NavigationDelegate {
            override fun onLocationChange(session: GeckoSession, url: String?) {
                toolBar.url = url!!
            }
        }
    }
}

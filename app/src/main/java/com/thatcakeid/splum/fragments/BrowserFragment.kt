package com.thatcakeid.splum.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.thatcakeid.splum.MainActivity
import com.thatcakeid.splum.R
import mozilla.components.browser.domains.autocomplete.CustomDomainsProvider
import mozilla.components.browser.domains.autocomplete.ShippedDomainsProvider
import mozilla.components.browser.menu.BrowserMenuBuilder
import mozilla.components.browser.menu.item.BrowserMenuDivider
import mozilla.components.browser.menu.item.BrowserMenuImageSwitch
import mozilla.components.browser.menu.item.BrowserMenuImageText
import mozilla.components.browser.menu.item.BrowserMenuItemToolbar
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.browser.toolbar.BrowserToolbar
import mozilla.components.concept.toolbar.Toolbar
import mozilla.components.feature.tabs.toolbar.TabsToolbarFeature
import mozilla.components.feature.toolbar.ToolbarAutocompleteFeature
import mozilla.components.feature.toolbar.WebExtensionToolbarFeature
import mozilla.components.support.base.feature.ViewBoundFeatureWrapper
import mozilla.components.support.utils.URLStringUtils
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoSessionSettings
import org.mozilla.geckoview.GeckoView


class BrowserFragment : Fragment() {
    private var sRuntime: GeckoRuntime? = null

    private val shippedDomainsProvider = ShippedDomainsProvider()
    private val customDomainsProvider = CustomDomainsProvider()

    private val webExtToolbarFeature = ViewBoundFeatureWrapper<WebExtensionToolbarFeature>()

    private var openUrl: String? = null;

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            openUrl = it.getString("openUrl")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val layout: View = inflater.inflate(R.layout.fragment_browser, container, false)

        val geckoView = layout.findViewById<GeckoView>(R.id.geckoview)
        val toolBar   = layout.findViewById<BrowserToolbar>(R.id.toolBar)

        shippedDomainsProvider.initialize(requireActivity().applicationContext)
        customDomainsProvider.initialize(requireActivity().applicationContext)

        ToolbarAutocompleteFeature(toolBar).apply {
            this.addDomainProvider(shippedDomainsProvider)
            this.addDomainProvider(customDomainsProvider)
        }

        val osBuildRelease = android.os.Build.VERSION.RELEASE.toString()
        val osBuildModel = android.os.Build.MODEL

        val settings = GeckoSessionSettings.Builder()
            .useTrackingProtection(false)
            .userAgentOverride("Mozilla/5.0 (Linux; Android $osBuildRelease; $osBuildModel) AppleWebKit/537.36 (KHTML, like Gecko) Splum/100.0.20220425210429 Mobile Safari/537.36")
            .build()

        val session = GeckoSession(settings)
            session.contentDelegate = object : GeckoSession.ContentDelegate {}

        if (sRuntime == null)
            sRuntime = GeckoRuntime.create(requireActivity().applicationContext)

        session.open(sRuntime!!)
        geckoView.setSession(session)

        toolBar.url = openUrl.toString()
        session.loadUri(openUrl.toString())

        session.progressDelegate   = object : GeckoSession.ProgressDelegate {
            override fun onPageStart(session: GeckoSession, url: String) {
                toolBar.url = url
            }

            override fun onProgressChange(session: GeckoSession, progress: Int) {
                toolBar.displayProgress(progress)
            }

            override fun onSecurityChange(
                session: GeckoSession,
                securityInfo: GeckoSession.ProgressDelegate.SecurityInformation
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

        fun setupToolBar() {
            toolBar.setBackgroundColor(ContextCompat.getColor(requireActivity().applicationContext, R.color.colorPrimary))

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

            val extensionsItemIc    = BrowserMenuImageText("Extensions", R.drawable.ic_extension) { /* Do nothing */ }

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
            val desktopItemIc       = BrowserMenuImageSwitch(R.drawable.ic_desktop, "Desktop View", listener = { checked ->
                val currentUrl: String = toolBar.url.toString()

                if(checked) {
                    val newSession = GeckoSession(GeckoSessionSettings.Builder()
                        .useTrackingProtection(false)
                        .userAgentOverride("Mozilla/5.0 (Linux; X11; Linux x86_64; rv:10.0) AppleWebKit/537.36 (KHTML, like Gecko) Splum/100.0.20220425210429 Mobile Safari/537.36")
                        .build())

                    newSession.contentDelegate = object : GeckoSession.ContentDelegate {}
                    geckoView.setSession(newSession)
                    newSession.loadUri(currentUrl)
                } else {
                    geckoView.setSession(session)
                    session.loadUri(currentUrl)
                }
            })

            val settingsItem        = BrowserMenuImageText("Settings", R.drawable.ic_settings) {
                (activity as MainActivity?)!!.setCurrentFragmentVar("settings")

                requireActivity()
                    .supportFragmentManager
                    .beginTransaction()
                    .replace(R.id.fragmentContainerView, SettingsFragment(), "settingsFragment")
                    .commit()
            }

            val items = listOf(menuToolbar, BrowserMenuDivider(), newTabItem, newTabIncognitoItem, BrowserMenuDivider(), extensionsItemIc, BrowserMenuDivider(), historyItemIc, downloadsItemIc, bookmarksItemIc, BrowserMenuDivider(), shareItemIc, desktopItemIc, BrowserMenuDivider(), settingsItem)
            toolBar.display.menuBuilder = BrowserMenuBuilder(items)
            toolBar.elevation = 8F

            toolBar.setOnUrlCommitListener { url ->
                session.loadUri(url)
                toolBar.url = url

                true
            }

            toolBar.display.urlFormatter = { url ->
                URLStringUtils.toDisplayUrl(url)
            }

            TabsToolbarFeature(
                toolbar = toolBar,
                store = BrowserStore(),
                sessionId = "sess",
                lifecycleOwner = this,
                showTabs = ::showTabs,
                countBasedOnSelectedTabType = false
            )
        }
        setupToolBar()

        return layout
    }

    private fun showTabs() {
        Toast.makeText(requireActivity().applicationContext, "tAB!!!s", Toast.LENGTH_LONG)
    }

    companion object {
        fun newInstance(openUrl: String) =
            BrowserFragment().apply {
                arguments = Bundle().apply {
                    putString("openUrl", openUrl)
                }
            }
    }
}
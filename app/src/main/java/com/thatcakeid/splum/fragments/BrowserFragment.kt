package com.thatcakeid.splum.fragments

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.thatcakeid.splum.MainActivity
import com.thatcakeid.splum.R
import com.thatcakeid.splum.tools.MainRequestInterceptor
import mozilla.components.browser.domains.CustomDomains
import mozilla.components.browser.domains.autocomplete.CustomDomainsProvider
import mozilla.components.browser.domains.autocomplete.ShippedDomainsProvider
import mozilla.components.browser.engine.gecko.GeckoEngine
import mozilla.components.browser.engine.gecko.GeckoEngineSession
import mozilla.components.browser.engine.gecko.GeckoEngineView
import mozilla.components.browser.menu.BrowserMenuBuilder
import mozilla.components.browser.menu.item.BrowserMenuDivider
import mozilla.components.browser.menu.item.BrowserMenuImageSwitch
import mozilla.components.browser.menu.item.BrowserMenuImageText
import mozilla.components.browser.menu.item.BrowserMenuItemToolbar
import mozilla.components.browser.state.engine.EngineMiddleware
import mozilla.components.browser.state.state.TabSessionState
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.browser.tabstray.TabsTray
import mozilla.components.browser.toolbar.BrowserToolbar
import mozilla.components.concept.engine.DefaultSettings
import mozilla.components.concept.engine.Engine
import mozilla.components.concept.engine.EngineSession
import mozilla.components.concept.toolbar.Toolbar
import mozilla.components.feature.session.SessionFeature
import mozilla.components.feature.session.SessionUseCases
import mozilla.components.feature.tabs.TabsUseCases
import mozilla.components.feature.tabs.tabstray.TabsFeature
import mozilla.components.feature.tabs.toolbar.TabsToolbarFeature
import mozilla.components.feature.toolbar.ToolbarAutocompleteFeature
import mozilla.components.support.utils.URLStringUtils
import org.mozilla.geckoview.GeckoRuntime

class BrowserFragment : Fragment() {
    private val shippedDomainsProvider = ShippedDomainsProvider()
    private val customDomainsProvider  = CustomDomainsProvider()

    private var canGoBack    = false
    private var canGoForward = false

    private var isLoading   = false
    private var isDesktop   = false
    private var isLightMode = false
    private var isFavourite = false

    private var openUrl:  String?       = null
    private var sRuntime: GeckoRuntime? = null

    private var browserStore: BrowserStore? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            openUrl  = it.getString("openUrl")
            sRuntime = it.getParcelable("sRuntime") as GeckoRuntime?
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val layout: View = inflater.inflate(R.layout.fragment_browser, container, false)

        val geckoView = layout.findViewById<GeckoEngineView>(R.id.geckoView)
        val toolBar   = layout.findViewById<BrowserToolbar>(R.id.toolBar)

        browserStore  = (activity as MainActivity?)!!.browserStore

        resources.getStringArray(R.array.auto_complete_urls).forEach { CustomDomains.add(requireContext(), it) }

        shippedDomainsProvider.initialize(requireActivity().applicationContext)
        customDomainsProvider.initialize(requireActivity().applicationContext)

        ToolbarAutocompleteFeature(toolBar).apply {
            this.addDomainProvider(shippedDomainsProvider)
            this.addDomainProvider(customDomainsProvider)
        }

        when (requireContext().resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
            Configuration.UI_MODE_NIGHT_YES       -> { isLightMode = false }
            Configuration.UI_MODE_NIGHT_NO        -> { isLightMode = true }
            Configuration.UI_MODE_NIGHT_UNDEFINED -> { isLightMode = true }
        }

        val settings = DefaultSettings().apply {
            userAgentString = "Mozilla/5.0 (Linux; Android ${android.os.Build.VERSION.RELEASE} ${android.os.Build.MODEL}) AppleWebKit/537.36 (KHTML, like Gecko) Splum/100.0.20220425210429 Mobile Safari/537.36"
            requestInterceptor = MainRequestInterceptor(requireContext())
            allowContentAccess = true
            allowFileAccess = true
        }

        val geckoEngineSession = GeckoEngineSession(sRuntime!!, defaultSettings = settings, openGeckoSession = true)

        /*
        SessionFeature(
            store = browserStore!!,
            engineView = geckoView,
            goBackUseCase = SessionUseCases(browserStore!!).goBack
        ).start()
        */

        geckoEngineSession.register(object : EngineSession.Observer {
            override fun onLocationChange(url: String) { toolBar.url = url }
            override fun onProgress(progress: Int) { toolBar.displayProgress(progress) }
            override fun onLoadingStateChange(loading: Boolean) { isLoading = loading }

            override fun onSecurityChange(secure: Boolean, host: String?, issuer: String?) {
                if (secure) toolBar.siteSecure = Toolbar.SiteSecurity.SECURE
                else toolBar.siteSecure = Toolbar.SiteSecurity.INSECURE

                super.onSecurityChange(secure, host, issuer)
            }

            override fun onNavigationStateChange(canGoBack: Boolean?, canGoForward: Boolean?) {
                if (canGoBack != null)    this@BrowserFragment.canGoBack    = canGoBack
                if (canGoForward != null) this@BrowserFragment.canGoForward = canGoForward

                super.onNavigationStateChange(canGoBack, canGoForward)
            }
        })

        fun setupToolBar() {
            if (isLightMode)
                toolBar.setBackgroundColor(ContextCompat.getColor(requireActivity().applicationContext, R.color.colorPrimaryLight))
            else
                toolBar.setBackgroundColor(ContextCompat.getColor(requireActivity().applicationContext, R.color.colorPrimaryDark))

            val backIc = BrowserMenuItemToolbar.Button(
                R.drawable.ic_arrow_back,
                "Back",
                isEnabled = { true }
            ) {
                if (!canGoBack) return@Button
                geckoEngineSession.goBack()
            }

            val forwardIc = BrowserMenuItemToolbar.Button(
                R.drawable.ic_arrow_forward,
                "Forward",
            ) {
                if (!canGoForward) return@Button
                geckoEngineSession.goForward()
            }

            val reloadIc = BrowserMenuItemToolbar.TwoStateButton(
                primaryImageResource = R.drawable.ic_refresh,
                primaryContentDescription = "Reload",
                secondaryImageResource = R.drawable.ic_close,
                secondaryContentDescription = "Stop Reload",
                isInPrimaryState = { !isLoading },
                listener = {
                    if (isLoading)
                        geckoEngineSession.stopLoading()
                    else
                        geckoEngineSession.reload()
                })

            val bookmarkIc = BrowserMenuItemToolbar.TwoStateButton(
                primaryImageResource = R.drawable.ic_bookmark_border,
                primaryContentDescription = "Add to favorites",
                secondaryImageResource = R.drawable.ic_bookmark,
                secondaryContentDescription = "Remove from favorites",
                isInPrimaryState = { !isFavourite },
                listener = {
                    if(isFavourite)
                        isFavourite = false
                    else
                        isFavourite = true
                })

            val menuToolbar         = BrowserMenuItemToolbar(listOf(backIc, forwardIc, reloadIc, bookmarkIc))

            val newTabItem          = BrowserMenuImageText("New Tab", R.drawable.ic_add) { /* Do nothing */ }
            val newTabIncognitoItem = BrowserMenuImageText("New Private Tab", R.drawable.ic_add) { /* Do nothing */ }

            val extensionsItemIc    = BrowserMenuImageText("Extensions", R.drawable.ic_extension) { /* Do nothing */ }

            val historyItemIc       = BrowserMenuImageText("History", R.drawable.ic_history) { /* Do nothing */ }
            val downloadsItemIc     = BrowserMenuImageText("Downloads", R.drawable.ic_download) { /* Do nothing */ }
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
            val desktopItemIc       = BrowserMenuImageSwitch(R.drawable.ic_desktop, "Desktop View",
                initialState = { isDesktop }) { checked ->
                isDesktop = checked
                geckoEngineSession.toggleDesktopMode(checked, true)

                return@BrowserMenuImageSwitch
            }

            val settingsItem        = BrowserMenuImageText("Settings", R.drawable.ic_settings) {
                //(activity as MainActivity?)!!.setCurrentFragmentVar("settings")

                requireActivity()
                    .supportFragmentManager
                    .beginTransaction()
                    .addToBackStack("settingsFragment")
                    .replace(R.id.fragmentContainerView, SettingsFragment(), "settingsFragment")
                    .commit()
            }


            val items = listOf(
                menuToolbar,
                BrowserMenuDivider(),
                newTabItem,
                newTabIncognitoItem,
                BrowserMenuDivider(),
                extensionsItemIc,
                BrowserMenuDivider(),
                historyItemIc,
                downloadsItemIc,
                bookmarksItemIc,
                BrowserMenuDivider(),
                shareItemIc,
                desktopItemIc,
                BrowserMenuDivider(),
                settingsItem)

            toolBar.display.menuBuilder = BrowserMenuBuilder(items)
            toolBar.display.hint        = "Enter an URL or search"
            toolBar.edit.hint           = "Enter an URL or search"
            toolBar.elevation           = 8F

            toolBar.setOnUrlCommitListener { url ->
                geckoEngineSession.loadUrl(url)
                toolBar.url = url

                true
            }

            toolBar.display.urlFormatter = { url ->
                URLStringUtils.toDisplayUrl(url)
            }

            toolBar.edit.setOnEditFocusChangeListener { hasFocus ->
                if (!hasFocus) toolBar.displayMode()
            }

            TabsToolbarFeature(
                toolbar = toolBar,
                store = browserStore!!,
                sessionId = "sess",
                lifecycleOwner = this,
                showTabs = ::showTabs,
                countBasedOnSelectedTabType = false
            )
        }

        setupToolBar()
        geckoView.render(geckoEngineSession)

        geckoEngineSession.loadUrl(openUrl.toString())
        toolBar.url = openUrl.toString()

        return layout
    }

    private fun showTabs() {
        Toast.makeText(requireActivity().applicationContext, "tAB!!!s", Toast.LENGTH_LONG).show()

        requireActivity()
            .supportFragmentManager
            .beginTransaction()
            .addToBackStack("tabsFragment")
            .replace(R.id.fragmentContainerView, TabsFragment.newInstance(browserStore), "tabsFragment")
            .commit()
    }

    companion object {
        fun newInstance(openUrl: String, sRuntime: GeckoRuntime?) =
            BrowserFragment().apply {
                arguments = Bundle().apply {
                    putString("openUrl", openUrl)
                    putParcelable("sRuntime", sRuntime)
                }
            }
    }
}
package com.thatcakeid.splum.fragments

import android.app.Notification
import android.app.NotificationManager
import android.content.Intent
import android.content.res.Resources
import android.media.session.MediaSession.Token
import android.opengl.Visibility
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompatSideChannelService
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowCompat.getInsetsController
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.Fragment
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.thatcakeid.splum.MainActivity
import com.thatcakeid.splum.R
import com.thatcakeid.splum.classes.MainRequestInterceptor
import mozilla.components.browser.domains.CustomDomains
import mozilla.components.browser.domains.autocomplete.CustomDomainsProvider
import mozilla.components.browser.domains.autocomplete.ShippedDomainsProvider
import mozilla.components.browser.engine.gecko.GeckoEngineSession
import mozilla.components.browser.engine.gecko.GeckoEngineView
import mozilla.components.browser.menu.BrowserMenuBuilder
import mozilla.components.browser.menu.item.BrowserMenuDivider
import mozilla.components.browser.menu.item.BrowserMenuImageSwitch
import mozilla.components.browser.menu.item.BrowserMenuImageText
import mozilla.components.browser.menu.item.BrowserMenuItemToolbar
import mozilla.components.browser.state.state.BrowserState
import mozilla.components.browser.state.state.TabSessionState
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.browser.tabstray.TabViewHolder
import mozilla.components.browser.tabstray.TabsTray
import mozilla.components.browser.tabstray.TabsTrayStyling
import mozilla.components.browser.toolbar.BrowserToolbar
import mozilla.components.concept.engine.DefaultSettings
import mozilla.components.concept.engine.EngineSession
import mozilla.components.concept.engine.EngineSessionState
import mozilla.components.concept.engine.EngineView
import mozilla.components.concept.engine.manifest.WebAppManifest
import mozilla.components.concept.engine.mediasession.MediaSession
import mozilla.components.concept.engine.permission.PermissionRequest
import mozilla.components.concept.engine.prompt.PromptRequest
import mozilla.components.concept.fetch.Response.Companion.SUCCESS
import mozilla.components.concept.toolbar.Toolbar
import mozilla.components.feature.downloads.DownloadsFeature
import mozilla.components.feature.downloads.DownloadsUseCases
import mozilla.components.feature.tabs.tabstray.TabsFeature
import mozilla.components.feature.tabs.toolbar.TabsToolbarFeature
import mozilla.components.feature.toolbar.ToolbarAutocompleteFeature
import mozilla.components.feature.toolbar.WebExtensionToolbarFeature
import mozilla.components.support.base.feature.ViewBoundFeatureWrapper
import mozilla.components.support.utils.URLStringUtils
import mozilla.components.ui.tabcounter.TabCounterMenu
import org.mozilla.geckoview.*

class BrowserFragment : Fragment() {
    private val shippedDomainsProvider = ShippedDomainsProvider()
    private val customDomainsProvider  = CustomDomainsProvider()

    private val webExtToolbarFeature = ViewBoundFeatureWrapper<WebExtensionToolbarFeature>()

    private var canGoBack = false
    private var canGoForward = false

    private var isLoading = false
    private var isDesktop = false

    private var openUrl: String? = null
    private var sRuntime: GeckoRuntime? = null

    private var browserStore: BrowserStore = BrowserStore()
    private var mediaNotif: Notification? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            openUrl = it.getString("openUrl")
            sRuntime = it.getParcelable("sRuntime") as GeckoRuntime?
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)

        val layout: View = inflater.inflate(R.layout.fragment_browser, container, false)

        val geckoView = layout.findViewById<GeckoEngineView>(R.id.geckoview)
        val toolBar   = layout.findViewById<BrowserToolbar>(R.id.toolBar)

        val customAutoCompleteDomains = resources.getStringArray(R.array.search_engines)

        customAutoCompleteDomains.forEach { it ->
            CustomDomains.add(requireContext(), it)
        }

        shippedDomainsProvider.initialize(requireActivity().applicationContext)
        customDomainsProvider.initialize(requireActivity().applicationContext)

        ToolbarAutocompleteFeature(toolBar).apply {
            this.addDomainProvider(shippedDomainsProvider)
            this.addDomainProvider(customDomainsProvider)
        }

        val osBuildRelease = android.os.Build.VERSION.RELEASE.toString()
        val osBuildModel = android.os.Build.MODEL

        val settings = DefaultSettings().apply {
            userAgentString = "Mozilla/5.0 (Linux; Android $osBuildRelease; $osBuildModel) AppleWebKit/537.36 (KHTML, like Gecko) Splum/100.0.20220425210429 Mobile Safari/537.36"
            requestInterceptor = MainRequestInterceptor(requireContext())
            allowContentAccess = true
            allowFileAccess = true
        }

        val session = GeckoEngineSession(sRuntime!!, defaultSettings = settings, openGeckoSession = true)

        geckoView.render(session)

        toolBar.url = openUrl.toString()
        session.loadUrl(openUrl.toString())

        session.register(object : EngineSession.Observer {
            override fun onLocationChange(url: String) {
                toolBar.url = url
            }

            override fun onProgress(progress: Int) {
                toolBar.displayProgress(progress)
            }

            override fun onSecurityChange(secure: Boolean, host: String?, issuer: String?) {
                if (secure)
                    toolBar.siteSecure = Toolbar.SiteSecurity.SECURE
                else
                    toolBar.siteSecure = Toolbar.SiteSecurity.INSECURE

                super.onSecurityChange(secure, host, issuer)
            }

            override fun onMediaFullscreenChanged(
                fullscreen: Boolean,
                elementMetadata: MediaSession.ElementMetadata?
            ) {
                if(fullscreen) {
                    toolBar.visibility = GONE

                    val windowInsetsController =
                        getInsetsController(requireActivity().window, requireActivity().window.decorView)

                    windowInsetsController.systemBarsBehavior =
                        WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

                    windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
                } else {
                    toolBar.visibility = VISIBLE

                    val windowInsetsController =
                        getInsetsController(requireActivity().window, requireActivity().window.decorView)

                    windowInsetsController.show(WindowInsetsCompat.Type.systemBars())
                }

                super.onMediaFullscreenChanged(fullscreen, elementMetadata)
            }

            override fun onNavigationStateChange(canGoBack: Boolean?, canGoForward: Boolean?) {
                if(canGoBack == null) this@BrowserFragment.canGoBack = false
                else this@BrowserFragment.canGoBack = canGoBack

                if(canGoForward == null) this@BrowserFragment.canGoBack = false
                else this@BrowserFragment.canGoForward = canGoForward

                super.onNavigationStateChange(canGoBack, canGoForward)
            }

            override fun onAppPermissionRequest(permissionRequest: PermissionRequest) {


                super.onAppPermissionRequest(permissionRequest)
            }

            override fun onLoadingStateChange(loading: Boolean) {
                isLoading = loading

                super.onLoadingStateChange(loading)
            }
        })

        DownloadsFeature(requireContext(), store = browserStore, useCases = DownloadsUseCases(browserStore), fragmentManager = childFragmentManager).start()

        /*

        session.promptDelegate = object : GeckoSession.PromptDelegate {
            override fun onAlertPrompt(
                session: GeckoSession,
                prompt: GeckoSession.PromptDelegate.AlertPrompt
            ): GeckoResult<GeckoSession.PromptDelegate.PromptResponse>? {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Message from ${toolBar.url}.")
                    .setMessage(prompt.message)
                    .setPositiveButton("Ok") { _, _ -> prompt.dismiss() }
                    .show()

                return super.onAlertPrompt(session, prompt)
            }

            override fun onSharePrompt(
                session: GeckoSession,
                prompt: GeckoSession.PromptDelegate.SharePrompt
            ): GeckoResult<GeckoSession.PromptDelegate.PromptResponse>? {
                val sendIntent: Intent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, prompt.text)
                    type = "text/plain"
                }

                val shareIntent = Intent.createChooser(sendIntent, null)
                startActivity(shareIntent)
                prompt.confirm(SUCCESS)

                return super.onSharePrompt(session, prompt)
            }

            override fun onDateTimePrompt(
                session: GeckoSession,
                prompt: GeckoSession.PromptDelegate.DateTimePrompt
            ): GeckoResult<GeckoSession.PromptDelegate.PromptResponse>? {
                val datePicker =
                    MaterialDatePicker.Builder.datePicker()
                        .setTitleText("Select date")
                        .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                        .build()

                datePicker.addOnPositiveButtonClickListener {
                    prompt.confirm(datePicker.selection.toString())
                }

                datePicker.show(requireActivity().supportFragmentManager, "datePick")

                return super.onDateTimePrompt(session, prompt)
            }
        }
        */

        fun setupToolBar() {
            toolBar.setBackgroundColor(ContextCompat.getColor(requireActivity().applicationContext, R.color.colorPrimary))

            val backIc = BrowserMenuItemToolbar.Button(
                R.drawable.ic_arrow_back,
                "Back",
                isEnabled = { true }
            ) {
                if (!canGoBack) return@Button
                session.goBack()
            }

            val forwardIc = BrowserMenuItemToolbar.Button(R.drawable.ic_arrow_forward, "Forward") {
                if (!canGoForward) return@Button
                session.goForward()
            }

            val reloadIc = BrowserMenuItemToolbar.TwoStateButton(
                primaryImageResource = R.drawable.ic_refresh,
                primaryContentDescription = "Reload",
                secondaryImageResource = R.drawable.ic_close,
                secondaryContentDescription = "Stop Reload",
                isInPrimaryState = { isLoading },
                listener = {
                    if(isLoading)
                        session.stopLoading()
                    else
                        session.reload()
                })
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
            val desktopItemIc       = BrowserMenuImageSwitch(R.drawable.ic_desktop, "Desktop View", initialState = { isDesktop }, listener = { checked ->
                isDesktop = checked
                session.toggleDesktopMode(checked, true)

                return@BrowserMenuImageSwitch
            })

            val settingsItem        = BrowserMenuImageText("Settings", R.drawable.ic_settings) {
                (activity as MainActivity?)!!.setCurrentFragmentVar("settings")

                requireActivity()
                    .supportFragmentManager
                    .beginTransaction()
                    .addToBackStack("settingsFragment")
                    .replace(R.id.fragmentContainerView, SettingsFragment(), "settingsFragment")
                    .commit()
            }
            val exitItem            = BrowserMenuImageText("Exit", R.drawable.ic_exit) {
                sRuntime?.shutdown()
                requireActivity().finish()
            }

            val items = listOf(menuToolbar, BrowserMenuDivider(), newTabItem, newTabIncognitoItem, BrowserMenuDivider(), extensionsItemIc, BrowserMenuDivider(), historyItemIc, downloadsItemIc, bookmarksItemIc, BrowserMenuDivider(), shareItemIc, desktopItemIc, BrowserMenuDivider(), settingsItem, exitItem)
            toolBar.display.menuBuilder = BrowserMenuBuilder(items)

            toolBar.display.hint = "Enter an URL or search"
            toolBar.edit.hint = "Enter an URL or search"
            toolBar.elevation = 8F

            toolBar.setOnUrlCommitListener { url ->
                session.loadUrl(url)
                toolBar.url = url

                true
            }

            toolBar.display.urlFormatter = { url ->
                URLStringUtils.toDisplayUrl(url)
            }

            TabsToolbarFeature(
                toolbar = toolBar,
                store = browserStore,
                sessionId = "sess",
                lifecycleOwner = this,
                showTabs = ::showTabs,
                countBasedOnSelectedTabType = false,
                tabCounterMenu = TabCounterMenu(requireContext(), iconColor = 0xFFFFFF, onItemTapped = {
                    Toast.makeText(requireActivity().applicationContext, "tAB!!!s 2", Toast.LENGTH_LONG).show()
                })
            )
        }

        fun registerBackPressed() {
            requireActivity()
                .onBackPressedDispatcher
                .addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
                    override fun handleOnBackPressed() {
                        if (canGoBack)
                            session.goBack()
                        else
                            requireActivity().moveTaskToBack(true)
                    }
                })
        }

        setupToolBar()
        registerBackPressed()

        return layout
    }

    private fun showTabs() {
        Toast.makeText(requireActivity().applicationContext, "tAB!!!s", Toast.LENGTH_LONG).show()
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
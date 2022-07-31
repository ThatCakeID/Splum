package com.thatcakeid.splum.fragments

import android.os.Bundle
import android.os.Parcelable
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.thatcakeid.splum.MainActivity
import com.thatcakeid.splum.R
import mozilla.components.browser.engine.gecko.GeckoEngineView
import mozilla.components.browser.state.state.TabSessionState
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.browser.tabstray.TabsAdapter
import mozilla.components.browser.tabstray.TabsTray
import mozilla.components.feature.tabs.tabstray.TabsFeature
import org.mozilla.geckoview.GeckoRuntime

class TabsFragment : Fragment() {
    private var browserStore: BrowserStore? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {}
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val layout: View = inflater.inflate(R.layout.fragment_tabs, container, false)

        browserStore = (activity as MainActivity?)!!.browserStore

        val tabsList = layout.findViewById<RecyclerView>(R.id.tabsList)

        tabsList.adapter = tabsAdapter()
        tabsList.layoutManager = GridLayoutManager(context, 2)

        TabsFeature(
            tabsTray = tabsAdapter(),
            store = browserStore!!
        ).start()

        return layout
    }

    private fun tabsAdapter() : TabsAdapter {
        return TabsAdapter(
            delegate = object : TabsTray.Delegate {
                override fun onTabClosed(tab: TabSessionState, source: String?) {
                    Toast.makeText(requireActivity().applicationContext, "onTabClosed", Toast.LENGTH_LONG).show()
                }

                override fun onTabSelected(tab: TabSessionState, source: String?) {
                    Toast.makeText(requireActivity().applicationContext, "onTabSelected", Toast.LENGTH_LONG).show()
                }
            }
        )
    }

    companion object {
        fun newInstance(bStore: BrowserStore?) =
            TabsFragment().apply {
                arguments = Bundle().apply {}
            }
    }
}
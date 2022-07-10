package com.thatcakeid.splum

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoSession.ContentDelegate
import org.mozilla.geckoview.GeckoSession.ProgressDelegate
import org.mozilla.geckoview.GeckoSessionSettings
import org.mozilla.geckoview.GeckoView


class MainActivity : AppCompatActivity() {
    private var sRuntime: GeckoRuntime? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val view = findViewById<GeckoView>(R.id.geckoview)
        val editTextSearch = findViewById<EditText>(R.id.editTextTextPersonName)
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)
        val session = GeckoSession()

        session.contentDelegate = object : ContentDelegate {}
        progressBar.visibility = View.GONE

        if (sRuntime == null) {
            sRuntime = GeckoRuntime.create(this)
        }

        val settings = GeckoSessionSettings.Builder()
            .chromeUri("splum")
            .useTrackingProtection(true)
            .userAgentOverride("")
            .build()

        session.open(sRuntime!!)
        view.setSession(session)
        session.loadUri("https://google.com")

        editTextSearch.setOnEditorActionListener { _, actionId, _ ->
            session.loadUri(editTextSearch.text.toString())
            true
        }

        session.progressDelegate = object : ProgressDelegate {
            override fun onPageStart(session: GeckoSession, url: String) {
                editTextSearch.setText(url)

                progressBar.visibility = View.VISIBLE
                progressBar.progress = 0
            }
            override fun onPageStop(session: GeckoSession, success: Boolean) {
                progressBar.visibility = View.GONE
            }

            override fun onProgressChange(session: GeckoSession, progress: Int) {
                progressBar.progress = progress
            }
        }

        session.navigationDelegate = object : GeckoSession.NavigationDelegate {
            override fun onLocationChange(session: GeckoSession, url: String?) {
                editTextSearch.setText(url)
            }
        }
    }
}

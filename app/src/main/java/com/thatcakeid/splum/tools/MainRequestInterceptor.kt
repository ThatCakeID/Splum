package com.thatcakeid.splum.tools

import android.content.Context
import android.content.res.Configuration
import androidx.preference.PreferenceManager
import com.thatcakeid.splum.R
import mozilla.components.browser.errorpages.ErrorPages
import mozilla.components.browser.errorpages.ErrorType
import mozilla.components.concept.engine.EngineSession
import mozilla.components.concept.engine.request.RequestInterceptor
import java.security.AccessController.getContext

class MainRequestInterceptor(private val context: Context) : RequestInterceptor {
    override fun interceptsAppInitiatedRequests(): Boolean { return true }

    override fun onLoadRequest(
        engineSession: EngineSession,
        uri: String,
        lastUri: String?,
        hasUserGesture: Boolean,
        isSameDomain: Boolean,
        isRedirect: Boolean,
        isDirectNavigation: Boolean,
        isSubframeRequest: Boolean
    ): RequestInterceptor.InterceptionResponse? {
        return when (uri) {
            "splum:homepage" -> {
                var isDarkMode = false
                val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

                when (context.resources?.configuration?.uiMode?.and(Configuration.UI_MODE_NIGHT_MASK)) {
                    Configuration.UI_MODE_NIGHT_YES -> { isDarkMode = true }
                    Configuration.UI_MODE_NIGHT_NO -> {  isDarkMode = false }
                    Configuration.UI_MODE_NIGHT_UNDEFINED -> { isDarkMode = false }
                }

                val response = context.resources.openRawResource(R.raw.homepage).readBytes().decodeToString()
                    .replace("::IsDarkMode", isDarkMode.toString())
                    .replace("::PagesVisited", "[]")
                    .replace("::SearchEngine", sharedPreferences.getString("search_engine", "https://google.com/search")!!)
                    .replace("#", "%23")

                RequestInterceptor.InterceptionResponse.Content(response)
            }
            "splum:secreturl" -> RequestInterceptor.InterceptionResponse.Content("<a href=\"https://www.youtube.com/watch?v=dQw4w9WgXcQ\">Here's the secret</a>")
            else -> super.onLoadRequest(
                engineSession,
                uri,
                lastUri,
                hasUserGesture,
                isSameDomain,
                isRedirect,
                isDirectNavigation,
                isSubframeRequest
            )
        }
    }

    override fun onErrorRequest(
        session: EngineSession,
        errorType: ErrorType,
        uri: String?
    ): RequestInterceptor.ErrorResponse {
        val errorPage = ErrorPages.createUrlEncodedErrorPage(context, errorType, uri)
        return RequestInterceptor.ErrorResponse(errorPage)
    }
}
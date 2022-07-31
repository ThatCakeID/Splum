package com.thatcakeid.splum.tools

import android.content.Context
import mozilla.components.browser.errorpages.ErrorPages
import mozilla.components.browser.errorpages.ErrorType
import mozilla.components.concept.engine.EngineSession
import mozilla.components.concept.engine.request.RequestInterceptor

class MainRequestInterceptor(private val context: Context) : RequestInterceptor {
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
            "sample:about" -> RequestInterceptor.InterceptionResponse.Content("splum:homepage")
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
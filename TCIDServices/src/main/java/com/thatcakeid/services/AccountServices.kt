package com.thatcakeid.services

import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.gson.Gson

/*
 * Account Services
 *
 *
 *
 */
open class AccountServices {
    private val apiUrl: String = "https://theclashfruit.me/tcid/as/"

    public fun getAccount(appPackage: String) : String {
        // TODO
        return "TBA"
    }
}
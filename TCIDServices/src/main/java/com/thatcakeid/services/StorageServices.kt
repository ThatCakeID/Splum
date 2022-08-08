package com.thatcakeid.services

import android.content.Context
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.android.material.dialog.MaterialAlertDialogBuilder

open class StorageServices {
    open fun requireCloudAccess(appName: String, appPackage: String, appContext: Context, uToken: String?) {
        if(!AccountServices().isLoggedIn(appContext))
            AccountServices().getAccount(appContext, null)
        else {
            MaterialAlertDialogBuilder(appContext)
                .setTitle("Storage Access")
                .setMessage(
                    "$appName wants access to your ThatCakeID cloud storage. " +
                            "\n\n" +
                            "The app will only be able to edit only it's own data under '//appdata/$appPackage'."
                )
                .setNeutralButton("?") { _, _ ->
                    val queue = Volley.newRequestQueue(appContext)
                    val url = "https://theclashfruit.me/api/v1/storageAccessAbout"

                    val stringRequest = StringRequest(Request.Method.GET, url,
                        { response ->
                            MaterialAlertDialogBuilder(appContext)
                                .setTitle("About Cloud Storage")
                                .setMessage(response.toString())
                                .setPositiveButton("Ok") { dialog, which ->
                                    // Respond to positive button press
                                }
                                .show()
                        },
                        { })

                    queue.add(stringRequest)
                }
                .setNegativeButton("Deny") { _, _ -> }
                .setPositiveButton("Grant") { _, _ ->
                    if(uToken != null) {
                        val queue = Volley.newRequestQueue(appContext)
                        val url = "https://theclashfruit.me/api/v1/storageAccessGrant"

                        val stringRequest = StringRequest(Request.Method.POST, url,
                            { response ->

                            },
                            { })
                    }
                }
                .show()
        }
    }
}
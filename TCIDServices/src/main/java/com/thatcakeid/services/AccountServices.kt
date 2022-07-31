package com.thatcakeid.services

import android.content.Context
import android.view.LayoutInflater
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/*
 * Account Services
 */
open class AccountServices {

    open fun getAccount(appContext: Context, uToken: String?): Boolean {
        if(uToken != null)
            logIn(appContext)
        else
            accountDetails(uToken, appContext)

        return true
    }

    open fun isLoggedIn(appContext: Context): Boolean {
        return true
    }

    private fun logIn(appContext: Context) {
        val customAlertDialogView = LayoutInflater.from(appContext)
            .inflate(R.layout.login_layout, null, false)

        MaterialAlertDialogBuilder(appContext)
            .setView(customAlertDialogView)
            .setNegativeButton("Cancel") { dialog, which ->

            }
            .setPositiveButton("Log In") { dialog, which ->
            }
            .show()
    }

    private fun accountDetails(cAccount: String?, appContext: Context) {
        if(cAccount == null)
            logIn(appContext)
        else {
            MaterialAlertDialogBuilder(appContext)
                .setTitle("Account Details")
                .setMessage("Username: joemama69\nE-Mail: admin@joemama69.com")
                .setNeutralButton("Log out") { dialog, which ->

                }
                .setPositiveButton("Ok") { dialog, which ->
                }
                .show()
        }
    }
}
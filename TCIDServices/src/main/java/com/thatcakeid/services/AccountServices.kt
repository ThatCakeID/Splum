package com.thatcakeid.services

import android.content.Context
import android.view.LayoutInflater
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/*
 * Account Services
 */
open class AccountServices {

    open fun getAccount(appContext: Context): Boolean {
        checkForAccount("com.thatcakeid.splum", appContext)

        return true
    }

    private fun checkForAccount(appPackage: String, appContext: Context) {
        val isLoggedIn        = false
        val cAccount: String? = null

        if(isLoggedIn) {
            accountDetails(cAccount, appContext)
        } else
            logIn(appContext)
    }

    private fun logIn(appContext: Context) {
        val customAlertDialogView = LayoutInflater.from(appContext)
            .inflate(R.layout.login_layout, null, false)

        MaterialAlertDialogBuilder(appContext)
            .setView(customAlertDialogView)
            .setNegativeButton("Cancel") { dialog, which ->
                // Respond to negative button press
            }
            .setPositiveButton("Log In") { dialog, which ->
                // Respond to positive button press
            }
            .show()
    }

    private fun accountDetails(cAccount: String?, appContext: Context) {
        if(cAccount == null)
            logIn(appContext)
    }
}
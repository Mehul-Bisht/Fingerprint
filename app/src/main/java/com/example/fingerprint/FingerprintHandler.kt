package com.example.fingerprint

import android.annotation.TargetApi
import android.app.Activity
import android.content.Context
import android.hardware.fingerprint.FingerprintManager
import android.hardware.fingerprint.FingerprintManager.AuthenticationResult
import android.os.Build
import android.os.CancellationSignal
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat

@TargetApi(Build.VERSION_CODES.M)
class FingerprintHandler(private val context: Context) :
    FingerprintManager.AuthenticationCallback() {

     var isAuthenticated = false

    fun startAuth( // i think all those meyhods that are overrididng are here
        fingerprintManager: FingerprintManager,
        cryptoObject: FingerprintManager.CryptoObject?
    ) {
        val cancellationSignal = CancellationSignal()
        fingerprintManager.authenticate(cryptoObject, cancellationSignal, 0, this, null)
    }

    override fun onAuthenticationError(
        errorCode: Int, errString: CharSequence
    ) {
        update("There was an Auth Error. $errString", false)

    }

    override fun onAuthenticationFailed() {
        update("Authentication Failed. ", false)

    }

    override fun onAuthenticationHelp(
        helpCode: Int,
        helpString: CharSequence
    ) {
        update("Error: $helpString", false)

    }

    override fun onAuthenticationSucceeded(result: AuthenticationResult) {
        isAuthenticated = true
       update("Successfully Authenticated",isAuthenticated)
    }

    private fun update(s: String, b: Boolean) {
        val paraLabel = (context as Activity).findViewById<View>(R.id.paraLabel) as TextView
        val imageView = context.findViewById<View>(R.id.fingerprintImage) as ImageView
        paraLabel.text = s
        if(!b){
            isAuthenticated = false
            paraLabel.setTextColor(ContextCompat.getColor(context, R.color.Red))
        } else {
            isAuthenticated = true
            paraLabel.setTextColor(ContextCompat.getColor(context, R.color.colorAccent))
            imageView.setImageResource(R.mipmap.action_done)
        }

    }

    fun getAuthStatus(): Boolean {
        return isAuthenticated
    }


}
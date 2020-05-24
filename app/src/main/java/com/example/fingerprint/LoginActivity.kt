package com.example.fingerprint

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.security.keystore.UserNotAuthenticatedException
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import kotlinx.android.synthetic.main.activity_login.*
import java.nio.charset.Charset
import java.security.InvalidKeyException
import java.security.KeyStore
import java.util.*
import java.util.concurrent.Executor
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

class LoginActivity : AppCompatActivity() {

    private lateinit var executor: Executor
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo
    private val KEY_NAME = "AndroidKey"

    val SHARED_PREFS = "sharedprefs"
    val SWITCH1 = "switch1"
    val AUTH_STATUS = "authstatus"

    var switchonoff = false
    lateinit var authstatus : String /* "auth turned OFF" */

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        save.visibility = View.INVISIBLE

        loadData()
        updateViews()

        switch1.setOnClickListener(){
            if(switch1.isChecked == switchonoff){
                save.visibility = View.INVISIBLE
            }
            else
                save.visibility = View.VISIBLE
        }

        if(authstatus == "auth turned ON")
            showitup(1)

        save.setOnClickListener(){
            doYourStuff()
        }

        val biometricLoginButton = findViewById<Button>(R.id.biometric_login)

        biometricLoginButton.setOnClickListener {
            // biometricPrompt.authenticate(promptInfo)
            if (switchonoff) {
                fingerprint.setImageResource(R.mipmap.fingerprint_round)
                showitup(10)
            } else /*if (!switchonoff)*/ {
                Toast.makeText(this, "Authentication must be turned ON !", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun doYourStuff(){

        if (switch1.isChecked) {
            showitup(1)
            auth_status.text = "auth turned ON"
            saveData()
            loadData()
            updateViews()
        } else if (!switch1.isChecked) {
            auth_status.text = "auth turned OFF"
            chameleon.text = "Place your finger for biometric verification"
            chameleon.setTextColor(ContextCompat.getColor(this@LoginActivity,R.color.Black))
            saveData()
            loadData()
            updateViews()
        }

        save.visibility = View.INVISIBLE

    }

    private fun showitup( delay : Long ){
        Handler().postDelayed({
            biometricPrompt.authenticate(promptInfo)
            fingerprint.setImageResource(R.mipmap.fingerprint_round)
        }, delay)


        val biometricManager = BiometricManager.from(this)

        when (biometricManager.canAuthenticate()) {
            BiometricManager.BIOMETRIC_SUCCESS ->
                chameleon.text = "Place your finger for biometric verification"
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE ->
                chameleon.text = "No biometric features available on this device."
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE ->
                chameleon.text = "Biometric features are currently unavailable."
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED ->
                chameleon.text =
                    "The user hasn't associated any biometric credentials with their account."
        }

        if (biometricManager.canAuthenticate() == BiometricManager.BIOMETRIC_SUCCESS) {

            executor = ContextCompat.getMainExecutor(this)
            biometricPrompt =
                BiometricPrompt(this, executor, object : BiometricPrompt.AuthenticationCallback() {

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        super.onAuthenticationError(errorCode, errString)
                        chameleon.text = "Authentication error : $errString"
                        chameleon.setTextColor(
                            ContextCompat.getColor(
                                this@LoginActivity,
                                R.color.Red
                            )
                        )
                    }

                    override fun onAuthenticationSucceeded(
                        result: BiometricPrompt.AuthenticationResult
                    ) {
                        super.onAuthenticationSucceeded(result)
                        chameleon.text = "Authentication succeeded!"
                        fingerprint.setImageResource(R.mipmap.action_done_round)
                        chameleon.setTextColor(
                            ContextCompat.getColor(
                                this@LoginActivity,
                                R.color.colorAccent
                            )
                        )

                        Handler().postDelayed(
                            {
                                val intent = Intent(this@LoginActivity, MainActivity::class.java)
                                startActivity(intent)

                            }, 1000
                        )

                    }

                    override fun onAuthenticationFailed() {
                        super.onAuthenticationFailed()
                        chameleon.text = "Authentication failed"
                        chameleon.setTextColor(
                            ContextCompat.getColor(
                                this@LoginActivity,
                                R.color.Red
                            )
                        )

                    }
                })

            promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle("Biometric login for my app")
                .setSubtitle("Log in using your biometric credential")
                //.setNegativeButtonText("Use account password")
                //.setConfirmationRequired(false)
                .setDeviceCredentialAllowed(true)
                .build()

            // Prompt appears when user clicks "Log in".
            // Consider integrating with the keystore to unlock cryptographic operations,
            // if needed by your app.
        }

    }

    private fun saveData(){
        val sharedPreferences = getSharedPreferences(SHARED_PREFS, Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()

        editor.putBoolean(SWITCH1,switch1.isChecked)
        editor.putString(AUTH_STATUS,auth_status.text.toString())

        editor.apply()

        Toast.makeText(this,"Auth status has been saved !",Toast.LENGTH_SHORT).show()
    }

    private fun loadData(){
        val sharedPreferences = getSharedPreferences(SHARED_PREFS, Context.MODE_PRIVATE)
        switchonoff = sharedPreferences.getBoolean(SWITCH1,false)
        authstatus = sharedPreferences.getString(AUTH_STATUS,"auth turned OFF").toString()
        Log.i("Switch status ","$switchonoff")
        Log.i("auth status ",authstatus)
    }

    private fun updateViews(){
        switch1.isChecked = switchonoff
        auth_status.text = authstatus
    }

    private fun generateSecretKey(keyGenParameterSpec: KeyGenParameterSpec) {
        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore"
        )
        keyGenerator.init(keyGenParameterSpec)
        keyGenerator.generateKey()
    }

    private fun getSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore")

        // Before the keystore can be accessed, it must be loaded.
        keyStore.load(null)
        return keyStore.getKey(KEY_NAME, null) as SecretKey
    }

    private fun getCipher(): Cipher {
        return Cipher.getInstance(
            KeyProperties.KEY_ALGORITHM_AES + "/"
                    + KeyProperties.BLOCK_MODE_CBC + "/"
                    + KeyProperties.ENCRYPTION_PADDING_PKCS7
        )
    }

   /* private fun encryptSecretInformation() {
        // Exceptions are unhandled for getCipher() and getSecretKey().
        val cipher = getCipher()
        val secretKey = getSecretKey()
        try {
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)

            val encryptedInfo: ByteArray = cipher.doFinal(plaintext-string.toByteArray(Charset.defaultCharset()))
            Log.d("MY_APP_TAG", "Encrypted information: " + Arrays.toString(encryptedInfo))

        } catch (e: InvalidKeyException) {
            Log.e("MY_APP_TAG", "Key is invalid.")
        } catch (e: UserNotAuthenticatedException) {
            Log.d("MY_APP_TAG", "The key's validity timed out.")
            biometricPrompt.authenticate(promptInfo)
        }
    }
    */
}


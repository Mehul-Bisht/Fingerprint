package com.example.fingerprint

import android.Manifest
import android.annotation.TargetApi
import android.app.KeyguardManager
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.fingerprint.FingerprintManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyProperties
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import java.io.IOException
import java.security.*
import java.security.cert.CertificateException
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.NoSuchPaddingException
import javax.crypto.SecretKey

class MyActivity : AppCompatActivity() {
    private var mHeadingLabel: TextView? = null
    private var mFingerprintImage: ImageView? = null
    private var mParaLabel: TextView? = null
    var fingerprintManager: FingerprintManager? = null
    private var keyguardManager: KeyguardManager? = null
    var keyStore : KeyStore? = null
    private var cipher: Cipher? = null
    private val KEY_NAME = "AndroidKey"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my)

        mHeadingLabel = findViewById(R.id.headingLabel) as TextView?
        mFingerprintImage = findViewById(R.id.fingerprintImage) as ImageView?
        mParaLabel = findViewById(R.id.paraLabel) as TextView?
        // Check 1: Android version should be greater or equal to Marshmallow
// Check 2: Device has Fingerprint Scanner
// Check 3: Have permission to use fingerprint scanner in the app
// Check 4: Lock screen is secured with atleast 1 type of lock
// Check 5: Atleast 1 Fingerprint is registered
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            fingerprintManager = getSystemService(FINGERPRINT_SERVICE) as FingerprintManager?
            keyguardManager = getSystemService(KEYGUARD_SERVICE) as KeyguardManager?

            if (!fingerprintManager!!.isHardwareDetected) {
                mParaLabel!!.text = "Fingerprint Scanner not detected in Device"

            } else if (ContextCompat.checkSelfPermission(this,Manifest.permission.USE_FINGERPRINT) != PackageManager.PERMISSION_GRANTED)

             {
                mParaLabel!!.text = "Permission not granted to use Fingerprint Scanner"
            }
            else if (!keyguardManager!!.isKeyguardSecure) {
                mParaLabel!!.text = "Add Lock to your Phone in Settings"
            }
            else if (!fingerprintManager!!.hasEnrolledFingerprints()) {
                mParaLabel!!.text = "You should add atleast 1 Fingerprint to use this Feature"
            }
            else {
                mParaLabel!!.text = "Place your Finger on Scanner to Access the App."
                generateKey()
                if (cipherInit()) {
                    val cryptoObject =
                        FingerprintManager.CryptoObject(cipher!!)
                    val fingerprintHandler = FingerprintHandler(this)

                    fingerprintHandler.startAuth(fingerprintManager!!, cryptoObject)

                   // Log.i("Mehul", "${fingerprintHandler.getAuthStatus()}")

                     fingerprintHandler.isAuthenticated = true
                    //since it is saing successfully authenticated
                    if(fingerprintHandler.isAuthenticated)
                    {
                      //there is only one way to access this block , by this setting here to true
                        Log.d("TAG", "autehnticated")
                        //here you go https://developer.android.com/training/sign-in/biometric-auth good luck thnx anytime mate ;)
                        Handler().postDelayed({
                            val intent = Intent(this, MainActivity::class.java)
                            startActivity(intent)

                            // finish()

                        }, 1000)
                    }
                }
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    private fun generateKey() {
        try {
            keyStore = KeyStore.getInstance("AndroidKeyStore")
            val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore")

            keyStore!!.load(null)
            keyGenerator.init(
                KeyGenParameterSpec.Builder(
                    KEY_NAME,
                    KeyProperties.PURPOSE_ENCRYPT or
                            KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                    .setUserAuthenticationRequired(true)
                    .setEncryptionPaddings(
                        KeyProperties.ENCRYPTION_PADDING_PKCS7
                    )
                    .build()
            )
            keyGenerator.generateKey()
        } catch (e: KeyStoreException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: CertificateException) {
            e.printStackTrace()
        } catch (e: NoSuchAlgorithmException) {
            e.printStackTrace()
        } catch (e: InvalidAlgorithmParameterException) {
            e.printStackTrace()
        } catch (e: NoSuchProviderException) {
            e.printStackTrace()
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    fun cipherInit(): Boolean {
        cipher = try {
            Cipher.getInstance(KeyProperties.KEY_ALGORITHM_AES + "/" + KeyProperties.BLOCK_MODE_CBC + "/" + KeyProperties.ENCRYPTION_PADDING_PKCS7)
        } catch (e: NoSuchAlgorithmException) {
            throw RuntimeException("Failed to get Cipher", e)
        } catch (e: NoSuchPaddingException) {
            throw RuntimeException("Failed to get Cipher", e)
        }
        return try {
            keyStore!!.load(null)
            val key = keyStore!!.getKey(
                KEY_NAME,
                null
            ) as SecretKey
            cipher!!.init(Cipher.ENCRYPT_MODE, key)
            true
        } catch (e: KeyPermanentlyInvalidatedException) {
            false
        } catch (e: KeyStoreException) {
            throw RuntimeException("Failed to init Cipher", e)
        } catch (e: CertificateException) {
            throw RuntimeException("Failed to init Cipher", e)
        } catch (e: UnrecoverableKeyException) {
            throw RuntimeException("Failed to init Cipher", e)
        } catch (e: IOException) {
            throw RuntimeException("Failed to init Cipher", e)
        } catch (e: NoSuchAlgorithmException) {
            throw RuntimeException("Failed to init Cipher", e)
        } catch (e: InvalidKeyException) {
            throw RuntimeException("Failed to init Cipher", e)
        }
    }
}
package com.deepschneider.addressbook.activities

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyProperties
import android.util.Base64
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricPrompt
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import com.android.volley.*
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.deepschneider.addressbook.BuildConfig
import com.deepschneider.addressbook.R
import com.deepschneider.addressbook.databinding.ActivityLoginBinding
import com.deepschneider.addressbook.databinding.DialogAppInfoBinding
import com.deepschneider.addressbook.utils.Constants
import com.deepschneider.addressbook.utils.Constants.SHARED_PREFERENCE_KEY_IV
import com.deepschneider.addressbook.utils.NetworkUtils
import com.deepschneider.addressbook.utils.Urls
import com.deepschneider.addressbook.utils.Utils
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import org.json.JSONObject
import java.nio.charset.Charset
import java.security.KeyStore
import java.util.*
import java.util.concurrent.Executor
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var requestQueue: RequestQueue
    private lateinit var executor: Executor
    private val requestTag = "LOGIN_TAG"

    override fun onCreate(savedInstanceState: Bundle?) {
        if (!resources.configuration.isNightModeActive)
            setTheme(R.style.Theme_Addressbook_Light_NoActionBar)
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        if (!resources.configuration.isNightModeActive)
            supportActionBar?.setBackgroundDrawable(ColorDrawable(Color.parseColor("#FFFFFF")))
        supportActionBar?.elevation = 0F
        title = null
        requestQueue = Volley.newRequestQueue(this)
        executor = ContextCompat.getMainExecutor(this)
        binding.loginButton.setOnClickListener {
            createOrRotateLoginToken(true, ::saveBiometrics)
        }
        Constants.PAGE_SIZE = (((resources.displayMetrics.run { heightPixels / density } - 50) / 90)).toInt()
        if (resources.configuration.isNightModeActive) {
            enableDarkIcon()
            Constants.ACTIVE_LOGIN_COMPONENT = ".activities.LoginActivity"
        } else {
            enableLightIcon()
            Constants.ACTIVE_LOGIN_COMPONENT = ".activities.LoginActivityAlias"
        }
        if (isBiometricSupported() && Utils.getBiometrics(this) != null) {
            binding.biometricLogin.visibility = View.VISIBLE
            binding.biometricLogin.setOnClickListener {
                getBiometrics()
            }
        } else {
            binding.biometricLogin.visibility = View.GONE
        }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                currentFocus?.clearFocus() ?: run {
                    finish()
                }
            }
        })
    }

    private fun isBiometricSupported(): Boolean {
        return when (BiometricManager.from(this).canAuthenticate(BIOMETRIC_STRONG)) {
            BiometricManager.BIOMETRIC_SUCCESS -> true
            else -> false
        }
    }

    private fun enableDarkIcon() {
        packageManager.setComponentEnabledSetting(
            ComponentName(
                this@LoginActivity,
                "com.deepschneider.addressbook.activities.LoginActivity"
            ),
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP
        )

        packageManager.setComponentEnabledSetting(
            ComponentName(
                this@LoginActivity,
                "com.deepschneider.addressbook.activities.LoginActivityAlias"
            ),
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP
        )
    }

    private fun enableLightIcon() {
        packageManager.setComponentEnabledSetting(
            ComponentName(
                this@LoginActivity,
                "com.deepschneider.addressbook.activities.LoginActivity"
            ),
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP
        )
        packageManager.setComponentEnabledSetting(
            ComponentName(
                this@LoginActivity,
                "com.deepschneider.addressbook.activities.LoginActivityAlias"
            ),
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP
        )
    }

    override fun onResume() {
        super.onResume()
        createOrRotateLoginToken(false, ::startOrganizationActivity)
    }

    override fun onStop() {
        super.onStop()
        requestQueue.cancelAll(requestTag)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    @SuppressLint("ApplySharedPref")
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                startActivity(Intent(applicationContext, SettingsActivity::class.java))
                return true
            }
            R.id.action_about_main -> {
                val binding = DialogAppInfoBinding.inflate(layoutInflater)
                binding.developer.setText(R.string.app_info_author)
                binding.version.setText(BuildConfig.VERSION_NAME)
                MaterialAlertDialogBuilder(this@LoginActivity).setView(binding.root)
                    .setCancelable(false).setPositiveButton(
                        android.R.string.ok
                    ) { dialog, _ ->
                        dialog.cancel()
                    }.create().show()
                return true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun createOrRotateLoginToken(create: Boolean, onSuccess: () -> (Unit)) {
        hideLoginButton()
        val serverUrl = NetworkUtils.getServerUrl(this)
        if (serverUrl == Constants.NO_VALUE) {
            if (create)
                makeSnackBar(this.getString(R.string.server_url_empty_error))
            showLoginButton()
            return
        }
        val targetDto = if (create) getLoginDto() else getTokenDto()
        if (!create && targetDto == null) {
            showLoginButton()
            return
        }
        requestQueue.add(JsonObjectRequest(Request.Method.POST,
            if (create) serverUrl + Urls.AUTH else serverUrl + Urls.ROTATE_TOKEN,
            targetDto,
            { response ->
                saveTokenFromResponse(response)
                showLoginButton()
                onSuccess()
            },
            { error ->
                makeErrorSnackBar(error)
                showLoginButton()
            }).also { it.tag = requestTag })
    }

    private fun generateSecretKey() {
        val keyStore = KeyStore.getInstance(Constants.KEYSTORE)
        keyStore.load(null)
        if (!keyStore.containsAlias(Constants.KEY_ALIAS)) {
            val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, Constants.KEYSTORE)
            keyGenerator.init(
                KeyGenParameterSpec.Builder(
                    Constants.KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
                    .setUserAuthenticationRequired(true)
                    .setInvalidatedByBiometricEnrollment(true)
                    .build()
            )
            keyGenerator.generateKey()
        }
    }

    private fun getSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(Constants.KEYSTORE)
        keyStore.load(null)
        return keyStore.getKey(Constants.KEY_ALIAS, null) as SecretKey
    }

    private fun getCipher(): Cipher {
        return Cipher.getInstance(
            KeyProperties.KEY_ALGORITHM_AES + "/"
                    + KeyProperties.BLOCK_MODE_CBC + "/"
                    + KeyProperties.ENCRYPTION_PADDING_PKCS7
        )
    }

    private fun getBiometrics() {
        val biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    makeSnackBar(errString as String)
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    val decryptedInfo = result.cryptoObject?.cipher?.doFinal(
                        Base64.decode(
                            Utils.getBiometrics(this@LoginActivity),
                            Base64.NO_WRAP
                        )
                    )
                    if (decryptedInfo != null) {
                        hideLoginButton()
                        val loginAndPass = String(decryptedInfo)
                        val serverUrl = NetworkUtils.getServerUrl(this@LoginActivity)
                        if (serverUrl == Constants.NO_VALUE) {
                            showLoginButton()
                            return
                        }
                        val loginDto = JSONObject()
                        loginDto.put("login", loginAndPass.split("&!#&")[0])
                        loginDto.put("password", loginAndPass.split("&!#&")[1])
                        requestQueue.add(JsonObjectRequest(Request.Method.POST,
                            serverUrl + Urls.AUTH,
                            loginDto,
                            { response ->
                                saveTokenFromResponse(response)
                                showLoginButton()
                                startOrganizationActivity()
                            },
                            { error ->
                                makeErrorSnackBar(error)
                                showLoginButton()
                            }).also { it.tag = requestTag })
                    }
                }

                override fun onAuthenticationFailed() {
                    makeSnackBar(this@LoginActivity.getString(R.string.biometric_authentification_failed_message))
                }
            })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(this.getString(R.string.biometric_login_title))
            .setSubtitle(this.getString(R.string.biometric_login_message))
            .setNegativeButtonText(this.getString(R.string.regular_login_message))
            .build()

        val keyIV = PreferenceManager.getDefaultSharedPreferences(this@LoginActivity)
            .getString(SHARED_PREFERENCE_KEY_IV, "")

        val cipher = getCipher()
        val secretKey = getSecretKey()
        try {
            cipher.init(
                Cipher.DECRYPT_MODE,
                secretKey,
                IvParameterSpec(Base64.decode(keyIV, Base64.NO_WRAP))
            )
        } catch (e: KeyPermanentlyInvalidatedException) {
            hideLoginButton()
            val keyStore = KeyStore.getInstance(Constants.KEYSTORE)
            keyStore.load(null)
            keyStore.deleteEntry(Constants.KEY_ALIAS)
            PreferenceManager.getDefaultSharedPreferences(this@LoginActivity)
                .edit()
                .remove(Constants.BIOMETRICS)
                .apply()
            showLoginButton()
            makeSnackBar(this@LoginActivity.getString(R.string.biometric_authentification_failed_message))
            return
        }

        biometricPrompt.authenticate(
            promptInfo,
            BiometricPrompt.CryptoObject(cipher)
        )
    }

    private fun saveBiometrics() {
        if (!isBiometricSupported()) {
            startOrganizationActivity()
        } else {
            generateSecretKey()
            val biometricPrompt = BiometricPrompt(this, executor,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        if (errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                            PreferenceManager.getDefaultSharedPreferences(this@LoginActivity)
                                .edit()
                                .remove(Constants.BIOMETRICS)
                                .apply()
                            startOrganizationActivity()
                        } else {
                            makeSnackBar(errString as String)
                        }
                    }

                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        super.onAuthenticationSucceeded(result)
                        val loginText = binding.editTextLogin.text.toString()
                        val passwordText = binding.editTextPassword.text.toString()
                        val encryptedInfo: ByteArray? = result.cryptoObject?.cipher?.doFinal(
                            ("$loginText&!#&$passwordText").toByteArray(Charset.defaultCharset())
                        )
                        encryptedInfo?.let {
                            Utils.saveBiometrics(this@LoginActivity, encryptedInfo)
                            startOrganizationActivity()
                        }
                    }

                    override fun onAuthenticationFailed() {
                        makeSnackBar(this@LoginActivity.getString(R.string.biometric_authentification_failed_message))
                    }
                })

            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle(this.getString(R.string.biometric_login_title_question))
                .setSubtitle(this.getString(R.string.biometric_login_message))
                .setNegativeButtonText(this.getString(R.string.regular_login_message))
                .build()

            val cipher = getCipher()
            var secretKey = getSecretKey()
            try {
                cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            } catch (e: KeyPermanentlyInvalidatedException) {
                val keyStore = KeyStore.getInstance(Constants.KEYSTORE)
                keyStore.load(null)
                keyStore.deleteEntry(Constants.KEY_ALIAS)
                generateSecretKey()
                secretKey = getSecretKey()
                cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            }

            PreferenceManager.getDefaultSharedPreferences(this@LoginActivity)
                .edit()
                .putString(
                    SHARED_PREFERENCE_KEY_IV,
                    Base64.encodeToString(cipher.iv, Base64.NO_WRAP)
                ).apply()

            biometricPrompt.authenticate(
                promptInfo,
                BiometricPrompt.CryptoObject(cipher)
            )
        }
    }

    private fun makeErrorSnackBar(error: VolleyError) {
        when (error) {
            is AuthFailureError -> makeSnackBar(this.getString(R.string.auth_failure_message))
            is TimeoutError -> makeSnackBar(this.getString(R.string.server_timeout_message))
            else -> makeSnackBar(error.message.toString())
        }
    }

    private fun makeSnackBar(message: String) {
        val snackBar = Snackbar.make(binding.coordinatorLayout, message, Snackbar.LENGTH_LONG)
        val view: View = snackBar.view
        view.findViewById<TextView>(com.google.android.material.R.id.snackbar_text).maxLines = 10
        val params = view.layoutParams as CoordinatorLayout.LayoutParams
        params.gravity = Gravity.TOP
        view.layoutParams = params
        snackBar.show()
    }

    private fun showLoginButton() {
        binding.loginButton.visibility = View.VISIBLE
        binding.progressBar.visibility = ProgressBar.INVISIBLE
        if (isBiometricSupported() && Utils.getBiometrics(this) != null) {
            binding.biometricLogin.visibility = View.VISIBLE
        }
    }

    private fun hideLoginButton() {
        binding.loginButton.visibility = View.GONE
        binding.biometricLogin.visibility = View.GONE
        binding.progressBar.visibility = ProgressBar.VISIBLE
    }

    private fun startOrganizationActivity() {
        val intent = Intent(applicationContext, OrganizationsActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }

    @SuppressLint("ApplySharedPref")
    private fun saveTokenFromResponse(response: JSONObject) {
        PreferenceManager.getDefaultSharedPreferences(this).edit().putString(
            Constants.TOKEN_KEY, response.get(Constants.TOKEN_KEY) as String?
        ).commit()
    }

    private fun getLoginDto(): JSONObject {
        val loginDto = JSONObject()
        loginDto.put("login", binding.editTextLogin.text)
        loginDto.put("password", binding.editTextPassword.text)
        return loginDto
    }

    private fun getTokenDto(): JSONObject? {
        val token = PreferenceManager.getDefaultSharedPreferences(this)
            .getString(Constants.TOKEN_KEY, Constants.NO_VALUE)
        if (token == null || token == Constants.NO_VALUE) return null
        val tokenDto = JSONObject()
        tokenDto.put(Constants.TOKEN_KEY, token)
        return tokenDto
    }
}
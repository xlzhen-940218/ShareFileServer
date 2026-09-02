package com.xlzhen.sharefileserver

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.provider.Settings
import android.view.Menu
import android.view.MenuItem
import android.webkit.JsResult
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.EditText
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.SystemBarStyle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.xlzhen.sharefileserver.service.ServerRunService
import com.xlzhen.sharefileserver.utils.DatabaseHelper
import com.xlzhen.sharefileserver.utils.ShareFileToMeUtils

class MainActivity : AppCompatActivity() {
    private lateinit var webView: WebView
    private var shareFiles: List<String>? = null

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK && requestCode == 1000) {
            var files: Array<String>? = null
            data?.let { intentData ->
                val uri = intentData.data
                if (uri != null) {
                    val parsed = ShareFileToMeUtils.parseFileUri(this, uri)
                    if (parsed != null) files = arrayOf(parsed)
                } else {
                    val clipData = intentData.clipData
                    if (clipData != null && clipData.itemCount > 0) {
                        files = Array(clipData.itemCount) { i ->
                            ShareFileToMeUtils.parseFileUri(this, clipData.getItemAt(i).uri) ?: ""
                        }.filter { it.isNotEmpty() }.toTypedArray()
                    }
                }
            }
            
            if (!files.isNullOrEmpty()) {
                Application.shareFiles = files
                webView.loadUrl("${ServerRunService.host}/${Application.registerShareFile}")
            }
        }
    }

    override fun onResume() {
        super.onResume()
        checkNetworkConnectivity()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // EdgeToEdge.enable(this, SystemBarStyle.dark(android.graphics.Color.TRANSPARENT))
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.root_layout)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            toolbar.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            findViewById<WebView>(R.id.webView).setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom)
            insets
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (webView.canGoBack()) {
                    webView.goBack()
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })

        checkAndRequestPermissions()

        ServerRunService.start(this)
        webView = findViewById(R.id.webView)
        webView.settings.javaScriptEnabled = true
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                view.loadUrl(request.url.toString())
                return true
            }
        }
        webView.webChromeClient = object : WebChromeClient() {
            override fun onJsAlert(view: WebView?, url: String?, message: String?, result: JsResult?): Boolean {
                return super.onJsAlert(view, url, message, result)
            }
        }
        shareFiles = ShareFileToMeUtils.getShareFiles(this)

        Thread {
            try {
                Thread.sleep(2000)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
            runOnUiThread {
                val files = shareFiles
                if (files != null && files.isNotEmpty()) {
                    Application.shareFiles = files.toTypedArray()
                    webView.loadUrl("${ServerRunService.host}/${Application.registerShareFile}")
                } else if (Application.clipDataNote.isNotEmpty()) {
                    webView.loadUrl("${ServerRunService.host}/sharednote.html")
                } else {
                    webView.loadUrl(ServerRunService.host?: "")
                }
            }
        }.start()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.add_file) {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "*/*"
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            }
            try {
                startActivityForResult(Intent.createChooser(intent, getString(R.string.please_select_files)), 1000)
            } catch (e: ActivityNotFoundException) {
                e.printStackTrace()
                Toast.makeText(this, R.string.please_install_file_explorer, Toast.LENGTH_SHORT).show()
            }
            return true
        } else if (item.itemId == R.id.set_password) {
            val dbHelper = DatabaseHelper(this)
            
            val container = android.widget.FrameLayout(this)
            val params = android.widget.FrameLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(60, 40, 60, 0)
            
            val textInputLayout = com.google.android.material.textfield.TextInputLayout(this)
            val input = com.google.android.material.textfield.TextInputEditText(this)
            input.setText(dbHelper.getSetting("password") ?: "")
            input.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            textInputLayout.hint = getString(R.string.password_hint)
            textInputLayout.addView(input)
            
            container.addView(textInputLayout, params)
            
            com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.set_password))
                .setView(container)
                .setPositiveButton(getString(R.string.save)) { _, _ ->
                    dbHelper.setSetting("password", input.text.toString())
                    Toast.makeText(this, getString(R.string.password_saved), Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton(getString(R.string.cancel), null)
                .show()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
                checkManageExternalStoragePermission()
            }
        }
    }

    private fun checkAndRequestPermissions() {
        val needStorage = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            !Environment.isExternalStorageManager()
        } else {
            ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE) != android.content.pm.PackageManager.PERMISSION_GRANTED
        }
        val needLocation = ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != android.content.pm.PackageManager.PERMISSION_GRANTED

        if (needStorage || needLocation) {
            AlertDialog.Builder(this)
                .setTitle(R.string.permission_dialog_title)
                .setMessage(R.string.permission_dialog_message)
                .setCancelable(false)
                .setPositiveButton(R.string.permission_dialog_positive) { _, _ ->
                    val permissionsToRequest = mutableListOf<String>()
                    if (needLocation) {
                        permissionsToRequest.add(android.Manifest.permission.ACCESS_FINE_LOCATION)
                    }
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R && needStorage) {
                        permissionsToRequest.add(android.Manifest.permission.READ_EXTERNAL_STORAGE)
                    }

                    if (permissionsToRequest.isNotEmpty()) {
                        ActivityCompat.requestPermissions(
                            this,
                            permissionsToRequest.toTypedArray(),
                            1
                        )
                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && needStorage) {
                        checkManageExternalStoragePermission()
                    }
                }
                .setNegativeButton(R.string.permission_dialog_negative) { _, _ ->
                    finish()
                }
                .show()
        }
    }

    private fun checkManageExternalStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.data = Uri.parse("package:$packageName")
                startActivityForResult(intent, 10001)
            } catch (e: Exception) {
                val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                startActivityForResult(intent, 10001)
            }
        }
    }

    private fun checkNetworkConnectivity() {
        val connectivityManager = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            connectivityManager.activeNetwork
        } else {
            null
        }
        
        val hasWiFi = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val capabilities = connectivityManager.getNetworkCapabilities(network)
            capabilities != null && capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
        } else {
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo
            @Suppress("DEPRECATION")
            networkInfo != null && networkInfo.type == ConnectivityManager.TYPE_WIFI
        }
        
        if (hasWiFi) return

        val builder = com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
        builder.setTitle(getString(R.string.network_warning))
        builder.setMessage(getString(R.string.no_wifi_msg))
        
        builder.setPositiveButton(getString(R.string.connect_wifi)) { _, _ ->
            startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
        }
        
        builder.setNegativeButton(getString(R.string.turn_on_hotspot)) { _, _ ->
            val intent = Intent()
            intent.component = ComponentName("com.android.settings", "com.android.settings.TetherSettings")
            try {
                startActivity(intent)
            } catch (e: Exception) {
                startActivity(Intent(Settings.ACTION_WIRELESS_SETTINGS))
            }
        }
        
        builder.setNeutralButton(getString(R.string.ignore)) { dialog, _ ->
            dialog.dismiss()
        }
        
        builder.show()
    }
}

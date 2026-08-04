package com.xlzhen.sharefileserver

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.view.Menu
import android.view.MenuItem
import android.webkit.JsResult
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
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
        if (item.itemId != R.id.add_file) {
            return super.onOptionsItemSelected(item)
        }
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
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1) {
            val allGranted = grantResults.all { it == android.content.pm.PackageManager.PERMISSION_GRANTED }
            if (allGranted) {
                checkManageExternalStoragePermission()
            }
        }
    }

    private fun checkAndRequestPermissions() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE) != android.content.pm.PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            AlertDialog.Builder(this)
                .setTitle(R.string.permission_dialog_title)
                .setMessage(R.string.permission_dialog_message)
                .setCancelable(false)
                .setPositiveButton(R.string.permission_dialog_positive) { _, _ ->
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(
                            android.Manifest.permission.READ_EXTERNAL_STORAGE,
                            android.Manifest.permission.ACCESS_FINE_LOCATION
                        ),
                        1
                    )
                }
                .setNegativeButton(R.string.permission_dialog_negative) { _, _ ->
                    finish()
                }
                .show()
        } else {
            checkManageExternalStoragePermission()
        }
    }

    private fun checkManageExternalStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
            val intent = Intent(android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
            intent.data = Uri.parse("package:$packageName")
            startActivityForResult(intent, 10001)
        }
    }
}

package it.gmmz.llamandroid

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import de.kherud.llama.LlamaModel
import de.kherud.llama.LogLevel
import de.kherud.llama.args.LogFormat
import it.gmmz.llamandroid.ui.DownloadsView
import it.gmmz.llamandroid.ui.components.ChatScreen
import it.gmmz.llamandroid.ui.components.ModelSelection
import it.gmmz.llamandroid.ui.theme.LLaMAndroid
import it.gmmz.llamandroid.vm.ChatViewModel
import it.gmmz.llamandroid.vm.DownloaderViewModel
import java.io.File
import kotlin.properties.Delegates

@SuppressLint("UnsafeDynamicallyLoadedCode")
class MainActivity : ComponentActivity() {
    var isOpenCL by Delegates.notNull<Boolean>()

    init {
        // Load OpenCL; this is required for GPU acceleration
        val tryPaths = listOf(
            "/system/vendor/lib64/libOpenCL.so",
            "/system/lib/libOpenCL.so",
        )

        isOpenCL = tryPaths.any {
            try {
                System.load(it)
                true
            } catch (e: UnsatisfiedLinkError) {
                Log.w("MainActivity", "OpenCL not available in $it: ${e.message}")
                false
            }
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Log.d("MainActivity", "Notification permission granted")
        } else {
            Log.d("MainActivity", "Notification permission denied")
        }
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    // Permission already granted
                    Log.d("MainActivity", "Notification permission already granted")
                }

                else -> {
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
    }

    private fun checkStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.data = "package:$packageName".toUri()
                startActivity(intent)
            }
        } else {
            val permissions = arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )

            requestPermissions(permissions, 1001)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        checkStoragePermission()
        checkNotificationPermission()

        LlamaModel.setLogger(LogFormat.TEXT) { level, message ->
            when (level) {
                LogLevel.DEBUG -> Log.d("LlamaModel", message)
                LogLevel.INFO -> Log.i("LlamaModel", message)
                LogLevel.WARN -> Log.w("LlamaModel", message)
                LogLevel.ERROR -> Log.e("LlamaModel", message)
                else -> Log.v("LlamaModel", message)
            }
        }

        setContent {
            LLaMAndroid {
                val downloaderVM: DownloaderViewModel = viewModel()
                val chatVM: ChatViewModel = viewModel()

                val downloads by downloaderVM.downloads.collectAsState()
                val selectedModel by chatVM.selectedModel.collectAsState()
                val messages by chatVM.messages.collectAsState()
                val currentGeneratingMessage by chatVM.currentGeneratingMessage.collectAsState()
                val loadingModel by chatVM.loadingModel.collectAsState()
                val isGenerating by chatVM.isGenerating.collectAsState()
                val currentTokensPerSecond by chatVM.currentGeneratingTokensPerSecond.collectAsState()

                LaunchedEffect(Unit) {
                    downloaderVM.initialize(this@MainActivity)
                    chatVM.loadModel(this@MainActivity)
                }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            // Model selection UI
                            ModelSelection(
                                selectedModel = selectedModel,
                                onModelSelected = { chatVM.selectModel(it, this@MainActivity) },
                                onDownloadModel = { model ->
                                    val downloadDir = modelsDir()
                                    if (!downloadDir.exists()) downloadDir.mkdirs()

                                    downloaderVM.addDownload(model.url, model.path(this@MainActivity).absolutePath)
                                },
                                context = this@MainActivity
                            )

                            // Downloads
                            DownloadsView(
                                downloads = downloads,
                                onPause = { downloaderVM.pauseDownload(it) },
                                onResume = { downloaderVM.resumeDownload(it) },
                                onCancel = { downloaderVM.cancelDownload(it) },
                                onRetry = { downloaderVM.retry(it) },
                                onRemove = { downloaderVM.removeDownload(it) }
                            )

                            // Chat UI
                            ChatScreen(
                                messages = messages,
                                currentGeneratingMessage = currentGeneratingMessage,
                                onSendMessage = { chatVM.sendMessage(it) },
                                isLoading = loadingModel,
                                isGenerating = isGenerating,
                                currentTokensPerSecond = currentTokensPerSecond
                            )
                        }
                    }
                }
            }
        }
    }
}

fun Context.modelsDir() = File(Environment.getExternalStorageDirectory(), "LLaMAndroid").apply {
    if (!exists()) {
        mkdirs()
    }
}
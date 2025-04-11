package it.gmmz.llamandroid.vm

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tonyodev.fetch2.DefaultFetchNotificationManager
import com.tonyodev.fetch2.Download
import com.tonyodev.fetch2.Error
import com.tonyodev.fetch2.Fetch
import com.tonyodev.fetch2.FetchConfiguration
import com.tonyodev.fetch2.FetchListener
import com.tonyodev.fetch2.NetworkType
import com.tonyodev.fetch2.Priority
import com.tonyodev.fetch2.Request
import com.tonyodev.fetch2.Status
import com.tonyodev.fetch2core.DownloadBlock
import com.tonyodev.fetch2core.Downloader
import com.tonyodev.fetch2okhttp.OkHttpDownloader
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

data class DownloadItem(
    val id: Int,
    val url: String,
    val fileName: String,
    val status: Status = Status.NONE,
    val speed: Long = 0,
    val eta: Long = 0,
    val progress: Int = 0,
    val downloadedBytes: Long = 0,
    val totalBytes: Long = 0,
    val error: Error? = null,
    val errorMessage: String? = null,
)

class DownloaderViewModel : ViewModel() {
    private lateinit var fetch: Fetch

    private val _downloads = MutableStateFlow<Map<Int, DownloadItem>>(emptyMap())
    val downloads: StateFlow<Map<Int, DownloadItem>> = _downloads.asStateFlow()

    fun initialize(context: Context) {
        val fetchConfiguration = FetchConfiguration.Builder(context)
            .setDownloadConcurrentLimit(10)
            .setAutoRetryMaxAttempts(3)
            .enableLogging(true)
            .enableAutoStart(true)
            .enableRetryOnNetworkGain(true)
            .setHttpDownloader(OkHttpDownloader(Downloader.FileDownloaderType.PARALLEL))
            .setNotificationManager(object : DefaultFetchNotificationManager(context) {
                override fun getFetchInstanceForNamespace(namespace: String) = fetch
            })
            .build()

        fetch = Fetch.getInstance(fetchConfiguration)
        fetch.addListener(createFetchListener())
    }

    fun addDownload(url: String, filePath: String) {
        viewModelScope.launch {
            val file = File(filePath)
            val fileName = file.name

            val request = Request(url, filePath)
            request.priority = Priority.NORMAL
            request.networkType = NetworkType.ALL

            fetch.enqueue(request, func = { updatedRequest ->
                val downloadItem = DownloadItem(
                    id = updatedRequest.id,
                    url = url,
                    fileName = fileName
                )

                _downloads.value = _downloads.value.toMutableMap().apply {
                    put(updatedRequest.id, downloadItem)
                }
            })
        }
    }

    fun pauseDownload(id: Int) {
        fetch.pause(id)
    }

    fun resumeDownload(id: Int) {
        fetch.resume(id)
    }

    fun cancelDownload(id: Int) {
        fetch.cancel(id)
    }

    fun retry(id: Int) {
        fetch.retry(id)
    }

    fun removeDownload(id: Int) {
        fetch.remove(id)
        _downloads.value = _downloads.value.toMutableMap().apply {
            remove(id)
        }
    }

    fun removeAllDownloads() {
        fetch.removeAll()
        _downloads.value = emptyMap()
    }

    private fun createFetchListener(): FetchListener {
        return object : FetchListener {
            override fun onAdded(download: Download) {
                updateDownloadItem(download)
            }

            override fun onCancelled(download: Download) {
                updateDownloadItem(download)
            }

            override fun onCompleted(download: Download) {
                updateDownloadItem(download)
            }

            override fun onDeleted(download: Download) {
                // No action needed - we handle removal in removeDownload function
            }

            override fun onError(download: Download, error: Error, throwable: Throwable?) {
                updateDownloadItem(download, error)
            }

            override fun onDownloadBlockUpdated(
                download: Download,
                downloadBlock: DownloadBlock,
                totalBlocks: Int,
            ) {

            }

            override fun onStarted(
                download: Download,
                downloadBlocks: List<DownloadBlock>,
                totalBlocks: Int,
            ) {
                updateDownloadItem(download)
            }

            override fun onPaused(download: Download) {
                updateDownloadItem(download)
            }

            override fun onProgress(
                download: Download,
                etaInMilliSeconds: Long,
                downloadedBytesPerSecond: Long,
            ) {
                updateDownloadItem(download, null, etaInMilliSeconds, downloadedBytesPerSecond)
            }

            override fun onQueued(download: Download, waitingOnNetwork: Boolean) {
                updateDownloadItem(download)
            }

            override fun onRemoved(download: Download) {
                // No action needed - we handle removal in removeDownload function
            }

            override fun onResumed(download: Download) {
                updateDownloadItem(download)
            }

            override fun onWaitingNetwork(download: Download) {
                updateDownloadItem(download)
            }
        }
    }

    private fun updateDownloadItem(
        download: Download,
        error: Error? = null,
        etaInMillis: Long = 0,
        speedBytesPerSecond: Long = 0,
    ) {
        val existingItem = _downloads.value[download.id]
        if (existingItem != null) {
            val updatedItem = existingItem.copy(
                status = download.status,
                progress = download.progress,
                downloadedBytes = download.downloaded,
                totalBytes = download.total,
                error = error ?: download.error,
                errorMessage = error?.name ?: download.error.name,
                eta = etaInMillis / 1000, // Convert milliseconds to seconds
                speed = speedBytesPerSecond
            )

            _downloads.value = _downloads.value.toMutableMap().apply {
                put(download.id, updatedItem)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        if (::fetch.isInitialized) {
            fetch.close()
        }
    }
}
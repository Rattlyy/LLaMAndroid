package it.gmmz.llamandroid.ui

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tonyodev.fetch2.Status
import it.gmmz.llamandroid.vm.DownloadItem

@Composable
fun DownloadsView(
    downloads: Map<Int, DownloadItem>,
    onPause: (Int) -> Unit,
    onResume: (Int) -> Unit,
    onCancel: (Int) -> Unit,
    onRetry: (Int) -> Unit,
    onRemove: (Int) -> Unit,
) {
    if (downloads.isEmpty()) return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = "Downloads",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        downloads.values.forEach { download ->
            DownloadListItem(
                downloadItem = download,
                onPause = onPause,
                onResume = onResume,
                onCancel = onCancel,
                onRetry = onRetry,
                onRemove = onRemove
            )
        }
    }
}

@Composable
fun DownloadListItem(
    downloadItem: DownloadItem,
    onPause: (Int) -> Unit,
    onResume: (Int) -> Unit,
    onCancel: (Int) -> Unit,
    onRetry: (Int) -> Unit,
    onRemove: (Int) -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = downloadItem.fileName,
                    style = MaterialTheme.typography.bodyLarge
                )

                Text(
                    text = "${downloadItem.progress}%",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = downloadItem.progress / 100f,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Speed: ${
                    when {
                        downloadItem.speed >= 1024 * 1024 * 1024 -> String.format(
                            "%.2f GB/s",
                            downloadItem.speed / (1024f * 1024f)
                        )

                        downloadItem.speed >= 1024 * 1024 -> String.format(
                            "%.2f MB/s",
                            downloadItem.speed / 1024f
                        )

                        else -> "${downloadItem.speed} KB/s"
                    }
                }",
                style = MaterialTheme.typography.bodySmall
            )

            val minutes = downloadItem.eta / 1000 / 60
            val seconds = (downloadItem.eta / 1000) % 60

            Text(
                text = "Downloaded: ${formatBytes(downloadItem.downloadedBytes)} / ${
                    formatBytes(
                        downloadItem.totalBytes
                    )
                }",
                style = MaterialTheme.typography.bodySmall
            )

            Text(
                text = "ETA: ${
                    if (minutes > 0) "${minutes}m ${seconds}s" else "${seconds}s"
                }",
                style = MaterialTheme.typography.bodySmall
            )

            if (!downloadItem.errorMessage.isNullOrEmpty() && downloadItem.errorMessage != "NONE") {
                Text(
                    text = "Error: ${downloadItem.errorMessage}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                when (downloadItem.status) {
                    Status.FAILED -> {
                        Button(
                            onClick = { onRetry(downloadItem.id) },
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text("Retry")
                        }

                        OutlinedButton(
                            onClick = { onRemove(downloadItem.id) }
                        ) {
                            Text("Remove")
                        }
                    }

                    Status.DOWNLOADING -> {
                        Button(
                            onClick = { onPause(downloadItem.id) },
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text("Pause")
                        }

                        OutlinedButton(
                            onClick = { onCancel(downloadItem.id) }
                        ) {
                            Text("Cancel")
                        }
                    }

                    Status.PAUSED -> {
                        Button(
                            onClick = { onResume(downloadItem.id) },
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text("Resume")
                        }

                        OutlinedButton(
                            onClick = { onCancel(downloadItem.id) }
                        ) {
                            Text("Cancel")
                        }
                    }

                    Status.COMPLETED -> {
                        OutlinedButton(
                            onClick = { onRemove(downloadItem.id) }
                        ) {
                            Text("Remove")
                        }
                    }

                    Status.CANCELLED, Status.REMOVED -> {
                        Button(
                            onClick = { onRetry(downloadItem.id) },
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text("Retry")
                        }

                        OutlinedButton(
                            onClick = { onRemove(downloadItem.id) }
                        ) {
                            Text("Remove")
                        }
                    }

                    Status.QUEUED, Status.NONE, Status.ADDED -> {
                        OutlinedButton(
                            onClick = { onCancel(downloadItem.id) }
                        ) {
                            Text("Cancel")
                        }
                    }

                    Status.DELETED -> {
                        OutlinedButton(
                            onClick = { onRemove(downloadItem.id) }
                        ) {
                            Text("Remove")
                        }
                    }
                }
            }
        }
    }
}

@SuppressLint("DefaultLocale")
private fun formatBytes(bytes: Long): String {
    return when {
        bytes >= 1024 * 1024 * 1024 -> String.format(
            "%.2f GB",
            bytes / (1024.0 * 1024.0 * 1024.0)
        )

        bytes >= 1024 * 1024 -> String.format("%.2f MB", bytes / (1024.0 * 1024.0))
        bytes >= 1024 -> String.format("%.2f KB", bytes / 1024.0)
        else -> "$bytes bytes"
    }
}
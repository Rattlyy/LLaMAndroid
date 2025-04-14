package it.gmmz.llamandroid.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import it.gmmz.llamandroid.Model

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ModelSelection(
    selectedModel: Model?,
    availableModels: List<Model>,
    onModelSelected: (Model) -> Unit,
    onDownloadModel: (Model) -> Unit,
    context: android.content.Context,
) {
    FlowRow(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        availableModels.forEach { model ->
            val modelFile = model.path(context)
            val modelAvailable = modelFile.exists()

            if (modelAvailable) {
                FilterChip(
                    selected = selectedModel == model,
                    onClick = { onModelSelected(model) },
                    label = { Text(model.name) }
                )
            } else {
                AssistChip(
                    onClick = { onDownloadModel(model) },
                    label = { Text(model.name) },
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "Download model"
                        )
                    }
                )
            }
        }
    }
}
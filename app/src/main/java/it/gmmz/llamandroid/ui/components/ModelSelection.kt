package it.gmmz.llamandroid.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import it.gmmz.llamandroid.Model
import it.gmmz.llamandroid.modelsDir

@Composable
fun ModelSelection(
    selectedModel: Model?,
    availableModels: List<Model>,
    onModelSelected: (Model) -> Unit,
    onDownloadModel: (Model) -> Unit,
    context: android.content.Context,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        availableModels.forEach { model ->
            val modelFile = context.modelsDir().resolve(model.path(context))
            val modelAvailable = modelFile.exists()

            if (modelAvailable) {
                FilterChip(
                    selected = selectedModel == model,
                    onClick = { onModelSelected(model) },
                    label = { Text(model.name) },
                    modifier = Modifier.padding(end = 8.dp)
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
                    },
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
        }
    }
}
package it.gmmz.llamandroid.ui

import android.app.Activity
import android.content.Context
import android.graphics.Canvas
import android.view.inputmethod.InputMethodManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.core.graphics.createBitmap
import dev.jeziellago.compose.markdowntext.MarkdownText
import it.gmmz.llamandroid.vm.ChatMessage
import ru.noties.jlatexmath.JLatexMathDrawable

@Composable
fun ChatScreen(
    messages: List<ChatMessage>,
    currentGeneratingMessage: String,
    currentTokensPerSecond: Float,
    onSendMessage: (String) -> Unit,
    isLoading: Boolean,
    isGenerating: Boolean,
) {
    val listState = rememberLazyListState()
    var inputText by remember { mutableStateOf("") }

    LaunchedEffect(messages.size, currentGeneratingMessage) {
        if (messages.isNotEmpty() || currentGeneratingMessage.isNotEmpty()) {
            listState.animateScrollToItem(messages.size)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(messages) { message ->
                ChatBubble(message = message)
            }

            if (currentGeneratingMessage.isNotEmpty()) {
                item {
                    ChatBubble(
                        message = ChatMessage(
                            isUser = false,
                            content = currentGeneratingMessage,
                            isComplete = false,
                            tokensPerSecond = currentTokensPerSecond,
                        )
                    )
                }
            } else if (isGenerating) {
                item {
                    ChatBubble(
                        message = ChatMessage(
                            isUser = false,
                            content = "Running inference...",
                            isComplete = false
                        )
                    )
                }
            }
        }

        ChatInput(
            value = inputText,
            onValueChange = { inputText = it },
            onSend = {
                if (inputText.isNotBlank()) {
                    onSendMessage(inputText)
                    inputText = ""
                }
            },
            isLoading = isLoading
        )
    }
}

@Composable
fun ChatBubble(message: ChatMessage) {
    val alignment = if (message.isUser) Alignment.End else Alignment.Start
    val backgroundColor = if (message.isUser)
        MaterialTheme.colorScheme.primaryContainer
    else
        MaterialTheme.colorScheme.secondaryContainer
    val textColor = if (message.isUser)
        MaterialTheme.colorScheme.onPrimaryContainer
    else
        MaterialTheme.colorScheme.onSecondaryContainer

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 300.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (message.isUser) 16.dp else 4.dp,
                        bottomEnd = if (message.isUser) 4.dp else 16.dp
                    )
                )
                .background(backgroundColor)
                .padding(12.dp)
        ) {
            val content = message.content
            val latexPattern =
                "\\$\\$(.*?)\\$\\$|\\$(.*?)\\$|\\\\begin\\{(.*?)\\}(.*?)\\\\end\\{\\3\\}|<latex>(.*?)</latex>"
                    .toRegex(RegexOption.DOT_MATCHES_ALL)
            val matches = latexPattern.findAll(content)

            if (matches.any()) {
                // Content contains LaTeX
                var lastEnd = 0
                Column {
                    matches.forEach { matchResult ->
                        val beforeLatex = content.substring(lastEnd, matchResult.range.first)
                        if (beforeLatex.isNotEmpty()) {
                            MarkdownText(
                                markdown = beforeLatex,
                                syntaxHighlightColor = Color.Transparent
                            )
                        }

                        // Extract the LaTeX content from whichever group matched
                        val latexContent =
                            matchResult.groupValues.drop(1).firstOrNull { it.isNotEmpty() } ?: ""

                        var latexError: String? = null
                        val latexDrawable = try {
                            JLatexMathDrawable.builder(latexContent)
                                .textSize(60f)
                                .padding(8)
                                .background(backgroundColor.toArgb())
                                .color(textColor.toArgb())
                                .align(JLatexMathDrawable.ALIGN_RIGHT)
                                .build()
                        } catch (e: Exception) {
                            latexError = e.localizedMessage
                            null
                        }

                        if (latexDrawable != null) {
                            Image(
                                painter = BitmapPainter(
                                    createBitmap(
                                        latexDrawable.intrinsicWidth,
                                        latexDrawable.intrinsicHeight
                                    ).apply {
                                        val canvas = Canvas(this)
                                        latexDrawable.setBounds(0, 0, canvas.width, canvas.height)
                                        latexDrawable.draw(canvas)
                                    }.asImageBitmap()
                                ),
                                contentDescription = "LaTeX formula",
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        } else {
                            Text(
                                text = "LaTeX Error: Unable to parse formula\n\nFormula: $latexContent\nError: $latexError",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        lastEnd = matchResult.range.last + 1
                    }

                    // Render any remaining content after the last LaTeX block
                    if (lastEnd < content.length) {
                        val afterLatex = content.substring(lastEnd)
                        MarkdownText(
                            markdown = afterLatex,
                            syntaxHighlightColor = Color.Transparent,
                        )
                    }
                }
            } else {
                // No LaTeX, just render markdown
                MarkdownText(
                    markdown = content,
                    syntaxHighlightColor = Color.Transparent,
                )
            }
        }

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = "${message.tokensPerSecond} t/s",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(start = 4.dp)
        )
    }
}

@Composable
fun ChatInput(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    isLoading: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val context = LocalContext.current
        val focusManager = LocalFocusManager.current

        fun closeKeyboard() {
            val imm =
                context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            val currentFocusedView = (context as? Activity)?.currentFocus
            currentFocusedView?.let {
                imm.hideSoftInputFromWindow(it.windowToken, 0)
            }

            focusManager.clearFocus()
        }

        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text("Type a message...") },
            modifier = Modifier.weight(1f),
            enabled = !isLoading,
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Send
            ),
            keyboardActions = KeyboardActions(
                onSend = {
                    if (value.isNotBlank()) {
                        onSend()
                        closeKeyboard()
                    }
                }
            )
        )

        Spacer(modifier = Modifier.width(8.dp))

        Button(
            onClick = { onSend(); closeKeyboard() },
            enabled = !isLoading && value.isNotBlank()
        ) {
            Text("Send")
        }
    }
}
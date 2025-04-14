package it.gmmz.llamandroid.vm

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.kherud.llama.LlamaModel
import it.gmmz.llamandroid.Model
import it.gmmz.llamandroid.createModel
import it.gmmz.llamandroid.llama
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import java.net.URL

data class ChatMessage(
    val isUser: Boolean,
    val content: String,
    val isComplete: Boolean = true,
    var tokensPerSecond: Float = 0f,
    var lastTokenTime: Long = 0L,
)

val json = Json {
    ignoreUnknownKeys = true
    isLenient = true
    prettyPrint = true
    encodeDefaults = true
    explicitNulls = false
}

class ChatViewModel : ViewModel() {
    private var model: LlamaModel? = null

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _loadingModel = MutableStateFlow(true)
    val loadingModel: StateFlow<Boolean> = _loadingModel.asStateFlow()

    private val _loadingModels = MutableStateFlow(true)
    val loadingModels: StateFlow<Boolean> = _loadingModels.asStateFlow()

    private val _availableModels = MutableStateFlow<List<Model>>(emptyList())
    val availableModels: StateFlow<List<Model>> = _availableModels.asStateFlow()

    private val _loadModelError: MutableStateFlow<String?> = MutableStateFlow(null)
    val loadModelError: StateFlow<String?> = _loadModelError.asStateFlow()

    private val _selectedModel: MutableStateFlow<Model?> = MutableStateFlow(null)
    val selectedModel: StateFlow<Model?> = _selectedModel.asStateFlow()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _currentGeneratingMessage = MutableStateFlow("")
    val currentGeneratingMessage: StateFlow<String> = _currentGeneratingMessage.asStateFlow()

    private val _currentGeneratingTokensPerSecond = MutableStateFlow(0f)
    val currentGeneratingTokensPerSecond: StateFlow<Float> = _currentGeneratingTokensPerSecond.asStateFlow()

    @OptIn(ExperimentalSerializationApi::class)
    suspend fun fetchModels() {
        _loadingModels.value = true
        try {
            val models = withContext(Dispatchers.IO) {
                try {
                    json.decodeFromStream<List<Model>>(URL("https://raw.githubusercontent.com/Rattlyy/LLaMAndroid/refs/heads/main/models.json").openStream())
                } catch (e: Exception) {
                    Log.e("ChatViewModel", "Error fetching or parsing models", e)
                    _loadModelError.value = "Error fetching or parsing models\n${e.message}"
                    emptyList()
                }
            }

            _availableModels.value = models
        } finally {
            _loadingModels.value = false
        }
    }

    fun selectModel(model: Model, context: Context) {
        _messages.value = emptyList()
        _selectedModel.value = model
        loadModel(context)
    }

    fun loadModel(context: Context) {
        viewModelScope.launch {
            _loadingModel.value = true
            if (selectedModel.value == null) {
                return@launch
            }

            try {
                withContext(Dispatchers.IO) {
                    Log.i("AI", "Loading model ${selectedModel.value!!.name}")
                    model?.close()
                    model = context.createModel(selectedModel.value!!)
                    Log.i("AI", "Model loaded")
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _loadingModel.value = false
            }
        }
    }

    fun sendMessage(userMessage: String) {
        if (userMessage.isBlank() || model == null || loadingModel.value) return

        _messages.value = _messages.value + ChatMessage(isUser = true, content = userMessage)
        _currentGeneratingMessage.value = ""

        viewModelScope.launch {
            try {
                _isGenerating.value = true
                withContext(Dispatchers.IO) {
                    var startTime = System.currentTimeMillis()
                    var tokenCount = 0
                    llama(model!!, selectedModel.value!!, userMessage)
                        .collect { newText ->
                            tokenCount++
                            val currentTime = System.currentTimeMillis()
                            val elapsedSeconds = (currentTime - startTime) / 1000f
                            val tokensPerSecond =
                                if (elapsedSeconds > 0) tokenCount / elapsedSeconds else 0f

                            _currentGeneratingMessage.value += newText

                            // Update the current tokensPerSecond value in the state flow
                            _messages.value.lastOrNull()?.let { lastMessage ->
                                if (lastMessage.isUser) {
                                    _currentGeneratingMessage.value =
                                        _currentGeneratingMessage.value
                                    // We'll store the t/s value to be used when creating the final message
                                    _currentGeneratingTokensPerSecond.value = tokensPerSecond
                                }
                            }
                        }
                }

                val finalMessage = _currentGeneratingMessage.value
                _messages.value =
                    _messages.value + ChatMessage(
                        isUser = false,
                        content = finalMessage,
                        tokensPerSecond = _currentGeneratingTokensPerSecond.value
                    )
                _currentGeneratingMessage.value = ""
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isGenerating.value = false
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        model?.close()
    }
}
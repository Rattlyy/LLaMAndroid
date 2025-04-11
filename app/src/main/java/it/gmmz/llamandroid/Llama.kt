package it.gmmz.llamandroid

import android.content.Context
import android.util.Log
import de.kherud.llama.InferenceParameters
import de.kherud.llama.LlamaModel
import de.kherud.llama.ModelParameters
import de.kherud.llama.args.MiroStat
import kotlinx.coroutines.flow.flow

enum class Models(
    val url: String,
    val temperature: Float,
    val sha256: String,
    val systemPrompt: String,
    val gpuLayers: Int = 35,
) {
    Phi128k(
        "https://huggingface.co/eccheng/Phi-3-mini-128k-instruct-Q4_0-GGUF/resolve/main/phi-3-mini-128k-instruct-q4_0.gguf",
        0.6f,
        "0a268d268c8ef66a9a323ca70f9def918849cdcccb73463d9a63694392e0440f",
        "<s>[INST] <<SYS>>\nYou are a helpful, respectful and honest assistant. Always answer as helpfully as possible, while being safe. Your answers should be informative and logical. If you don't know the answer to a question, please don't share false information. Format your responses using Markdown for better readability.\n<</SYS>>\n\n<prompt> [/INST]"
    ),
    DeepSeek1B(
        "https://huggingface.co/ggml-org/DeepSeek-R1-Distill-Qwen-1.5B-Q4_0-GGUF/resolve/main/deepseek-r1-distill-qwen-1.5b-q4_0.gguf",
        0.6f,
        "",
        "<prompt>\n\n<think>"
    );

    fun path(context: Context) = context.modelsDir().resolve(url.split("/").last())
}

fun Context.createModel(model: Models): LlamaModel {
    val model = LlamaModel(
        ModelParameters()
            .setModel(modelsDir().resolve(model.path(this)).absolutePath)
            .setGpuLayers(model.gpuLayers)
            .enableLogPrefix()
            .setVerbose()
            .setLogVerbosity(1000000)
    )

    return model
}

fun llama(model: LlamaModel, modelData: Models, prompt: String) = flow {
    var prompt = modelData.systemPrompt.replace("<prompt>", prompt)
    Log.i("AI", prompt)

    val inferParams = InferenceParameters(prompt)
        .setTemperature(modelData.temperature)
        .setPenalizeNl(true)
        .setMiroStat(MiroStat.V2)
        .setStopStrings("[/INST]", "</s>", "[INST]")

    for (output in model.generate(inferParams)) {
        Log.i("AI", output.text)
        prompt += output.text

        emit(output.text)
    }
}
package it.gmmz.llamandroid

import android.content.Context
import android.util.Log
import de.kherud.llama.InferenceParameters
import de.kherud.llama.LlamaModel
import de.kherud.llama.ModelParameters
import de.kherud.llama.args.MiroStat
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.Serializable

@Suppress("ArrayInDataClass")
@Serializable
data class Model(
    val name: String,
    val url: String,
    val sha256: String,
    val gpuLayers: Int = 35,
    val minP: Float = 0.1f,
    val topP: Float = 0.95f,
    val topK: Int = 40,
    val repeatPenalty: Float = 1.0f,
    val stopStrings: Array<String> = arrayOf("[/INST]", "</s>", "[INST]"),
    val temperature: Float,
    val systemPrompt: String,
) {
    fun path(context: Context) = context.modelsDir().resolve(url.split("/").last())
}

fun Context.createModel(model: Model): LlamaModel {
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

fun llama(model: LlamaModel, modelData: Model, prompt: String) = flow {
    var prompt = modelData.systemPrompt.replace("<prompt>", prompt)
    Log.i("AI", prompt)

    val inferParams = InferenceParameters(prompt)
        .setTemperature(modelData.temperature)
        .setPenalizeNl(true)
        .setTopP(modelData.topP)
        .setTopK(modelData.topK)
        .setRepeatPenalty(modelData.repeatPenalty)
        .setMinP(modelData.minP)
        .setMiroStat(MiroStat.V2)
        .setStopStrings(*modelData.stopStrings)

    for (output in model.generate(inferParams)) {
        Log.i("AI", output.text)
        prompt += output.text

        emit(output.text)
    }
}
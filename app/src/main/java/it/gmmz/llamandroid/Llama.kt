package it.gmmz.llamandroid

import android.content.Context
import android.util.Log
import de.kherud.llama.InferenceParameters
import de.kherud.llama.LlamaModel
import de.kherud.llama.ModelParameters
import de.kherud.llama.args.MiroStat
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.Serializable

@Serializable
data class Model(
    val name: String,
    val url: String,
    val sha256: String,
    val gpuLayers: Int = 35,
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
        .setMiroStat(MiroStat.V2)
        .setStopStrings("[/INST]", "</s>", "[INST]")

    for (output in model.generate(inferParams)) {
        Log.i("AI", output.text)
        prompt += output.text

        emit(output.text)
    }
}
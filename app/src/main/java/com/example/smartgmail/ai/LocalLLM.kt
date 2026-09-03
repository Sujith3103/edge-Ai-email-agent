package com.example.smartgmail.ai

import android.content.Context
import com.arm.aichat.AiChat
import com.arm.aichat.InferenceEngine
import kotlinx.coroutines.flow.Flow

class LocalLLM(context: Context) {

    private val engine: InferenceEngine =
        AiChat.getInferenceEngine(context)

    suspend fun loadModel(modelPath: String) {
        engine.loadModel(modelPath)
    }

    suspend fun setSystemPrompt(prompt: String) {
        engine.setSystemPrompt(prompt)
    }

    fun generate(
        prompt: String,
        maxTokens: Int
    ): Flow<String> {
        return engine.sendUserPrompt(
            message = prompt,
            predictLength = maxTokens
        )
    }

    suspend fun bench(
        pp: Int,
        tg: Int,
        pl: Int,
        nr: Int = 1
    ): String {
        return engine.bench(pp, tg, pl, nr)
    }

    val state
        get() = engine.state

    fun cleanUp() {
        engine.cleanUp()
    }

    fun destroy() {
        engine.destroy()
    }
}
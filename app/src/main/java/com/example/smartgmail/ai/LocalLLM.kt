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

    private var lastSystemPrompt: String? = null

    suspend fun setSystemPrompt(prompt: String) {
        if (lastSystemPrompt == prompt) {
            // Already set in native engine, just reset context to it
            resetContext()
            return
        }
        
        engine.setSystemPrompt(prompt)
        lastSystemPrompt = prompt
    }

    suspend fun resetContext() {
        engine.resetContext()
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
package com.example.smartgmail.ai

import android.content.Context
import com.example.smartgmail.model.ModelManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class AIManager(
    private val context: Context
) {

    private val localLLM = LocalLLM(context)
    private val modelManager = ModelManager(context)

    private val _isReady = MutableStateFlow(false)
    val isReady = _isReady.asStateFlow()

    suspend fun initialize() {

        if (_isReady.value) return

        if (!modelManager.isModelInstalled()) {
            println("AI model is not installed")
            return
        }

        val modelPath =
            modelManager
                .modelFile()
                .absolutePath

        localLLM.loadModel(modelPath)

        _isReady.value = true

        println("========== AI MODEL READY ==========")
    }

    fun getLLM(): LocalLLM {
        check(_isReady.value) {
            "AI model is not ready"
        }

        return localLLM
    }
}
package com.example.smartgmail.ai

import android.content.Context
import com.example.smartgmail.model.ModelManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class AIManager(
    private val context: Context
) {

    private val localLLM = LocalLLM(context)
    val modelManager = ModelManager(context)

    private val _isReady = MutableStateFlow(false)
    val isReady = _isReady.asStateFlow()

    private val analysisMutex = Mutex()

    // Stats for observability
    private val _totalProcessed = MutableStateFlow(0)
    val totalProcessed = _totalProcessed.asStateFlow()

    private val _totalFailed = MutableStateFlow(0)
    val totalFailed = _totalFailed.asStateFlow()

    private val _averageTimeMs = MutableStateFlow(0L)
    val averageTimeMs = _averageTimeMs.asStateFlow()

    private var analysisCount = 0L
    private var totalTimeMs = 0L

    suspend fun initialize() {
        if (_isReady.value) return

        if (!modelManager.isModelInstalled()) {
            println("AI model is not installed")
            return
        }

        val modelPath = modelManager.modelFile().absolutePath
        localLLM.loadModel(modelPath)
        _isReady.value = true
        println("========== AI MODEL READY ==========")
    }

    fun getLLM(): LocalLLM {
        check(_isReady.value) { "AI model is not ready" }
        return localLLM
    }

    suspend fun <T> runAnalysis(block: suspend (LocalLLM) -> T): T {
        return analysisMutex.withLock {
            val startTime = System.currentTimeMillis()
            try {
                val result = block(localLLM)
                updateStats(System.currentTimeMillis() - startTime, true)
                result
            } catch (e: Exception) {
                updateStats(System.currentTimeMillis() - startTime, false)
                throw e
            }
        }
    }

    private fun updateStats(timeMs: Long, success: Boolean) {
        if (success) {
            _totalProcessed.value += 1
            analysisCount++
            totalTimeMs += timeMs
            _averageTimeMs.value = totalTimeMs / analysisCount
        } else {
            _totalFailed.value += 1
        }
    }
}

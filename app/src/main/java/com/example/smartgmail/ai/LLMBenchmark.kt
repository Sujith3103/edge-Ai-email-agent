package com.example.smartgmail.ai

import android.util.Log

suspend fun runLLMBenchmark(localLLM: LocalLLM) {

    val tests = listOf(
        512,
        513,
        514,
        515
    )

    for (contextSize in tests) {

        Log.d(
            "LLM_BENCH",
            "========================================"
        )

        Log.d(
            "LLM_BENCH",
            "Starting test: $contextSize tokens"
        )

        val startTime = System.currentTimeMillis()

        try {

            val result = localLLM.bench(
                pp = contextSize,
                tg = 1,
                pl = 1,
                nr = 1
            )

            val elapsed =
                System.currentTimeMillis() - startTime

            Log.d(
                "LLM_BENCH",
                "Result:\n$result"
            )

            Log.d(
                "LLM_BENCH",
                "Total benchmark time: ${elapsed} ms"
            )

            Log.d(
                "LLM_BENCH",
                "TEST $contextSize SUCCESS"
            )

        } catch (e: Exception) {

            val elapsed =
                System.currentTimeMillis() - startTime

            Log.e(
                "LLM_BENCH",
                "TEST $contextSize FAILED after ${elapsed} ms",
                e
            )
        }
    }
}
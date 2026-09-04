package com.example.smartgmail.ai

import android.util.Log

suspend fun runLLMBenchmark(localLLM: LocalLLM) {

    Log.d(
        "LLM_BENCH",
        "========================================"
    )

    Log.d(
        "LLM_BENCH",
        "       SMARTGMAIL LLM BENCHMARK"
    )

    Log.d(
        "LLM_BENCH",
        "========================================"
    )


    // =========================================================
    // 1. PROMPT PROCESSING BENCHMARK
    // =========================================================

    val promptTests = listOf(
       500,1000,2000,3000,4000
    )

    Log.d(
        "LLM_BENCH",
        ""
    )

    Log.d(
        "LLM_BENCH",
        "========== PROMPT PROCESSING =========="
    )

    Log.d(
        "LLM_BENCH",
        "Testing how fast the model consumes input tokens."
    )

    for (promptSize in promptTests) {

        Log.d(
            "LLM_BENCH",
            "----------------------------------------"
        )

        Log.d(
            "LLM_BENCH",
            "Starting PP test: $promptSize tokens"
        )

        val startTime = System.currentTimeMillis()

        try {

            val result = localLLM.bench(
                pp = promptSize,

                // Keep generation almost irrelevant.
                tg = 1,

                pl = 1,
                nr = 1
            )

            val elapsed =
                System.currentTimeMillis() - startTime

            Log.d(
                "LLM_BENCH",
                "PP RESULT ($promptSize):\n$result"
            )

            Log.d(
                "LLM_BENCH",
                "PP TOTAL TIME: ${elapsed} ms"
            )

            Log.d(
                "LLM_BENCH",
                "PP TEST $promptSize SUCCESS"
            )

        } catch (e: Exception) {

            val elapsed =
                System.currentTimeMillis() - startTime

            Log.e(
                "LLM_BENCH",
                "PP TEST $promptSize FAILED after ${elapsed} ms",
                e
            )
        }
    }


    // =========================================================
    // 2. GENERATION BENCHMARK
    // =========================================================

    val generationTests = listOf(
        16,
        32,
        64,
        128,
        256
    )

    Log.d(
        "LLM_BENCH",
        ""
    )

    Log.d(
        "LLM_BENCH",
        "========== GENERATION =========="
    )

    Log.d(
        "LLM_BENCH",
        "Testing how fast the model generates output tokens."
    )

//    for (generationTokens in generationTests) {
//
//        Log.d(
//            "LLM_BENCH",
//            "----------------------------------------"
//        )
//
//        Log.d(
//            "LLM_BENCH",
//            "Starting TG test: $generationTokens tokens"
//        )
//
//        val startTime = System.currentTimeMillis()
//
//        try {
//
//            val result = localLLM.bench(
//                // Small prompt so PP does not dominate.
//                pp = 32,
//
//                tg = generationTokens,
//
//                pl = 1,
//                nr = 1
//            )
//
//            val elapsed =
//                System.currentTimeMillis() - startTime
//
//            Log.d(
//                "LLM_BENCH",
//                "TG RESULT ($generationTokens):\n$result"
//            )
//
//            Log.d(
//                "LLM_BENCH",
//                "TG TOTAL TIME: ${elapsed} ms"
//            )
//
//            Log.d(
//                "LLM_BENCH",
//                "TG TEST $generationTokens SUCCESS"
//
//            )
//
//        } catch (e: Exception) {
//
//            val elapsed =
//                System.currentTimeMillis() - startTime
//
//            Log.e(
//                "LLM_BENCH",
//                "TG TEST $generationTokens FAILED after ${elapsed} ms",
//                e
//            )
//        }
//    }


    // =========================================================
    // 3. DONE
    // =========================================================

    Log.d(
        "LLM_BENCH",
        ""
    )

    Log.d(
        "LLM_BENCH",
        "========================================"
    )

    Log.d(
        "LLM_BENCH",
        "       BENCHMARK COMPLETE"
    )

    Log.d(
        "LLM_BENCH",
        "========================================"
    )
}
package com.example.smartgmail.model

import android.content.Context
import android.net.Uri
import java.io.File

class ModelManager(private val context: Context) {

    companion object {
        private const val MODEL_NAME =
            "qwen2.5-1.5b-instruct-q4_k_m.gguf"
//            "qwen2.5-3b-instruct-q4_k_m.gguf"
    }

    private val modelsDirectory: File
        get() = File(context.filesDir, "models")

    fun modelFile(): File {
        return File(modelsDirectory, MODEL_NAME)
    }

    fun isModelInstalled(): Boolean {
        val file = modelFile()
        return file.exists() && file.isFile && file.canRead()
    }

    fun importModel(uri: Uri): File {
        modelsDirectory.mkdirs()

        val destination = modelFile()

        context.contentResolver.openInputStream(uri).use { input ->
            requireNotNull(input) {
                "Unable to open selected model file"
            }

            destination.outputStream().use { output ->
                input.copyTo(output)
            }
        }

        require(destination.exists()) {
            "Model file was not created"
        }

        require(destination.length() > 0) {
            "Model file is empty"
        }

        return destination
    }
}
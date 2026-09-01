package com.example.smartgmail.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.smartgmail.ui.gmail.GmailScreen
import com.example.smartgmail.ui.llm.LLMScreen


@Composable
fun SmartGmailScreen(
    modifier: Modifier = Modifier
) {

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        GmailScreen()

//        LLMScreen(
//            modifier = Modifier
//                .weight(1f)
//        )
    }
}
package com.example.smartgmail.ui.tasks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp


data class Task(
    val id: String,
    val title: String,
    val date: String?,
    val time: String?,
    val sender: String,
    val completed: Boolean
)


@Composable
fun TasksScreen(
    modifier: Modifier = Modifier
) {

    var tasks by remember {

        mutableStateOf(

            listOf(

                Task(
                    id = "1",
                    title = "Submit project report",
                    date = "Sep 4",
                    time = "5:00 PM",
                    sender = "Dr. Rao",
                    completed = false
                ),

                Task(
                    id = "2",
                    title = "Prepare project presentation",
                    date = "Sep 5",
                    time = null,
                    sender = "Dr. Rao",
                    completed = false
                ),

                Task(
                    id = "3",
                    title = "Submit registration form",
                    date = null,
                    time = null,
                    sender = "College",
                    completed = true
                )
            )
        )
    }


    val incomplete =
        tasks.filter {
            !it.completed
        }

    val completed =
        tasks.filter {
            it.completed
        }


    LazyColumn(

        modifier =
            modifier
                .fillMaxSize()
                .padding(16.dp),

        verticalArrangement =
            Arrangement.spacedBy(8.dp)
    ) {

        // =================================================
        // HEADER
        // =================================================

        item {

            Text(
                text = "Tasks",
                fontWeight = FontWeight.Bold
            )

            Spacer(
                modifier =
                    Modifier.height(16.dp)
            )
        }


        // =================================================
        // ACTIVE TASKS
        // =================================================

        item {

            Text(
                text = "Upcoming",
                fontWeight = FontWeight.Bold
            )
        }


        items(
            items = incomplete,
            key = {
                it.id
            }
        ) { task ->

            TaskCard(
                task = task,

                onCheckedChange = {

                    tasks =
                        tasks.map {

                            if (it.id == task.id) {

                                it.copy(
                                    completed = true
                                )

                            } else {
                                it
                            }
                        }
                }
            )
        }


        // =================================================
        // COMPLETED
        // =================================================

        if (completed.isNotEmpty()) {

            item {

                Spacer(
                    modifier =
                        Modifier.height(16.dp)
                )

                Text(
                    text = "Completed",
                    fontWeight = FontWeight.Bold
                )
            }


            items(
                items = completed,
                key = {
                    it.id
                }
            ) { task ->

                TaskCard(
                    task = task,

                    onCheckedChange = {

                        tasks =
                            tasks.map {

                                if (it.id == task.id) {

                                    it.copy(
                                        completed = false
                                    )

                                } else {
                                    it
                                }
                            }
                    }
                )
            }
        }
    }
}


@Composable
private fun TaskCard(
    task: Task,
    onCheckedChange: () -> Unit
) {

    Card(
        modifier =
            Modifier.fillMaxWidth()
    ) {

        Row(

            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(12.dp),

            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Checkbox(

                checked =
                    task.completed,

                onCheckedChange = {
                    onCheckedChange()
                }
            )


            Column(
                modifier =
                    Modifier.weight(1f)
            ) {

                Text(
                    text =
                        task.title,

                    fontWeight =
                        FontWeight.Medium
                )


                Spacer(
                    modifier =
                        Modifier.height(4.dp)
                )


                if (
                    task.date != null ||
                    task.time != null
                ) {

                    Text(
                        text =
                            listOfNotNull(
                                task.date,
                                task.time
                            ).joinToString(
                                " · "
                            )
                    )
                }


                Text(
                    text =
                        "From: ${task.sender}"
                )
            }
        }
    }
}
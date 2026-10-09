package com.example.comp90018.ui.screens.cards

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton

@Composable
fun EnterDisplayName(
    onSubmit: (String) -> Unit
) {
    var input by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = {
            // Do nothing: prevent dismissal by tapping outside or pressing Back
        },
        title = {
            Text("Enter display name")
        },
        text = {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                label = { Text("Display name") },
                singleLine = true,
                placeholder = {
                    Text("Enter display name")
                }
            )
        },
        confirmButton = {
            TextButton(
                enabled = input.isNotBlank(),
                onClick = {
                    onSubmit(input.trim())
                }
            ) {
                Text("Submit")
            }
        }
    )
}
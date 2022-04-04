package ru.danilkharadmate.texttranslation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun AboutScreen(){
    Box(Modifier.fillMaxSize()) {
        Text(
            modifier = Modifier
                .padding(16.dp),
            text = "Приложение сделано для итогового проекта 11 класса.\nРазработчик: Хайрулин Данил")
    }
}
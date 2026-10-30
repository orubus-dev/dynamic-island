package app.orubus.island.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.orubus.island.core.tr
import app.orubus.island.ui.Page
import app.orubus.island.ui.SectionHeader
import app.orubus.island.ui.Ui
import app.orubus.island.ui.glassCard

private data class OssLib(val name: String, val license: String, val note: String, val noteRu: String)

/** Проекты, чьим трудом построен остров, и их лицензии. */
private val ossLibs = listOf(
    OssLib(
        "Jetpack Compose · Material 3", "Apache License 2.0",
        "The whole interface of the island and of this app is drawn with Compose.",
        "Весь интерфейс острова и этого приложения нарисован с помощью Compose.",
    ),
    OssLib(
        "Kotlin & kotlinx.coroutines · kotlinx.serialization", "Apache License 2.0",
        "The language and the runtime the island speaks.",
        "Язык и среда, на которых работает остров.",
    ),
    OssLib(
        "Material Icons (extended)", "Apache License 2.0",
        "System icons of events and screens.",
        "Системные значки событий и экранов.",
    ),
    OssLib(
        "Ionicons", "MIT License",
        "The iOS-style icons of the island's pills.",
        "Значки плашек острова в стиле iOS.",
    ),
    OssLib(
        "Inter", "SIL Open Font License 1.1",
        "The default font of the island (\"Core UI\").",
        "Шрифт острова по умолчанию («Core UI»).",
    ),
    OssLib(
        "Nunito · Lora · Bayon", "SIL Open Font License 1.1",
        "The optional island fonts.",
        "Дополнительные шрифты острова.",
    ),
)

private val licenseTexts = mapOf(
    "Apache License 2.0" to "Copyright the respective authors.\n\n" +
        "Licensed under the Apache License, Version 2.0 (the \"License\"); you may not use these files except in " +
        "compliance with the License. You may obtain a copy of the License at\n\nwww.apache.org/licenses/LICENSE-2.0\n\n" +
        "Unless required by applicable law or agreed to in writing, software distributed under the License is " +
        "distributed on an \"AS IS\" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.",
    "MIT License" to "Copyright the respective authors.\n\n" +
        "Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated " +
        "documentation files, to deal in the software without restriction, including without limitation the rights to " +
        "use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the software, subject to the " +
        "following conditions: the above copyright notice and this permission notice shall be included in all copies " +
        "or substantial portions of the software.",
    "SIL Open Font License 1.1" to "Copyright the respective authors.\n\n" +
        "These fonts are licensed under the SIL Open Font License, Version 1.1: the fonts may be used, studied, " +
        "modified and redistributed freely — bundled with software, embedded in documents and installed on any " +
        "computer. The full text lives at\n\nscripts.sil.org/OFL.",
)

/**
 * «Лицензии open source»: благодарности проектам, на которых стоит остров,
 * и краткие тексты их лицензий — как требует Apache-2.0 и пр.
 */
@Composable
fun LicensesScreen(onBack: () -> Unit) {
    Page(tr("Лицензии open source", "Open source licenses"), onBack) {
        Text(
            tr(
                "Остров собран из отличных свободных проектов. Ниже — что именно и под какими лицензиями. " +
                    "Полные тексты лицензий — на сайтах проектов.",
                "The island is built on great open-source projects. Below is what we use and under which licenses. " +
                    "Full license texts live on the projects' sites.",
            ),
            color = Ui.Sub.copy(alpha = 0.8f), fontSize = 14.sp, lineHeight = 20.sp,
        )
        Spacer(Modifier.height(18.dp))

        SectionHeader(tr("Проекты", "Projects"))
        ossLibs.forEach { lib ->
            Column(
                Modifier
                    .fillMaxWidth()
                    .glassCard(RoundedCornerShape(20.dp))
                    .padding(horizontal = 18.dp, vertical = 14.dp),
            ) {
                Text(lib.name, color = Ui.Text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                Text(
                    "${tr(lib.noteRu, lib.note)}\n${lib.license}",
                    color = Ui.Sub, fontSize = 13.sp, lineHeight = 18.sp,
                )
            }
            Spacer(Modifier.height(10.dp))
        }

        SectionHeader(tr("Тексты лицензий", "License texts"))
        licenseTexts.forEach { (name, text) ->
            Column(
                Modifier
                    .fillMaxWidth()
                    .glassCard(RoundedCornerShape(20.dp))
                    .padding(horizontal = 18.dp, vertical = 14.dp),
            ) {
                Text(name, color = Ui.Text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(6.dp))
                Text(text, color = Ui.Sub.copy(alpha = 0.85f), fontSize = 12.sp, lineHeight = 17.sp)
            }
            Spacer(Modifier.height(10.dp))
        }

        Spacer(Modifier.height(24.dp))
    }
}

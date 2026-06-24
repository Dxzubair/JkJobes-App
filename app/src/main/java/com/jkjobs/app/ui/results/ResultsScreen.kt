package com.jkjobs.app.ui.results

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Grade
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import com.jkjobs.app.ui.admitcards.ComingSoon

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultsScreen() {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Results") }) }
    ) { padding ->
        ComingSoon(
            modifier = androidx.compose.ui.Modifier.padding(padding),
            icon = Icons.Filled.Grade,
            title = "Results are coming soon",
            message = "Declared results from JKSSB, JKPSC, JK Police, and university exams will " +
                "show up here once we wire up that source. Stay tuned!"
        )
    }
}

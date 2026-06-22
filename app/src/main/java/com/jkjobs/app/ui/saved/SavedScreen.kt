package com.jkjobs.app.ui.saved

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jkjobs.app.data.SafeUrl
import com.jkjobs.app.ui.JobCard
import com.jkjobs.app.ui.JobsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedScreen(viewModel: JobsViewModel = viewModel()) {
    val saved by viewModel.saved.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Scaffold(
        topBar = { TopAppBar(title = { Text("Saved Jobs") }) }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (saved.isEmpty()) {
                Text(
                    "Nothing saved yet. Tap the bookmark icon on any job to save it here.",
                    modifier = Modifier.align(Alignment.Center).padding(24.dp)
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(saved, key = { it.id }) { job ->
                        JobCard(
                            job = job,
                            onOpen = {
                                if (SafeUrl.isSafeToOpen(job.link)) {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, job.link.toUri()))
                                }
                            },
                            onToggleSave = { viewModel.toggleSaved(job) }
                        )
                    }
                }
            }
        }
    }
}

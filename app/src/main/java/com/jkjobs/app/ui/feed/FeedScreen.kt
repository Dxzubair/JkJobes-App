package com.jkjobs.app.ui.feed

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jkjobs.app.data.SafeUrl
import com.jkjobs.app.ui.FILTERABLE_DISTRICTS
import com.jkjobs.app.ui.JobCard
import com.jkjobs.app.ui.JobsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(viewModel: JobsViewModel = viewModel()) {
    val jobs by viewModel.visibleFeed.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val status by viewModel.statusMessage.collectAsStateWithLifecycle()
    val query by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedSource by viewModel.selectedSource.collectAsStateWithLifecycle()
    val selectedDistrict by viewModel.selectedDistrict.collectAsStateWithLifecycle()
    val sources by viewModel.availableSources.collectAsStateWithLifecycle()
    val context = LocalContext.current

    androidx.compose.runtime.LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(3000)
        viewModel.markAllSeen()
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("All Jobs") }) }
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.refresh() },
            modifier = Modifier.padding(padding).fillMaxSize()
        ) {
            Column(Modifier.fillMaxSize()) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    placeholder = { Text("Search job titles") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .semantics { contentDescription = "Search job titles" }
                )

                if (sources.isNotEmpty()) {
                    FilterChipRow(
                        label = "Category",
                        allLabel = "All sources",
                        options = sources,
                        selected = selectedSource,
                        onSelect = { viewModel.setSourceFilter(it) }
                    )
                }

                FilterChipRow(
                    label = "District",
                    allLabel = "All districts",
                    options = FILTERABLE_DISTRICTS,
                    selected = selectedDistrict,
                    onSelect = { viewModel.setDistrictFilter(it) }
                )

                status?.let { msg ->
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(msg, modifier = Modifier.padding(10.dp), style = MaterialTheme.typography.bodySmall)
                    }
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    if (jobs.isEmpty() && !isRefreshing) {
                        Text(
                            status ?: "No jobs match these filters. Try clearing one, or pull down to refresh.",
                            modifier = Modifier.align(Alignment.Center).padding(24.dp),
                            textAlign = TextAlign.Center
                        )
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Stable key = job.id (the SHA-256-based id) avoids unnecessary
                            // recomposition/re-layout of unaffected cards when the list reorders
                            // or partially updates after a refresh.
                            items(jobs, key = { it.id }) { job ->
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
    }
}

/**
 * One horizontally-scrollable row of filter chips with an "All" reset chip first.
 * Reused for both the source/category row and the district row to keep the two visually
 * and behaviorally consistent, and to avoid duplicating the chip-building logic.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterChipRow(
    label: String,
    allLabel: String,
    options: List<String>,
    selected: String?,
    onSelect: (String?) -> Unit
) {
    Column(modifier = Modifier.padding(bottom = 6.dp)) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 16.dp, bottom = 2.dp)
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item(key = "__all__") {
                SelectableChip(text = allLabel, isSelected = selected == null) { onSelect(null) }
            }
            items(options, key = { it }) { option ->
                SelectableChip(text = option, isSelected = selected == option) { onSelect(option) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectableChip(text: String, isSelected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = { Text(text) },
        leadingIcon = if (isSelected) {
            { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
        } else null
    )
}

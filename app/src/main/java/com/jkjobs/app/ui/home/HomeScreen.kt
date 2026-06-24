package com.jkjobs.app.ui.home

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jkjobs.app.data.JobPosting
import com.jkjobs.app.data.SafeUrl
import com.jkjobs.app.ui.JobsViewModel
import com.jkjobs.app.ui.isLikelyStale

/** JKJobs+ brand purple, used for the Home dashboard's hero/banner elements.
 *  Kept local to Home (rather than overriding the app-wide Material You theme) so the
 *  rest of the app's dynamic-color behavior on Android 12+ is unaffected. */
private val BrandPurple = Color(0xFF4B2EDB)
private val BrandPurpleDark = Color(0xFF3A21B0)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: JobsViewModel = viewModel(),
    onGoToJobs: () -> Unit,
    onGoToSaved: () -> Unit,
    onGoToSettings: () -> Unit
) {
    val jobs by viewModel.visibleFeed.collectAsStateWithLifecycle()
    val context = LocalContext.current
    // Stopgap: keep obviously old backlog postings out of the "Latest Jobs" spotlight until
    // real per-source date parsing (postedAtMillis) replaces this. See StaleJobHeuristic.kt.
    val latest = jobs.filterNot { it.isLikelyStale() }.take(5).ifEmpty { jobs.take(5) }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(3000)
        viewModel.markAllSeen()
    }

    Scaffold { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item { HomeHeader() }
            item { HeroBanner(onExplore = onGoToJobs) }
            item { QuickAccessGrid(onGoToJobs, onGoToSaved, onGoToSettings) }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Latest Jobs", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    TextButton(onClick = onGoToJobs) { Text("View All") }
                }
            }
            if (latest.isEmpty()) {
                item {
                    Text(
                        "No jobs yet - pull to refresh on the Jobs tab.",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(latest, key = { it.id }) { job ->
                    HomeJobRow(
                        job = job,
                        onOpen = {
                            if (SafeUrl.isSafeToOpen(job.link)) {
                                context.startActivity(Intent(Intent.ACTION_VIEW, job.link.toUri()))
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeHeader() {
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = BrandPurple,
            modifier = Modifier.size(46.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text("JK", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            }
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text("Hello 👋", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                "Welcome to JKJobs+",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun HeroBanner(onExplore: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.horizontalGradient(listOf(BrandPurple, BrandPurpleDark)))
            .padding(20.dp)
    ) {
        Column {
            Text(
                "Latest Jobs",
                color = Color.White,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Right Opportunities. Bright Future.",
                color = Color.White.copy(alpha = 0.9f),
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(14.dp))
            Button(
                onClick = onExplore,
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = BrandPurple),
                shape = RoundedCornerShape(50)
            ) {
                Text("Explore Jobs")
            }
        }
    }
}

private data class QuickAccessItem(val label: String, val icon: ImageVector, val onClick: () -> Unit)

@Composable
private fun QuickAccessGrid(onGoToJobs: () -> Unit, onGoToSaved: () -> Unit, onGoToSettings: () -> Unit) {
    val items = listOf(
        QuickAccessItem("All Jobs", Icons.AutoMirrored.Filled.List, onGoToJobs),
        QuickAccessItem("Saved Jobs", Icons.Filled.BookmarkBorder, onGoToSaved),
        QuickAccessItem("Notifications", Icons.Filled.Notifications, onGoToSettings),
        QuickAccessItem("Settings", Icons.Filled.Settings, onGoToSettings)
    )
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text("Quick Access", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items.forEach { QuickAccessTile(it, Modifier.weight(1f)) }
        }
    }
}

@Composable
private fun QuickAccessTile(item: QuickAccessItem, modifier: Modifier = Modifier) {
    Surface(
        onClick = item.onClick,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier.aspectRatio(0.95f)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(shape = CircleShape, color = BrandPurple.copy(alpha = 0.12f)) {
                Icon(
                    item.icon,
                    contentDescription = item.label,
                    tint = BrandPurple,
                    modifier = Modifier.padding(10.dp).size(20.dp)
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(item.label, style = MaterialTheme.typography.labelSmall, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
    }
}

@Composable
private fun HomeJobRow(job: JobPosting, onOpen: () -> Unit) {
    Surface(
        onClick = onOpen,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        job.title,
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (!job.isSeen) {
                        Spacer(Modifier.width(6.dp))
                        Surface(color = BrandPurple, shape = RoundedCornerShape(6.dp)) {
                            Text(
                                "New",
                                color = Color.White,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    "${job.source} • ${job.publishedLabel}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
}

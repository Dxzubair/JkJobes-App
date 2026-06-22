package com.jkjobs.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jkjobs.app.data.JobPosting
import com.jkjobs.app.data.JobRepository
import com.jkjobs.app.data.local.AppDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Districts worth surfacing as quick filter chips. Matched against job titles via DAO LIKE
 *  query - if a posting doesn't mention the district by name in its title, it won't match
 *  (acceptable tradeoff vs. building a real geo-tagging pipeline for a v1 filter feature). */
val FILTERABLE_DISTRICTS = listOf(
    "Srinagar", "Jammu", "Anantnag", "Kulgam", "Baramulla", "Pulwama", "Budgam", "Kupwara"
)

class JobsViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = JobRepository(AppDatabase.get(app).jobDao())

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    /** Short, human-readable status for the top of the Feed screen - null when everything's fine. */
    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    /** null = "All" chip selected, i.e. no filter on that dimension. */
    private val _selectedSource = MutableStateFlow<String?>(null)
    val selectedSource: StateFlow<String?> = _selectedSource.asStateFlow()

    private val _selectedDistrict = MutableStateFlow<String?>(null)
    val selectedDistrict: StateFlow<String?> = _selectedDistrict.asStateFlow()

    /** Drives the "source" chip row from whatever sources actually have cached postings,
     *  so it never drifts out of sync with JobSources.kt. */
    val availableSources: StateFlow<List<String>> =
        repo.observeDistinctSources()
            .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), emptyList())

    /** Backs the Saved tab - independent of the Feed's source/district/search filters. */
    val saved: StateFlow<List<JobPosting>> =
        repo.observeSaved()
            .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * The feed actually shown on screen.
     *
     * source + district are pushed down to the DB query (flatMapLatest re-subscribes to a new
     * Room query whenever either changes - Room then only re-emits when the underlying table
     * changes, no manual diffing needed). Free-text search is applied client-side on top of the
     * already-filtered, already-small result set, which is the cheaper place to do substring
     * matching than re-querying SQLite on every keystroke.
     */
    val visibleFeed: StateFlow<List<JobPosting>> =
        combine(_selectedSource, _selectedDistrict) { source, district -> source to district }
            .flatMapLatest { (source, district) -> repo.observeFiltered(source, district) }
            .combine(_searchQuery) { jobs, query ->
                if (query.isBlank()) jobs
                else jobs.filter { it.title.contains(query, ignoreCase = true) }
            }
            .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        refresh()
    }

    fun setSearchQuery(q: String) { _searchQuery.value = q }

    /** Tapping an already-selected source chip clears it back to "All" - standard toggle UX. */
    fun setSourceFilter(source: String?) {
        _selectedSource.value = if (_selectedSource.value == source) null else source
    }

    fun setDistrictFilter(district: String?) {
        _selectedDistrict.value = if (_selectedDistrict.value == district) null else district
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            _statusMessage.value = null
            try {
                val result = repo.refresh()
                _statusMessage.value = when {
                    result.allSourcesFailed && result.looksLikeDeviceOffline ->
                        "Couldn't reach any source - check your connection. Showing your last saved feed."
                    result.allSourcesFailed ->
                        "All sources failed to respond this time (not a connectivity issue - may need attention). Showing your last saved feed."
                    result.failures.isNotEmpty() ->
                        "${result.failures.size} of ${result.totalSourcesAttempted} sources didn't respond this time (${result.failures.joinToString { it.sourceName }}). Feed updated with the rest."
                    else -> null
                }
            } catch (e: Exception) {
                _statusMessage.value = "Couldn't refresh: ${e.message ?: "unknown error"}"
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun toggleSaved(job: JobPosting) {
        viewModelScope.launch { repo.toggleSaved(job.id, !job.isSaved) }
    }
}

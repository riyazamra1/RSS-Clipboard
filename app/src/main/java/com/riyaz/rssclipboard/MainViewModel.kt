package com.riyaz.rssclipboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.riyaz.rssclipboard.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val repository = ClipboardRepository(AppDatabase.get(app).clipboardDao())
    val items = repository.items.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()
    private val _filter = MutableStateFlow<ClipboardType?>(null)
    val filter: StateFlow<ClipboardType?> = _filter.asStateFlow()

    val visibleItems = combine(items, query, filter) { list, q, type ->
        list.filter { (type == null || it.type == type) && it.content.contains(q, ignoreCase = true) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init { viewModelScope.launch { repository.deleteExpired() } }
    fun setQuery(value: String) { _query.value = value }
    fun setFilter(value: ClipboardType?) { _filter.value = value }
    fun add(content: String) = viewModelScope.launch { repository.add(content) }
    fun delete(item: ClipboardItem) = viewModelScope.launch { repository.delete(item) }
    fun togglePin(item: ClipboardItem) = viewModelScope.launch {
        repository.update(item.copy(pinned = !item.pinned), item.content)
    }
    fun update(item: ClipboardItem, content: String) = viewModelScope.launch {
        repository.update(item, content)
    }
    fun clearAll() = viewModelScope.launch { repository.clear() }
}

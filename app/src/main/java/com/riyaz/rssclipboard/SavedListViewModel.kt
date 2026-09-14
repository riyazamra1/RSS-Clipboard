package com.riyaz.rssclipboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.riyaz.rssclipboard.data.AppDatabase
import com.riyaz.rssclipboard.data.SavedItem
import com.riyaz.rssclipboard.data.SavedRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class SavedListViewModel(app: Application) : AndroidViewModel(app) {
    private val repository = SavedRepository(AppDatabase.get(app).savedDao())
    val items = repository.items.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()
    private val _category = MutableStateFlow<String?>(null)
    val category: StateFlow<String?> = _category.asStateFlow()

    val categories = items.map { list -> list.map { it.category }.distinct().sorted() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val visibleItems = combine(items, query, category) { list, q, category ->
        list.filter { (category == null || it.category == category) &&
            (q.isBlank() || it.category.contains(q, true) || it.fileName.contains(q, true) ||
                it.data.contains(q, true) || it.description.contains(q, true)) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setQuery(value: String) { _query.value = value }
    fun setCategory(value: String?) { _category.value = value }
    fun add(category: String, fileName: String, data: String, description: String) = viewModelScope.launch {
        repository.add(category, fileName, data, description)
    }
    fun update(item: SavedItem, category: String, fileName: String, data: String, description: String) = viewModelScope.launch {
        repository.update(item, category, fileName, data, description)
    }
    fun delete(item: SavedItem) = viewModelScope.launch { repository.delete(item) }
    fun clearAll() = viewModelScope.launch { repository.clear() }
}

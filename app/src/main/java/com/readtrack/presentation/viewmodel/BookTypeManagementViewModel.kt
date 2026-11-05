package com.readtrack.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.readtrack.data.local.entity.BookTypeEntity
import com.readtrack.domain.repository.BookTypeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class BookTypeManagementViewModel @Inject constructor(
    private val bookTypeRepository: BookTypeRepository
) : ViewModel() {

    val bookTypes: StateFlow<List<BookTypeEntity>> = bookTypeRepository.getAllBookTypes()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    suspend fun createBookType(name: String): Long =
        bookTypeRepository.createBookType(name.trim())

    suspend fun renameBookType(id: Long, newName: String): Boolean =
        bookTypeRepository.renameBookType(id, newName.trim())

    /** 删除类型；仅剩一个类型时拒绝删除并返回 false */
    suspend fun deleteBookType(id: Long): Boolean =
        bookTypeRepository.deleteBookType(id)

    suspend fun countBooksWithType(name: String): Int =
        bookTypeRepository.countBooksWithType(name)
}

package com.example.bookclub.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.example.bookclub.database.AppDatabase
import com.example.bookclub.database.Book
import com.example.bookclub.database.Category
import kotlinx.coroutines.launch

class BookViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val bookDao = db.bookDao()
    private val categoryDao = db.categoryDao()

    // Обычный список всех книг (для сортировки из настроек)
    val allBooks: LiveData<List<Book>> = bookDao.getAllBooks()

    // Категории
    val allCategories: LiveData<List<Category>> = categoryDao.getAllCategories()

    // === ФИЛЬТРАЦИЯ ===
    // Храним текущие параметры фильтрации: query, status, categoryId
    private val filterParams = MutableLiveData<Triple<String, String, Int>>(
        Triple("", "", -1)
    )

    // Реактивный список: при изменении параметров — новый запрос в DAO
    val filteredBooks: LiveData<List<Book>> = filterParams.switchMap { params ->
        val query = params.first
        val status = params.second
        val categoryId = params.third
        bookDao.filterBooks(query, status, categoryId)
    }

    // Обновить фильтры
    fun setFilters(query: String, status: String, categoryId: Int) {
        filterParams.value = Triple(query, status, categoryId)
    }

    // === CRUD ===
    fun insert(book: Book) = viewModelScope.launch {
        bookDao.insert(book)
    }

    fun update(book: Book) = viewModelScope.launch {
        bookDao.update(book)
    }

    fun delete(book: Book) = viewModelScope.launch {
        bookDao.delete(book)
    }

    fun getBookById(id: Int): LiveData<Book> = bookDao.getBookById(id)

    // === ДЛЯ СТАТИСТИКИ ===
    suspend fun getAllBooksOnce(): List<Book> = bookDao.getAllBooksOnce()
    suspend fun getAllCategoriesOnce(): List<Category> = categoryDao.getAllCategoriesOnce()

    suspend fun getCategoryName(categoryId: Int): String {
        return categoryDao.getCategoryById(categoryId)?.name ?: "Без жанра"
    }

    // === СОРТИРОВКА ===
    fun getBooksSorted(sortOrder: String): LiveData<List<Book>> = when (sortOrder) {
        "author" -> bookDao.getAllBooksByAuthor()
        "status" -> bookDao.getAllBooksByStatus()
        else -> bookDao.getAllBooks()
    }

    // === ЗАПОЛНЕНИЕ ЖАНРОВ ПРИ ПЕРВОМ ЗАПУСКЕ ===
    fun seedCategoriesIfEmpty() = viewModelScope.launch {
        val existing = categoryDao.getAllCategoriesOnce()
        if (existing.isEmpty()) {
            categoryDao.insert(Category(name = "Роман"))
            categoryDao.insert(Category(name = "Классика"))
            categoryDao.insert(Category(name = "Детектив"))
            categoryDao.insert(Category(name = "Сказка"))
            categoryDao.insert(Category(name = "Фантастика"))
        }
    }
}
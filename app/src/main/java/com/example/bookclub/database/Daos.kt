package com.example.bookclub.database

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface BookDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(book: Book)

    @Update
    suspend fun update(book: Book)

    @Delete
    suspend fun delete(book: Book)

    @Query("SELECT * FROM books ORDER BY title ASC")
    fun getAllBooks(): LiveData<List<Book>>

    @Query("SELECT * FROM books WHERE id = :id")
    fun getBookById(id: Int): LiveData<Book>

    @Query("SELECT * FROM books ORDER BY author ASC")
    fun getAllBooksByAuthor(): LiveData<List<Book>>

    @Query("SELECT * FROM books ORDER BY status ASC")
    fun getAllBooksByStatus(): LiveData<List<Book>>

    @Query("SELECT * FROM books")
    suspend fun getAllBooksOnce(): List<Book>

    @Query("""
    SELECT * FROM books 
    WHERE (title LIKE '%' || :query || '%' OR author LIKE '%' || :query || '%')
    AND (:status = '' OR status = :status)
    AND (:categoryId = -1 OR categoryId = :categoryId)
    ORDER BY title ASC
""")
    fun filterBooks(query: String, status: String, categoryId: Int): LiveData<List<Book>>

}

@Dao
interface CategoryDao {
    @Insert
    suspend fun insert(category: Category)

    @Query("SELECT * FROM categories")
    fun getAllCategories(): LiveData<List<Category>>

    @Query("SELECT * FROM categories")
    suspend fun getAllCategoriesOnce(): List<Category>

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getCategoryById(id: Int): Category?
}

@Dao
interface TagDao {
    @Insert
    suspend fun insert(tag: Tag)

    @Query("SELECT * FROM tags")
    fun getAllTags(): LiveData<List<Tag>>
}


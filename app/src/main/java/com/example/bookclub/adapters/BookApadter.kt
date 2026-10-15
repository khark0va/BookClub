package com.example.bookclub.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.bookclub.database.Book
import com.example.bookclub.databinding.ItemBookBinding
import coil.load
import android.graphics.Color
class BookAdapter(
    private val onItemClick: (Book) -> Unit
) : ListAdapter<Book, BookAdapter.BookViewHolder>(BookDiffCallback()) {

    inner class BookViewHolder(
        private val binding: ItemBookBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(book: Book) {
            binding.titleTextView.text = book.title
            binding.authorTextView.text = book.author
            binding.statusTextView.text = book.status

            if (book.rating != null) {
                val stars = "★".repeat(book.rating.toInt()) + "☆".repeat(5 - book.rating.toInt())
                binding.ratingTextView.text = stars
            } else {
                binding.ratingTextView.text = ""
            }
            if (!book.coverUri.isNullOrBlank()) {
                binding.coverImageView.load(book.coverUri) {
                    crossfade(true)
                }
            } else {
                binding.coverImageView.setImageDrawable(null)
                binding.coverImageView.setBackgroundColor(Color.parseColor("#5C1A1B"))
            }

            binding.root.setOnClickListener { onItemClick(book) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookViewHolder {
        val binding = ItemBookBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return BookViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BookViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}

class BookDiffCallback : DiffUtil.ItemCallback<Book>() {
    override fun areItemsTheSame(oldItem: Book, newItem: Book): Boolean = oldItem.id == newItem.id
    override fun areContentsTheSame(oldItem: Book, newItem: Book): Boolean = oldItem == newItem
}
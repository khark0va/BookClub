package com.example.bookclub.ui

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.bookclub.database.Book
import com.example.bookclub.databinding.DialogEditNotesBinding
import com.example.bookclub.databinding.DialogEditReviewBinding
import com.example.bookclub.databinding.FragmentDetailBinding
import com.example.bookclub.viewmodels.BookViewModel
import coil.load
import com.example.bookclub.R
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class BookDetailFragment : Fragment() {

    private var _binding: FragmentDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: BookViewModel by viewModels()
    private var currentBook: Book? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val bookId = arguments?.getInt("bookId") ?: -1
        if (bookId == -1) {
            binding.titleTextView.text = "Книга не выбрана"
            binding.editButton.isEnabled = false
            binding.deleteButton.isEnabled = false
            return
        }

        viewModel.getBookById(bookId).observe(viewLifecycleOwner) { book ->
            currentBook = book
            displayBook(book)
        }

        // Клик по заметкам — редактируем заметки
        binding.notesTextView.setOnClickListener { openNotesDialog() }

        // Завершить чтение → СРАЗУ открываем диалог отзыва
        binding.finishReadingButton.setOnClickListener {
            currentBook?.let { book ->
                val updated = book.copy(status = "Прочитано")
                viewModel.update(updated)
                Toast.makeText(requireContext(), "Книга прочитана!", Toast.LENGTH_SHORT).show()

                // Открываем диалог отзыва сразу после смены статуса
                openReviewDialog()
            }
        }

        // Написать отзыв (если книга прочитана, но отзыва нет)
        binding.writeReviewButton.setOnClickListener { openReviewDialog() }

        // Редактировать отзыв (если отзыв уже есть)
        binding.editReviewButton.setOnClickListener { openReviewDialog() }

        binding.deleteButton.setOnClickListener { confirmDelete() }

        binding.editButton.setOnClickListener {
            val bundle = Bundle().apply { putInt("bookId", currentBook?.id ?: -1) }
            findNavController().navigate(R.id.addBookFragment, bundle)
        }
    }

    private fun displayBook(book: Book) {
        binding.titleTextView.text = book.title
        binding.authorTextView.text = book.author
        binding.statusTextView.text = book.status

        // Загружаем название жанра асинхронно
        viewLifecycleOwner.lifecycleScope.launch {
            val genreName = viewModel.getCategoryName(book.categoryId)
            binding.genreTextView.text = genreName
        }

        // Заметки
        if (book.notes.isNullOrBlank()) {
            binding.notesTextView.text = "Нажмите, чтобы добавить заметку"
        } else {
            binding.notesTextView.text = book.notes
        }

        if (!book.coverUri.isNullOrBlank()) {
            binding.coverImageView.load(book.coverUri)
        } else {
            binding.coverImageView.setImageDrawable(null)
        }

        // Заметки
        if (book.notes.isNullOrBlank()) {
            binding.notesTextView.text = "Нажмите, чтобы добавить заметку"
        } else {
            binding.notesTextView.text = book.notes
        }

        when (book.status) {
            "Читаю" -> {
                binding.finishReadingButton.visibility = View.VISIBLE
                binding.reviewSection.visibility = View.GONE
            }
            "Прочитано" -> {
                binding.finishReadingButton.visibility = View.GONE
                binding.reviewSection.visibility = View.VISIBLE

                // Если отзыва нет — показываем только кнопку "Написать отзыв"
                if (book.reviewText.isNullOrBlank() && (book.rating == null || book.rating == 0f)) {
                    binding.writeReviewButton.visibility = View.VISIBLE
                    binding.reviewContentLayout.visibility = View.GONE
                } else {
                    // Если отзыв есть — показываем звёзды и текст
                    binding.writeReviewButton.visibility = View.GONE
                    binding.reviewContentLayout.visibility = View.VISIBLE

                    val rating = book.rating?.toInt() ?: 0
                    val stars = "★".repeat(rating) + "☆".repeat(5 - rating)
                    binding.ratingTextView.text = stars
                    binding.reviewTextView.text = book.reviewText ?: "Без текста"
                }
            }
            else -> {
                binding.finishReadingButton.visibility = View.GONE
                binding.reviewSection.visibility = View.GONE
            }

        }
    }

    // === ДИАЛОГ РЕДАКТИРОВАНИЯ ЗАМЕТОК ===
    private fun openNotesDialog() {
        val book = currentBook ?: return
        val dialogBinding = DialogEditNotesBinding.inflate(layoutInflater)
        dialogBinding.notesEditText.setText(book.notes ?: "")

        AlertDialog.Builder(requireContext())
            .setTitle("Заметки")
            .setView(dialogBinding.root)
            .setPositiveButton("Сохранить") { _, _ ->
                val newNotes = dialogBinding.notesEditText.text.toString()
                viewModel.update(book.copy(notes = newNotes))
                Toast.makeText(requireContext(), "Заметки сохранены", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Отмена", null)
            .show()
    }


    private fun openReviewDialog() {
        val book = currentBook ?: return
        val dialogBinding = DialogEditReviewBinding.inflate(layoutInflater)
        dialogBinding.reviewEditText.setText(book.reviewText ?: "")
        dialogBinding.ratingBar.rating = book.rating ?: 0f

        AlertDialog.Builder(requireContext())
            .setTitle(if (book.reviewText.isNullOrBlank()) "Оставить отзыв" else "Редактировать отзыв")
            .setView(dialogBinding.root)
            .setPositiveButton("Сохранить") { _, _ ->
                val newReview = dialogBinding.reviewEditText.text.toString().trim()
                val newRating = dialogBinding.ratingBar.rating

                if (newRating == 0f) {
                    Toast.makeText(requireContext(), "Поставьте оценку", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                // Если отзыв сохраняется — статус автоматически "Прочитано"
                viewModel.update(
                    book.copy(
                        reviewText = newReview,
                        rating = newRating,
                        status = "Прочитано"   // <-- ВОТ ЭТА СТРОКА
                    )
                )
                Toast.makeText(requireContext(), "Отзыв сохранён", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun confirmDelete() {
        val book = currentBook ?: return
        AlertDialog.Builder(requireContext())
            .setTitle("Удалить книгу?")
            .setMessage("Вы уверены, что хотите удалить \"${book.title}\"?")
            .setPositiveButton("Удалить") { _, _ ->
                viewModel.delete(book)
                Toast.makeText(requireContext(), "Книга удалена", Toast.LENGTH_SHORT).show()
                findNavController().navigateUp()
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
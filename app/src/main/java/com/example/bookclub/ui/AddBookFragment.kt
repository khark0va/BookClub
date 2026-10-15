package com.example.bookclub.ui

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import coil.load
import com.example.bookclub.database.Book
import com.example.bookclub.databinding.FragmentAddBookBinding
import com.example.bookclub.viewmodels.BookViewModel
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class AddBookFragment : Fragment() {

    private var _binding: FragmentAddBookBinding? = null
    private val binding get() = _binding!!

    private val viewModel: BookViewModel by viewModels()
    private var selectedStatus = "Планирую"
    private var selectedCategoryId = 1
    private var selectedCoverUri: String? = null
    private var editingBookId: Int = -1   // -1 = режим добавления

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        Toast.makeText(
            requireContext(),
            if (isGranted) "Доступ к камере получен. Сканер в разработке." else "Разрешение не выдано",
            Toast.LENGTH_SHORT
        ).show()
    }

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            try {
                requireContext().contentResolver.takePersistableUriPermission(
                    it,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: SecurityException) {}
            selectedCoverUri = it.toString()
            binding.coverPreview.load(it)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddBookBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        editingBookId = arguments?.getInt("bookId") ?: -1

        setupStatusChips()
        setupGenreDropdown()

        binding.scanButton.setOnClickListener { requestCameraPermission() }
        binding.pickCoverButton.setOnClickListener { pickImageLauncher.launch(arrayOf("image/*")) }
        binding.saveButton.setOnClickListener { saveBook() }
        binding.cancelButton.setOnClickListener {
            if (editingBookId != -1) findNavController().navigateUp() else clearForm()
        }

        // Если пришли редактировать — загружаем данные книги
        if (editingBookId != -1) {
            binding.headerText.text = "Редактировать книгу"
            binding.saveButton.text = "Сохранить изменения"
            loadBookForEdit(editingBookId)
        }
    }

    private fun loadBookForEdit(bookId: Int) {
        viewModel.getBookById(bookId).observe(viewLifecycleOwner) { book ->
            binding.titleEditText.setText(book.title)
            binding.authorEditText.setText(book.author)
            selectedCoverUri = book.coverUri
            book.coverUri?.let { binding.coverPreview.load(it) }

            // Восстанавливаем жанр
            val genres = arrayOf("Роман", "Классика", "Детектив", "Сказка", "Фантастика")
            val genreIndex = (book.categoryId - 1).coerceIn(0, genres.size - 1)
            binding.genreDropdown.setText(genres[genreIndex], false)
            selectedCategoryId = book.categoryId

            // Статус
            selectedStatus = book.status
            when (book.status) {
                "Читаю" -> binding.chipReading.isChecked = true
                "Прочитано" -> binding.chipRead.isChecked = true
                else -> binding.chipPlanning.isChecked = true
            }
        }
    }

    private fun setupStatusChips() {
        binding.statusChipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            selectedStatus = when (checkedIds.firstOrNull()) {
                binding.chipReading.id -> "Читаю"
                binding.chipRead.id -> "Прочитано"
                else -> "Планирую"
            }
        }
    }

    private fun setupGenreDropdown() {
        val genres = arrayOf("Роман", "Классика", "Детектив", "Сказка", "Фантастика")
        binding.genreDropdown.setSimpleItems(genres)
        binding.genreDropdown.setText(genres[0], false)
        selectedCategoryId = 1

        binding.genreDropdown.setOnItemClickListener { _, _, position, _ ->
            selectedCategoryId = position + 1   //
        }

        binding.genreDropdown.setOnClickListener {
            binding.genreDropdown.showDropDown()
        }
    }

    private fun requestCameraPermission() {
        when {
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED -> {
                Toast.makeText(requireContext(), "Сканер в разработке", Toast.LENGTH_SHORT).show()
            }
            else -> cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun saveBook() {
        val title = binding.titleEditText.text.toString().trim()
        val author = binding.authorEditText.text.toString().trim()

        if (title.isEmpty()) {
            binding.titleInputLayout.error = "Введите название"
            return
        } else binding.titleInputLayout.error = null

        if (author.isEmpty()) {
            binding.authorInputLayout.error = "Введите автора"
            return
        } else binding.authorInputLayout.error = null

        // Показываем прогресс и блокируем кнопку
        binding.saveProgressBar.visibility = View.VISIBLE
        binding.saveButton.isEnabled = false

        if (editingBookId != -1) {
            viewModel.getBookById(editingBookId).observe(viewLifecycleOwner) { existing ->
                viewLifecycleOwner.lifecycleScope.launch {
                    val updated = existing.copy(
                        title = title,
                        author = author,
                        categoryId = selectedCategoryId,
                        status = selectedStatus,
                        coverUri = selectedCoverUri
                    )
                    viewModel.update(updated)

                    // Небольшая задержка для видимости анимации
                    kotlinx.coroutines.delay(300)

                    binding.saveProgressBar.visibility = View.GONE
                    binding.saveButton.isEnabled = true

                    Toast.makeText(requireContext(), "Изменения сохранены", Toast.LENGTH_SHORT).show()
                    findNavController().navigateUp()
                }
            }
        } else {
            viewLifecycleOwner.lifecycleScope.launch {
                val book = Book(
                    title = title,
                    author = author,
                    categoryId = selectedCategoryId,
                    status = selectedStatus,
                    coverUri = selectedCoverUri
                )
                viewModel.insert(book)

                kotlinx.coroutines.delay(300)

                binding.saveProgressBar.visibility = View.GONE
                binding.saveButton.isEnabled = true

                Toast.makeText(requireContext(), "Книга добавлена!", Toast.LENGTH_SHORT).show()
                clearForm()
            }
        }
    }

    private fun clearForm() {
        binding.titleEditText.text?.clear()
        binding.authorEditText.text?.clear()
        binding.chipPlanning.isChecked = true
        selectedCoverUri = null
        binding.coverPreview.setImageDrawable(null)
        binding.coverPreview.setBackgroundColor(Color.parseColor("#5C1A1B"))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
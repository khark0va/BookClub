package com.example.bookclub.ui

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.bookclub.adapters.BookAdapter
import com.example.bookclub.databinding.FragmentHomeBinding
import com.example.bookclub.viewmodels.BookViewModel
import com.example.bookclub.R

class BooksFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: BookViewModel by viewModels()
    private lateinit var adapter: BookAdapter

    // Текущие параметры фильтрации
    private var currentQuery = ""
    private var currentStatus = ""
    private var currentCategoryId = -1

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.seedCategoriesIfEmpty()
        setupRecyclerView()
        setupSearch()
        setupStatusFilters()
        observeBooks()
    }

    private fun setupRecyclerView() {
        adapter = BookAdapter { book ->
            val bundle = Bundle().apply { putInt("bookId", book.id) }
            findNavController().navigate(R.id.bookDetailFragment, bundle)
        }
        binding.booksRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.booksRecyclerView.adapter = adapter
    }

    private fun setupSearch() {
        binding.searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                currentQuery = s?.toString()?.trim() ?: ""
                applyFilters()
            }
        })
    }

    // Фильтр по статусу через чипсы
    private fun setupStatusFilters() {
        binding.statusFilterGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            currentStatus = when (checkedIds.firstOrNull()) {
                binding.chipPlanningFilter.id -> "Планирую"
                binding.chipReadingFilter.id -> "Читаю"
                binding.chipReadFilter.id -> "Прочитано"
                else -> ""
            }
            applyFilters()
        }
    }

    private fun applyFilters() {
        viewModel.setFilters(currentQuery, currentStatus, currentCategoryId)
    }

    private fun observeBooks() {
        viewModel.filteredBooks.observe(viewLifecycleOwner) { books ->
            // Скрываем прогресс-бар
            binding.progressBar.visibility = View.GONE

            if (books.isNullOrEmpty()) {
                binding.emptyStateLayout.visibility = View.VISIBLE
                binding.booksRecyclerView.visibility = View.GONE
            } else {
                binding.emptyStateLayout.visibility = View.GONE
                binding.booksRecyclerView.visibility = View.VISIBLE
                adapter.submitList(books)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
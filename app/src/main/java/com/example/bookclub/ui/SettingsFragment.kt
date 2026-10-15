package com.example.bookclub.ui

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import com.example.bookclub.databinding.FragmentSettingsBinding

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private val prefs by lazy {
        requireContext().getSharedPreferences("settings", Context.MODE_PRIVATE)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadSettings()
        setupListeners()
    }

    private fun loadSettings() {
        val isDarkTheme = prefs.getBoolean("dark_theme", false)
        val sortOrder = prefs.getString("sort_order", "title") ?: "title"
        val notifications = prefs.getBoolean("notifications", true)
        val notifText = if (notifications) "Время: 19:00" else "Выключено"
        binding.notificationTimeText.text = notifText
        binding.themeSwitch.isChecked = isDarkTheme
        binding.notificationsSwitch.isChecked = notifications
        binding.sortValueText.text = sortOrderToLabel(sortOrder)
    }

    private fun setupListeners() {
        binding.themeSwitch.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("dark_theme", isChecked).apply()
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
        }

        binding.notificationsSwitch.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("notifications", isChecked).apply()
            val text = if (isChecked) "Время: 19:00" else "Выключено"
            binding.notificationTimeText.text = text
        }

        binding.sortCard.setOnClickListener { showSortDialog() }
    }

    private fun showSortDialog() {
        val options = arrayOf("По названию", "По автору", "По статусу")
        val keys = arrayOf("title", "author", "status")
        val currentSort = prefs.getString("sort_order", "title") ?: "title"
        val checkedItem = keys.indexOf(currentSort).coerceAtLeast(0)

        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Сортировка книг")
            .setSingleChoiceItems(options, checkedItem) { dialog, which ->
                prefs.edit().putString("sort_order", keys[which]).apply()
                binding.sortValueText.text = options[which]
                dialog.dismiss()
            }
            .show()
    }

    private fun sortOrderToLabel(key: String): String = when (key) {
        "author" -> "По автору"
        "status" -> "По статусу"
        else -> "По названию"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
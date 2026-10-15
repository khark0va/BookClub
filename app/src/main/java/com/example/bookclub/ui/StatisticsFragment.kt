package com.example.bookclub.ui

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.bookclub.database.Book
import com.example.bookclub.database.Category
import com.example.bookclub.databinding.FragmentStatisticsBinding
import com.example.bookclub.viewmodels.BookViewModel
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import kotlinx.coroutines.launch

class StatisticsFragment : Fragment() {

    private var _binding: FragmentStatisticsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: BookViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStatisticsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadStatistics()
    }

    private fun loadStatistics() {
        lifecycleScope.launch {
            val books = viewModel.getAllBooksOnce()
            val categories = viewModel.getAllCategoriesOnce()

            if (books.isEmpty()) {
                binding.emptyStatsText.visibility = View.VISIBLE
                binding.statusPieChart.visibility = View.GONE
                binding.genreBarChart.visibility = View.GONE
                return@launch
            }

            binding.emptyStatsText.visibility = View.GONE
            binding.statusPieChart.visibility = View.VISIBLE
            binding.genreBarChart.visibility = View.VISIBLE

            updateNumberCards(books)
            setupPieChart(books)
            setupBarChart(books, categories)
        }
    }

    // Числовые показатели
    private fun updateNumberCards(books: List<Book>) {
        binding.totalBooksText.text = books.size.toString()

        val readCount = books.count { it.status == "Прочитано" }
        binding.readBooksText.text = readCount.toString()

        val rated = books.filter { it.rating != null && it.rating > 0 }
        val avg = if (rated.isEmpty()) 0.0 else rated.map { it.rating!! }.average()
        binding.avgRatingText.text = String.format("%.1f", avg)
    }

    private fun setupPieChart(books: List<Book>) {
        val planning = books.count { it.status == "Планирую" }.toFloat()
        val reading = books.count { it.status == "Читаю" }.toFloat()
        val read = books.count { it.status == "Прочитано" }.toFloat()

        val entries = mutableListOf<PieEntry>()
        if (planning > 0) entries.add(PieEntry(planning, "Планирую"))
        if (reading > 0) entries.add(PieEntry(reading, "Читаю"))
        if (read > 0) entries.add(PieEntry(read, "Прочитано"))

        val dataSet = PieDataSet(entries, "")
        dataSet.colors = listOf(
            Color.parseColor("#C9A227"),
            Color.parseColor("#8B5A2B"),
            Color.parseColor("#5C1A1B")
        )
        // Убираем цифры с самого круга — они вылезали
        dataSet.setDrawValues(false)

        val data = PieData(dataSet)

        binding.statusPieChart.apply {
            this.data = data
            description.isEnabled = false
            setUsePercentValues(false)
            setDrawEntryLabels(false)          // не рисуем подписи у секторов
            setDrawSlicesUnderHole(false)
            isDrawHoleEnabled = true
            holeRadius = 55f
            transparentCircleRadius = 60f
            setHoleColor(Color.TRANSPARENT)
            centerText = "Всего:\n${books.size}"
            setCenterTextSize(16f)
            setCenterTextColor(Color.parseColor("#5C1A1B"))
            setCenterTextTypeface(android.graphics.Typeface.DEFAULT_BOLD)
            legend.isEnabled = true
            legend.textSize = 12f
            legend.textColor = Color.parseColor("#333333")
            legend.verticalAlignment = com.github.mikephil.charting.components.Legend.LegendVerticalAlignment.CENTER
            legend.horizontalAlignment = com.github.mikephil.charting.components.Legend.LegendHorizontalAlignment.RIGHT
            legend.orientation = com.github.mikephil.charting.components.Legend.LegendOrientation.VERTICAL
            legend.setDrawInside(false)
            legend.xEntrySpace = 8f
            legend.form = com.github.mikephil.charting.components.Legend.LegendForm.CIRCLE
            legend.formSize = 10f
            setExtraOffsets(0f, 0f, 30f, 0f)
        }
    }

    private fun setupBarChart(books: List<Book>, categories: List<Category>) {
        val categoryCounts = mutableMapOf<String, Int>()

        books.forEach { book ->
            val category = categories.find { it.id == book.categoryId }
            val name = category?.name ?: "Без жанра"
            categoryCounts[name] = (categoryCounts[name] ?: 0) + 1
        }

        val entries = categoryCounts.entries.mapIndexed { index, entry ->
            BarEntry(index.toFloat(), entry.value.toFloat())
        }

        val dataSet = BarDataSet(entries, "Книги по жанрам")
        dataSet.color = Color.parseColor("#5C1A1B")
        dataSet.valueTextSize = 12f
        dataSet.valueTextColor = Color.parseColor("#333333")

        val data = BarData(dataSet)
        data.barWidth = 0.6f

        binding.genreBarChart.apply {
            this.data = data
            description.isEnabled = false
            xAxis.valueFormatter =
                com.github.mikephil.charting.formatter.IndexAxisValueFormatter(categoryCounts.keys.toList())
            xAxis.granularity = 1f
            xAxis.textSize = 11f
            xAxis.setDrawGridLines(false)
            xAxis.position = com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM
            axisLeft.axisMinimum = 0f
            axisLeft.granularity = 1f
            axisLeft.setDrawGridLines(false)
            axisRight.isEnabled = false
            legend.isEnabled = false
            animateY(800)
            invalidate()
        }
    }
}
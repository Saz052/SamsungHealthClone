package com.samsunghealthclone

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import com.samsunghealthclone.data.StepEntry
import com.samsunghealthclone.databinding.ActivityMainBinding
import com.samsunghealthclone.ui.MainViewModel
import com.samsunghealthclone.ui.StepsAdapter

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: MainViewModel
    private lateinit var stepsAdapter: StepsAdapter
    private var tapCount = 0
    private var lastTapTime = 0L

    private val activityRecognitionPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            StepCounterService.startService(this)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this)[MainViewModel::class.java]

        setupToolbar()
        setupObservers()
        setupChart()
        setupRecyclerView()
        setupClickListeners()

        requestActivityRecognitionAndStartTracking()
    }

    private fun requestActivityRecognitionAndStartTracking() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
            checkSelfPermission(Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED
        ) {
            activityRecognitionPermission.launch(Manifest.permission.ACTIVITY_RECOGNITION)
        } else {
            StepCounterService.startService(this)
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_secret -> {
                openSecretMenu()
                true
            }
            R.id.action_refresh -> {
                viewModel.loadTodayData()
                viewModel.loadWeeklyData()
                viewModel.loadMonthlyData()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun openSecretMenu() {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastTapTime < 1000) {
            tapCount++
            if (tapCount >= 5) {
                tapCount = 0
                startActivity(Intent(this, SecretMenuActivity::class.java))
            }
        } else {
            tapCount = 1
        }
        lastTapTime = currentTime
    }

    private fun setupObservers() {
        viewModel.todayEntry.observe(this) { entry ->
            updateTodayUI(entry)
        }

        viewModel.weeklyData.observe(this) { data ->
            updateWeeklyChart(data)
            stepsAdapter.submitList(data)
        }

        viewModel.weeklyTotal.observe(this) { total ->
            binding.tvWeeklyTotal.text = formatNumber(total)
        }

        viewModel.weeklyAverage.observe(this) { avg ->
            binding.tvWeeklyAvg.text = String.format("%.0f", avg)
        }

        viewModel.monthlyTotal.observe(this) { total ->
            binding.tvMonthlyTotal.text = formatNumber(total)
        }

        viewModel.monthlyAverage.observe(this) { avg ->
            binding.tvMonthlyAvg.text = String.format("%.0f", avg)
        }
    }

    private fun updateTodayUI(entry: StepEntry?) {
        val steps = entry?.steps ?: 0
        val hours = viewModel.getHoursWalked(steps)
        val distance = viewModel.getDistanceKm(steps)
        val calories = viewModel.getCalories(steps)

        binding.tvStepsCount.text = formatNumber(steps)
        binding.tvHoursWalked.text = String.format("%.1f ч", hours)
        binding.tvDistance.text = String.format("%.2f км", distance)
        binding.tvCalories.text = String.format("%.0f ккал", calories)

        // Update progress (goal: 10000 steps)
        val progress = (steps / 10000f * 100).coerceAtMost(100f)
        binding.progressSteps.progress = progress.toInt()
        binding.tvStepsGoal.text = "$steps / 10 000"
    }

    private fun setupChart() {
        binding.barChart.apply {
            description = Description().apply { text = "" }
            setDrawGridBackground(false)
            setDrawBarShadow(false)
            setHighlightFullBarEnabled(false)
            setPinchZoom(false)
            setDoubleTapToZoomEnabled(false)
            legend.isEnabled = false
            animateY(1000)

            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                granularity = 1f
                labelCount = 7
                textSize = 10f
            }

            axisLeft.apply {
                setDrawGridLines(true)
                gridColor = resources.getColor(R.color.chart_grid)
                axisMinimum = 0f
                textSize = 10f
            }

            axisRight.isEnabled = false
        }
    }

    private fun updateWeeklyChart(data: List<StepEntry>) {
        val entries = data.reversed().mapIndexed { index, entry ->
            BarEntry(index.toFloat(), entry.steps.toFloat())
        }

        val labels = data.reversed().map { viewModel.formatDate(it.date) }

        val dataSet = BarDataSet(entries, "Steps").apply {
            setColor(resources.getColor(R.color.primary))
            valueTextSize = 10f
            valueTextColor = resources.getColor(R.color.text_primary)
        }

        binding.barChart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        binding.barChart.data = BarData(dataSet)
        binding.barChart.invalidate()
    }

    private fun setupRecyclerView() {
        stepsAdapter = StepsAdapter { entry, position ->
            viewModel.setSelectedDate(entry.date)
            scrollToDate(entry.date)
        }
        binding.rvHistory.apply {
            layoutManager = LinearLayoutManager(this@MainActivity, LinearLayoutManager.HORIZONTAL, false)
            adapter = stepsAdapter
        }
    }

    private fun scrollToDate(date: Long) {
        // Find position and scroll
        val data = stepsAdapter.currentList
        val position = data.indexOfFirst { it.date == date }
        if (position >= 0) {
            binding.rvHistory.smoothScrollToPosition(position)
        }
    }

    private fun setupClickListeners() {
        binding.btnAddSteps.setOnClickListener {
            viewModel.addSteps(100)
        }

        binding.tvDateHeader.setOnClickListener {
            // Could open date picker
        }
    }

    private fun formatNumber(number: Int): String {
        return if (number >= 10000) {
            String.format("%.1fк", number / 1000f).replace(".0", "")
        } else {
            number.toString()
        }
    }
}
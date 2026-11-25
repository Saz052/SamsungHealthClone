package com.samsunghealthclone

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.samsunghealthclone.databinding.ActivitySecretMenuBinding
import com.samsunghealthclone.ui.SecretMenuViewModel

class SecretMenuActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySecretMenuBinding
    private lateinit var viewModel: SecretMenuViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySecretMenuBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this)[SecretMenuViewModel::class.java]

        setupToolbar()
        setupObservers()
        setupClickListeners()
        setupInputListeners()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupObservers() {
        viewModel.currentSteps.observe(this) { steps ->
            binding.tvCurrentSteps.text = formatNumber(steps)
        }

        viewModel.targetSteps.observe(this) { steps ->
            binding.etTargetSteps.setText(steps.toString())
            binding.seekSteps.progress = (steps / 50000f * 100).coerceIn(0, 100).toInt()
        }

        viewModel.hoursWalked.observe(this) { hours ->
            binding.tvHoursWalked.text = String.format("%.1f ч", hours)
            binding.etHoursWalked.setText(String.format("%.1f", hours))
            binding.seekHours.progress = (hours / 8.33f * 100).coerceIn(0, 100).toInt() // 8.33h = 50000 steps
        }

        viewModel.isLoading.observe(this) { loading ->
            binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
            binding.btnApply.isEnabled = !loading
            binding.btnRandom.isEnabled = !loading
            binding.btnMax.isEnabled = !loading
            binding.btnReset.isEnabled = !loading
            binding.btnCopyYesterday.isEnabled = !loading
        }

        viewModel.message.observe(this) { msg ->
            if (msg.isNotEmpty()) {
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupClickListeners() {
        binding.btnApply.setOnClickListener {
            viewModel.applyChanges()
        }

        binding.btnRandom.setOnClickListener {
            viewModel.addRandomSteps()
        }

        binding.btnMax.setOnClickListener {
            viewModel.setMaxSteps()
        }

        binding.btnReset.setOnClickListener {
            viewModel.resetToZero()
        }

        binding.btnCopyYesterday.setOnClickListener {
            viewModel.copyYesterdaySteps()
        }

        binding.btnToday.setOnClickListener {
            viewModel.setSelectedDate(com.samsunghealthclone.data.StepEntry.getDayStart())
        }

        binding.btnYesterday.setOnClickListener {
            val cal = java.util.Calendar.getInstance()
            cal.timeInMillis = com.samsunghealthclone.data.StepEntry.getDayStart()
            cal.add(java.util.Calendar.DAY_OF_YEAR, -1)
            viewModel.setSelectedDate(cal.timeInMillis)
        }
    }

    private fun setupInputListeners() {
        binding.etTargetSteps.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val text = binding.etTargetSteps.text.toString()
                if (text.isNotEmpty()) {
                    val steps = text.toIntOrNull() ?: 0
                    viewModel.setTargetSteps(steps.coerceIn(0, 50000))
                }
            }
        }

        binding.etHoursWalked.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val text = binding.etHoursWalked.text.toString()
                if (text.isNotEmpty()) {
                    val hours = text.toFloatOrNull() ?: 0f
                    viewModel.setHoursWalked(hours.coerceIn(0f, 8.33f))
                }
            }
        }

        binding.seekSteps.setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    val steps = (progress / 100f * 50000).toInt()
                    viewModel.setTargetSteps(steps)
                }
            }
            override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) {}
        })

        binding.seekHours.setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    val hours = progress / 100f * 8.33f
                    viewModel.setHoursWalked(hours)
                }
            }
            override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) {}
        })
    }

    private fun formatNumber(number: Int): String {
        return if (number >= 1000) {
            String.format("%,d", number).replace(",", " ")
        } else {
            number.toString()
        }
    }
}
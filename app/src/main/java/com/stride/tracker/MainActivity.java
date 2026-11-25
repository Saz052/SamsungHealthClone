package com.stride.tracker;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public final class MainActivity extends Activity {
    private static final int ACTIVITY_PERMISSION_REQUEST = 12;
    private static final int DAILY_GOAL = 10_000;
    private static final int BACKGROUND = Color.rgb(239, 244, 240);
    private static final int INK = Color.rgb(31, 48, 42);
    private static final int MUTED = Color.rgb(112, 130, 120);
    private static final int GREEN = Color.rgb(27, 153, 122);

    private TextView countView;
    private TextView timeView;
    private TextView distanceView;
    private TextView energyView;
    private LinearLayout history;
    private ProgressBar progress;
    private int titleTaps;
    private long lastTitleTap;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(BACKGROUND);
        getWindow().setNavigationBarColor(BACKGROUND);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }
        setContentView(createScreen());
        refresh();
        requestActivityPermission();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    private View createScreen() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(BACKGROUND);

        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(20), dp(16), dp(20), dp(28));
        scroll.addView(page, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = text("Шаги", 27, INK, true);
        title.setOnClickListener(view -> onTitleTap());
        title.setContentDescription("Шаги. Пять нажатий открывают настройки тестовых данных");
        header.addView(title, new LinearLayout.LayoutParams(0, dp(52), 1));
        header.addView(text("АКТИВНОСТЬ", 11, MUTED, true));
        page.addView(header);
        page.addView(text("Двигайтесь в своём ритме.", 14, MUTED, false), top(4));

        LinearLayout today = column();
        today.setPadding(dp(22), dp(22), dp(22), dp(20));
        today.setBackgroundColor(Color.rgb(20, 54, 45));
        page.addView(today, top(18));
        today.addView(text("СЕГОДНЯ", 12, Color.rgb(183, 214, 200), true));
        countView = text("0", 52, Color.WHITE, true);
        today.addView(countView, top(6));
        today.addView(text("из 10 000 шагов", 14, Color.rgb(205, 226, 215), false), top(2));
        progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progress.setMax(100);
        progress.setProgressTintList(android.content.res.ColorStateList.valueOf(Color.rgb(82, 211, 166)));
        progress.setProgressBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.rgb(59, 91, 77)));
        today.addView(progress, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(8)));
        today.addView(text("Цель на день", 12, Color.rgb(183, 214, 200), false), top(10));

        page.addView(text("Ваши показатели", 18, INK, true), top(26));
        LinearLayout stats = new LinearLayout(this);
        stats.setOrientation(LinearLayout.HORIZONTAL);
        page.addView(stats, top(12));
        timeView = addStat(stats, "ВРЕМЯ", "0 ч", true);
        distanceView = addStat(stats, "РАССТОЯНИЕ", "0 км", false);
        energyView = addStat(stats, "ЭНЕРГИЯ", "0 ккал", false);

        page.addView(text("По дням", 18, INK, true), top(26));
        history = column();
        history.setPadding(dp(16), dp(4), dp(16), dp(4));
        history.setBackgroundColor(Color.WHITE);
        page.addView(history, top(12));
        page.addView(text("Время, расстояние и калории рассчитаны приблизительно.", 12, MUTED, false), top(14));
        return scroll;
    }

    private TextView addStat(LinearLayout row, String label, String value, boolean first) {
        LinearLayout card = column();
        card.setPadding(dp(10), dp(13), dp(8), dp(13));
        card.setBackgroundColor(Color.WHITE);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(82), 1);
        if (!first) params.leftMargin = dp(8);
        row.addView(card, params);
        TextView name = text(label, 9, MUTED, true);
        card.addView(name);
        TextView amount = text(value, 16, INK, true);
        card.addView(amount, top(8));
        return amount;
    }

    private void refresh() {
        if (countView == null) return;
        int steps = StepStore.todaySteps(this);
        countView.setText(NumberFormat.getIntegerInstance().format(steps));
        progress.setProgress(Math.min(100, Math.round(steps * 100f / DAILY_GOAL)));
        timeView.setText(String.format(Locale.getDefault(), "%.1f ч", StepStore.hoursForSteps(steps)));
        distanceView.setText(String.format(Locale.getDefault(), "%.2f км", StepStore.distanceKmForSteps(steps)));
        energyView.setText(NumberFormat.getIntegerInstance().format(StepStore.caloriesForSteps(steps)) + " ккал");

        history.removeAllViews();
        for (int i = 0; i < 7; i++) {
            Calendar day = StepStore.dayOffset(-i);
            LinearLayout line = new LinearLayout(this);
            line.setGravity(Gravity.CENTER_VERTICAL);
            line.setPadding(0, dp(11), 0, dp(11));
            String date = i == 0 ? "Сегодня" : StepStore.displayDate(day);
            line.addView(text(date, 14, INK, i == 0), new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
            line.addView(text(NumberFormat.getIntegerInstance().format(StepStore.getSteps(this, day)),
                    14, i == 0 ? GREEN : MUTED, true));
            history.addView(line);
            if (i < 6) {
                View divider = new View(this);
                divider.setBackgroundColor(Color.rgb(233, 239, 235));
                history.addView(divider, new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, dp(1)));
            }
        }
    }

    private void requestActivityPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                && checkSelfPermission(Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACTIVITY_RECOGNITION}, ACTIVITY_PERMISSION_REQUEST);
        } else {
            startStepService();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == ACTIVITY_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startStepService();
            } else {
                Toast.makeText(this, "Разрешите распознавание активности для подсчёта шагов.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void startStepService() {
        SensorManager manager = (SensorManager) getSystemService(SENSOR_SERVICE);
        if (manager == null || manager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR) == null) {
            Toast.makeText(this, "Датчик шагов не найден. Тестовые данные доступны в скрытых настройках.", Toast.LENGTH_LONG).show();
            return;
        }
        Intent service = new Intent(this, StepCounterService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(service);
        else startService(service);
    }

    private void onTitleTap() {
        long now = System.currentTimeMillis();
        if (now - lastTitleTap > 1400L) titleTaps = 0;
        lastTitleTap = now;
        if (++titleTaps >= 5) {
            titleTaps = 0;
            showTestSettings();
        }
    }

    private void showTestSettings() {
        Calendar selectedDay = StepStore.today();
        EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setSingleLine(true);
        input.setHint("Количество шагов");
        input.setText(String.valueOf(StepStore.getSteps(this, selectedDay)));
        input.setSelection(input.length());

        int padding = dp(20);
        LinearLayout container = column();
        container.setPadding(padding, dp(8), padding, 0);
        TextView dateButton = text("Дата: " + formatDate(selectedDay), 16, GREEN, true);
        dateButton.setPadding(0, dp(8), 0, dp(12));
        dateButton.setOnClickListener(view -> {
            DatePickerDialog picker = new DatePickerDialog(this, (datePicker, year, month, dayOfMonth) -> {
                selectedDay.set(year, month, dayOfMonth, 0, 0, 0);
                selectedDay.set(Calendar.MILLISECOND, 0);
                dateButton.setText("Дата: " + formatDate(selectedDay));
                input.setText(String.valueOf(StepStore.getSteps(this, selectedDay)));
                input.setSelection(input.length());
            }, selectedDay.get(Calendar.YEAR), selectedDay.get(Calendar.MONTH),
                    selectedDay.get(Calendar.DAY_OF_MONTH));
            picker.getDatePicker().setMaxDate(System.currentTimeMillis());
            picker.show();
        });
        container.addView(dateButton);
        container.addView(input);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Изменить шаги за дату")
                .setMessage("Выберите день и укажите общее количество шагов. Изменения хранятся только в приложении.")
                .setView(container)
                .setNegativeButton("Отмена", null)
                .setPositiveButton("Сохранить", (d, which) -> {
                    int steps;
                    try {
                        steps = Integer.parseInt(input.getText().toString().trim());
                    } catch (NumberFormatException ignored) {
                        steps = 0;
                    }
                    StepStore.setSteps(this, selectedDay, Math.max(0, Math.min(200_000, steps)));
                    refresh();
                })
                .create();
        dialog.show();
    }

    private String formatDate(Calendar day) {
        return new SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(day.getTime());
    }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
    }

    private LinearLayout column() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        return layout;
    }

    private LinearLayout.LayoutParams top(int marginDp) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.topMargin = dp(marginDp);
        return params;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}

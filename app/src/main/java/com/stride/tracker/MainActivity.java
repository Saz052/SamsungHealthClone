package com.stride.tracker;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.NumberFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.Random;

public final class MainActivity extends Activity {
    private static final int ACTIVITY_PERMISSION_REQUEST = 12;
    private static final int DAILY_GOAL = 10_000;
    private static final int PINE = Color.rgb(20, 54, 45);
    private static final int INK = Color.rgb(31, 48, 42);
    private static final int MUTED = Color.rgb(112, 130, 120);
    private static final int CANVAS = Color.rgb(239, 244, 240);
    private static final int MINT = Color.rgb(27, 153, 122);
    private static final int PALE_MINT = Color.rgb(222, 240, 232);
    private static final int WHITE = Color.WHITE;

    private TextView countView;
    private TextView goalView;
    private TextView hoursView;
    private TextView distanceView;
    private TextView caloriesView;
    private TextView weekTotalView;
    private TextView weekAverageView;
    private LinearLayout historyContainer;
    private WeekChart weekChart;
    private SharedPreferences preferences;
    private int brandTaps;
    private long lastBrandTap;

    private final SharedPreferences.OnSharedPreferenceChangeListener preferenceListener = (prefs, key) -> {
        if (key == null || key.startsWith("steps_")) updateDashboard();
    };

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        preferences = StepStore.preferences(this);
        getWindow().setStatusBarColor(CANVAS);
        getWindow().setNavigationBarColor(CANVAS);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getWindow().getDecorView().setSystemUiVisibility(
                    getWindow().getDecorView().getSystemUiVisibility() | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        }
        setContentView(buildDashboard());
        updateDashboard();
        askForActivityPermission();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (preferences != null) {
            preferences.registerOnSharedPreferenceChangeListener(preferenceListener);
            updateDashboard();
        }
    }

    @Override
    protected void onPause() {
        if (preferences != null) preferences.unregisterOnSharedPreferenceChangeListener(preferenceListener);
        super.onPause();
    }

    private View buildDashboard() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(CANVAS);

        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(20), dp(12), dp(20), dp(32));
        scroll.addView(page, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setOrientation(LinearLayout.HORIZONTAL);
        TextView title = text("Шаги", 27, INK, true);
        title.setOnClickListener(v -> registerSecretTap());
        title.setContentDescription("Шаги. Нажмите пять раз для инструментов разработчика");
        header.addView(title, new LinearLayout.LayoutParams(0, dp(52), 1));
        TextView dayLabel = text("АКТИВНОСТЬ", 11, MUTED, true);
        dayLabel.setLetterSpacing(0.08f);
        header.addView(dayLabel);
        page.addView(header);

        TextView intro = text("Двигайтесь в своём ритме.", 14, MUTED, false);
        intro.setPadding(0, 0, 0, dp(18));
        page.addView(intro);

        LinearLayout hero = column();
        hero.setPadding(dp(22), dp(22), dp(22), dp(20));
        hero.setBackground(rounded(PINE, 24));
        page.addView(hero, matchWrap());

        TextView today = text("СЕГОДНЯ", 12, Color.rgb(183, 214, 200), true);
        today.setLetterSpacing(0.1f);
        hero.addView(today);
        countView = text("0", 52, WHITE, true);
        countView.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        countView.setPadding(0, dp(7), 0, 0);
        hero.addView(countView);
        goalView = text("из 10 000 шагов", 14, Color.rgb(205, 226, 215), false);
        goalView.setPadding(0, 0, 0, dp(18));
        hero.addView(goalView);

        ProgressBar goalBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        goalBar.setMax(100);
        goalBar.setProgressTintList(android.content.res.ColorStateList.valueOf(Color.rgb(82, 211, 166)));
        goalBar.setProgressBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.rgb(59, 91, 77)));
        goalBar.setProgress(0);
        goalBar.setTag("goal_bar");
        hero.addView(goalBar, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(7)));

        TextView goalNote = text("Цель на день", 12, Color.rgb(183, 214, 200), false);
        goalNote.setPadding(0, dp(12), 0, 0);
        hero.addView(goalNote);

        TextView statsTitle = sectionTitle("Ваши показатели");
        page.addView(statsTitle, marginTop(26));
        LinearLayout stats = new LinearLayout(this);
        stats.setOrientation(LinearLayout.HORIZONTAL);
        stats.setGravity(Gravity.CENTER_VERTICAL);
        stats.setPadding(0, dp(12), 0, 0);
        page.addView(stats, matchWrap());
        hoursView = addStat(stats, "ВРЕМЯ", "0 ч", true);
        distanceView = addStat(stats, "РАССТОЯНИЕ", "0 км", false);
        caloriesView = addStat(stats, "ЭНЕРГИЯ", "0 ккал", false);

        LinearLayout summary = new LinearLayout(this);
        summary.setOrientation(LinearLayout.HORIZONTAL);
        summary.setGravity(Gravity.CENTER_VERTICAL);
        summary.setPadding(dp(18), dp(16), dp(18), dp(16));
        summary.setBackground(rounded(PALE_MINT, 16));
        LinearLayout.LayoutParams summaryParams = marginTop(14);
        page.addView(summary, summaryParams);
        LinearLayout weekTotalBox = column();
        summary.addView(weekTotalBox, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        weekTotalView = text("0", 21, INK, true);
        weekTotalBox.addView(weekTotalView);
        TextView totalLabel = text("шагов за 7 дней", 12, MUTED, false);
        totalLabel.setPadding(0, dp(3), 0, 0);
        weekTotalBox.addView(totalLabel);
        LinearLayout averageBox = column();
        averageBox.setGravity(Gravity.END);
        summary.addView(averageBox);
        weekAverageView = text("0", 21, INK, true);
        averageBox.addView(weekAverageView);
        TextView averageLabel = text("в среднем в день", 12, MUTED, false);
        averageLabel.setPadding(0, dp(3), 0, 0);
        averageBox.addView(averageLabel);

        page.addView(sectionTitle("Ритм недели"), marginTop(27));
        weekChart = new WeekChart(this);
        LinearLayout chartCard = column();
        chartCard.setPadding(dp(12), dp(12), dp(12), dp(8));
        chartCard.setBackground(rounded(WHITE, 18));
        chartCard.addView(weekChart, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(170)));
        page.addView(chartCard, marginTop(12));

        page.addView(sectionTitle("По дням"), marginTop(26));
        historyContainer = column();
        historyContainer.setPadding(dp(16), dp(5), dp(16), dp(5));
        historyContainer.setBackground(rounded(WHITE, 18));
        page.addView(historyContainer, marginTop(12));

        TextView estimate = text("Время и расстояние рассчитаны приблизительно по числу шагов.", 12, MUTED, false);
        estimate.setPadding(dp(2), dp(15), dp(2), 0);
        page.addView(estimate);
        return scroll;
    }

    private TextView addStat(LinearLayout row, String label, String initialValue, boolean first) {
        LinearLayout card = column();
        card.setPadding(dp(12), dp(14), dp(8), dp(14));
        card.setBackground(rounded(WHITE, 16));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(83), 1);
        if (!first) params.leftMargin = dp(8);
        row.addView(card, params);
        TextView name = text(label, 9, MUTED, true);
        name.setLetterSpacing(0.04f);
        card.addView(name);
        TextView value = text(initialValue, 17, INK, true);
        value.setPadding(0, dp(8), 0, 0);
        card.addView(value);
        return value;
    }

    private void updateDashboard() {
        if (countView == null) return;
        int today = StepStore.todaySteps(this);
        countView.setText(NumberFormat.getIntegerInstance(Locale.getDefault()).format(today));
        goalView.setText("из " + NumberFormat.getIntegerInstance(Locale.getDefault()).format(DAILY_GOAL) + " шагов");
        float hours = StepStore.hoursForSteps(today);
        hoursView.setText(String.format(Locale.getDefault(), "%.1f ч", hours));
        distanceView.setText(String.format(Locale.getDefault(), "%.2f км", StepStore.distanceKmForSteps(today)));
        caloriesView.setText(NumberFormat.getIntegerInstance(Locale.getDefault()).format(StepStore.caloriesForSteps(today)) + " ккал");

        View hero = (View) countView.getParent();
        ProgressBar progressBar = hero.findViewWithTag("goal_bar");
        if (progressBar != null) progressBar.setProgress(Math.min(100, Math.round(today * 100f / DAILY_GOAL)));

        int total = 0;
        int[] sevenDays = new int[7];
        for (int i = 0; i < 7; i++) {
            Calendar day = StepStore.dayOffset(i - 6);
            sevenDays[i] = StepStore.getSteps(this, day);
            total += sevenDays[i];
        }
        weekTotalView.setText(NumberFormat.getIntegerInstance(Locale.getDefault()).format(total));
        weekAverageView.setText(NumberFormat.getIntegerInstance(Locale.getDefault()).format(total / 7));
        weekChart.setValues(sevenDays);
        updateHistory();
    }

    private void updateHistory() {
        historyContainer.removeAllViews();
        for (int i = 0; i < 7; i++) {
            Calendar day = StepStore.dayOffset(-i);
            int steps = StepStore.getSteps(this, day);
            LinearLayout row = new LinearLayout(this);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(0, dp(11), 0, dp(11));
            TextView date = text(i == 0 ? "Сегодня" : StepStore.displayDate(day), 14, INK, i == 0);
            row.addView(date, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
            TextView count = text(NumberFormat.getIntegerInstance(Locale.getDefault()).format(steps), 14, i == 0 ? MINT : MUTED, true);
            row.addView(count);
            historyContainer.addView(row);
            if (i < 6) {
                View divider = new View(this);
                divider.setBackgroundColor(Color.rgb(233, 239, 235));
                historyContainer.addView(divider, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1)));
            }
        }
    }

    private void askForActivityPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                checkSelfPermission(Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED) {
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
                Toast.makeText(this, "Разрешите распознавание активности, чтобы считать реальные шаги.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void startStepService() {
        SensorManager sensors = (SensorManager) getSystemService(SENSOR_SERVICE);
        if (sensors == null || sensors.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR) == null) {
            Toast.makeText(this, "На этом устройстве нет датчика шагов. Показатели можно менять в скрытом меню.", Toast.LENGTH_LONG).show();
            return;
        }
        Intent service = new Intent(this, StepCounterService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(service);
        else startService(service);
    }

    private void registerSecretTap() {
        long now = System.currentTimeMillis();
        if (now - lastBrandTap > 1400) brandTaps = 0;
        lastBrandTap = now;
        brandTaps++;
        if (brandTaps >= 5) {
            brandTaps = 0;
            showSecretMenu();
        }
    }

    private void showSecretMenu() {
        LinearLayout form = column();
        form.setPadding(dp(22), dp(8), dp(22), dp(10));
        TextView explanation = text("Локальная настройка тестовых показателей", 13, MUTED, false);
        form.addView(explanation, marginBottom(14));
        EditText stepsInput = input("Количество шагов", InputType.TYPE_CLASS_NUMBER);
        EditText hoursInput = input("Часы ходьбы", InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        form.addView(stepsInput, marginBottom(10));
        form.addView(hoursInput, marginBottom(12));

        int initial = StepStore.todaySteps(this);
        stepsInput.setText(String.valueOf(initial));
        hoursInput.setText(String.format(Locale.US, "%.1f", StepStore.hoursForSteps(initial)));
        boolean[] syncing = {false};
        stepsInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (syncing[0]) return;
                syncing[0] = true;
                int value = parseInt(s.toString());
                hoursInput.setText(String.format(Locale.US, "%.1f", StepStore.hoursForSteps(value)));
                hoursInput.setSelection(hoursInput.length());
                syncing[0] = false;
            }
            @Override public void afterTextChanged(Editable s) {}
        });
        hoursInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (syncing[0]) return;
                syncing[0] = true;
                float value = parseFloat(s.toString());
                stepsInput.setText(String.valueOf(Math.max(0, Math.min(200_000, Math.round(value * 6000f)))));
                stepsInput.setSelection(stepsInput.length());
                syncing[0] = false;
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        TextView quickLabel = text("Быстрые действия", 12, MUTED, true);
        quickLabel.setPadding(0, 0, 0, dp(8));
        form.addView(quickLabel);
        addQuickAction(form, "Добавить 1 000 шагов", () -> setInputSteps(stepsInput, StepStore.todaySteps(this) + 1000));
        addQuickAction(form, "Добавить случайно 1 000–5 000", () -> setInputSteps(stepsInput,
                StepStore.todaySteps(this) + 1000 + new Random().nextInt(4001)));
        addQuickAction(form, "Установить 50 000", () -> setInputSteps(stepsInput, 50_000));
        addQuickAction(form, "Сбросить на ноль", () -> setInputSteps(stepsInput, 0));

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Скрытые инструменты")
                .setView(form)
                .setNegativeButton("Отмена", null)
                .setPositiveButton("Сохранить", null)
                .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            int value = parseInt(stepsInput.getText().toString());
            StepStore.setSteps(this, StepStore.today(), value);
            updateDashboard();
            dialog.dismiss();
            Toast.makeText(this, "Показатели обновлены", Toast.LENGTH_SHORT).show();
        }));
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(MINT);
    }

    private void addQuickAction(LinearLayout parent, String label, Runnable action) {
        TextView button = text(label, 14, MINT, true);
        button.setGravity(Gravity.CENTER_VERTICAL);
        button.setPadding(dp(12), 0, dp(12), 0);
        button.setBackground(rounded(PALE_MINT, 11));
        parent.addView(button, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(42)));
        LinearLayout.LayoutParams gap = new LinearLayout.LayoutParams(1, dp(7));
        parent.addView(new View(this), gap);
        button.setOnClickListener(v -> action.run());
    }

    private void setInputSteps(EditText input, int value) {
        input.setText(String.valueOf(Math.max(0, Math.min(200_000, value))));
        input.setSelection(input.length());
    }

    private EditText input(String hint, int type) {
        EditText field = new EditText(this);
        field.setSingleLine(true);
        field.setTextSize(16);
        field.setHint(hint);
        field.setInputType(type);
        field.setPadding(dp(13), dp(9), dp(13), dp(9));
        field.setBackground(rounded(Color.rgb(246, 249, 247), 12));
        return field;
    }

    private int parseInt(String text) {
        try { return Math.max(0, Math.min(200_000, Integer.parseInt(text.trim()))); }
        catch (Exception ignored) { return 0; }
    }

    private float parseFloat(String text) {
        try { return Math.max(0f, Math.min(33.3f, Float.parseFloat(text.replace(',', '.').trim()))); }
        catch (Exception ignored) { return 0f; }
    }

    private TextView sectionTitle(String value) {
        return text(value, 18, INK, true);
    }

    private TextView text(String value, int sp, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(sp);
        view.setTextColor(color);
        if (bold) view.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        return view;
    }

    private LinearLayout column() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        return layout;
    }

    private GradientDrawable rounded(int color, int radiusDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(radiusDp));
        return drawable;
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private LinearLayout.LayoutParams marginTop(int margin) {
        LinearLayout.LayoutParams params = matchWrap();
        params.topMargin = dp(margin);
        return params;
    }

    private LinearLayout.LayoutParams marginBottom(int margin) {
        LinearLayout.LayoutParams params = matchWrap();
        params.bottomMargin = dp(margin);
        return params;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private final class WeekChart extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private int[] values = new int[7];

        WeekChart(Context context) {
            super(context);
            setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        }

        void setValues(int[] newValues) {
            values = newValues.clone();
            invalidate();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float density = getResources().getDisplayMetrics().density;
            float width = getWidth();
            float height = getHeight();
            float labelHeight = 24 * density;
            float chartHeight = height - labelHeight - 10 * density;
            float gap = 12 * density;
            float barWidth = (width - gap * 8) / 7f;
            int max = 1;
            for (int value : values) max = Math.max(max, value);
            paint.setColor(Color.rgb(226, 235, 229));
            paint.setStrokeWidth(density);
            canvas.drawLine(0, chartHeight, width, chartHeight, paint);
            for (int i = 0; i < 7; i++) {
                float left = gap + i * (barWidth + gap);
                float barHeight = values[i] == 0 ? 3 * density : Math.max(5 * density, chartHeight * values[i] / max);
                RectF bar = new RectF(left, chartHeight - barHeight, left + barWidth, chartHeight);
                paint.setColor(i == 6 ? MINT : (values[i] == 0 ? Color.rgb(224, 233, 227) : Color.rgb(130, 190, 163)));
                canvas.drawRoundRect(bar, 5 * density, 5 * density, paint);
                paint.setColor(MUTED);
                paint.setTextSize(10 * density);
                paint.setTextAlign(Paint.Align.CENTER);
                Calendar day = StepStore.dayOffset(i - 6);
                String label = new java.text.SimpleDateFormat("EE", Locale.getDefault()).format(day.getTime());
                canvas.drawText(label, left + barWidth / 2f, height - 4 * density, paint);
            }
        }
    }
}

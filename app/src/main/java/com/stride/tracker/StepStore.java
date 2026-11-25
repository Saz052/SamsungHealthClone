package com.stride.tracker;

import android.content.Context;
import android.content.SharedPreferences;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

final class StepStore {
    private static final String PREFERENCES = "stride_activity";
    private static final String STEP_PREFIX = "steps_";

    private StepStore() {}

    static SharedPreferences preferences(Context context) {
        return context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
    }

    static Calendar today() {
        Calendar day = Calendar.getInstance();
        clearTime(day);
        return day;
    }

    static Calendar dayOffset(int offset) {
        Calendar day = today();
        day.add(Calendar.DAY_OF_YEAR, offset);
        return day;
    }

    static String keyFor(Calendar day) {
        return STEP_PREFIX + new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(day.getTime());
    }

    static int getSteps(Context context, Calendar day) {
        return preferences(context).getInt(keyFor(day), 0);
    }

    static void setSteps(Context context, Calendar day, int steps) {
        int safeSteps = Math.max(0, Math.min(200_000, steps));
        preferences(context).edit().putInt(keyFor(day), safeSteps).apply();
    }

    static void addSteps(Context context, int amount) {
        Calendar day = today();
        setSteps(context, day, getSteps(context, day) + Math.max(0, amount));
    }

    static int todaySteps(Context context) {
        return getSteps(context, today());
    }

    static String displayDate(Calendar day) {
        return new SimpleDateFormat("EEE, d MMM", Locale.getDefault()).format(day.getTime());
    }

    static float hoursForSteps(int steps) {
        return steps / 6000f;
    }

    static float distanceKmForSteps(int steps) {
        return steps * 0.75f / 1000f;
    }

    static int caloriesForSteps(int steps) {
        return Math.round(steps * 0.04f);
    }

    private static void clearTime(Calendar day) {
        day.set(Calendar.HOUR_OF_DAY, 0);
        day.set(Calendar.MINUTE, 0);
        day.set(Calendar.SECOND, 0);
        day.set(Calendar.MILLISECOND, 0);
    }
}

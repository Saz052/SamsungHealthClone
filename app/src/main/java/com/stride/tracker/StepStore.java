package com.stride.tracker;

import android.content.Context;
import android.content.SharedPreferences;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

final class StepStore {
    private static final String PREFERENCES = "stride_activity";
    private static final String STEP_PREFIX = "steps_";
    private static final String ACTIVE_PREFIX = "active_ms_";
    private static final String LAST_STEP_TIME = "last_step_time_ms";
    private static final String LAST_STEP_DAY = "last_step_day";
    private static final long MAX_ACTIVE_GAP_MS = 10_000L;

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

    private static String dateStamp(Calendar day) {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(day.getTime());
    }

    static String keyFor(Calendar day) {
        return STEP_PREFIX + dateStamp(day);
    }

    private static String activeKey(Calendar day) {
        return ACTIVE_PREFIX + dateStamp(day);
    }

    static int getSteps(Context context, Calendar day) {
        return preferences(context).getInt(keyFor(day), 0);
    }

    static long activeMilliseconds(Context context, Calendar day) {
        return preferences(context).getLong(activeKey(day), 0L);
    }

    static void recordSensorSteps(Context context, int amount) {
        SharedPreferences prefs = preferences(context);
        Calendar day = today();
        String stamp = dateStamp(day);
        long now = System.currentTimeMillis();
        long previousTime = prefs.getLong(LAST_STEP_TIME, 0L);
        String previousDay = prefs.getString(LAST_STEP_DAY, "");
        long activeTime = prefs.getLong(activeKey(day), 0L);
        long gap = now - previousTime;

        if (stamp.equals(previousDay) && gap > 0L && gap <= MAX_ACTIVE_GAP_MS) {
            activeTime += gap;
        }

        int currentSteps = prefs.getInt(keyFor(day), 0);
        int newSteps = Math.min(200_000, currentSteps + Math.max(0, amount));
        prefs.edit()
                .putInt(keyFor(day), newSteps)
                .putLong(activeKey(day), activeTime)
                .putLong(LAST_STEP_TIME, now)
                .putString(LAST_STEP_DAY, stamp)
                .apply();
    }

    static void setSteps(Context context, Calendar day, int steps) {
        int safeSteps = Math.max(0, Math.min(200_000, steps));
        preferences(context).edit().putInt(keyFor(day), safeSteps).apply();
    }

    static int todaySteps(Context context) {
        return getSteps(context, today());
    }

    static String displayDate(Calendar day) {
        return new SimpleDateFormat("EEE, d MMM", Locale.getDefault()).format(day.getTime());
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

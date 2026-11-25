package com.stride.tracker;

import android.content.Context;
import android.content.SharedPreferences;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

final class StepStore {
    private static final String PREFS = "stride_activity";
    private static final String PREFIX = "steps_";

    private StepStore() {}

    static SharedPreferences preferences(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    static Calendar today() {
        Calendar calendar = Calendar.getInstance();
        clearTime(calendar);
        return calendar;
    }

    static Calendar dayOffset(int offset) {
        Calendar calendar = today();
        calendar.add(Calendar.DAY_OF_YEAR, offset);
        return calendar;
    }

    static String keyFor(Calendar calendar) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        return PREFIX + format.format(calendar.getTime());
    }

    static int getSteps(Context context, Calendar calendar) {
        return preferences(context).getInt(keyFor(calendar), 0);
    }

    static void setSteps(Context context, Calendar calendar, int steps) {
        int safeSteps = Math.max(0, Math.min(200_000, steps));
        preferences(context).edit().putInt(keyFor(calendar), safeSteps).apply();
    }

    static void addSteps(Context context, int amount) {
        Calendar calendar = today();
        int next = getSteps(context, calendar) + Math.max(0, amount);
        setSteps(context, calendar, next);
    }

    static int todaySteps(Context context) {
        return getSteps(context, today());
    }

    static String displayDate(Calendar calendar) {
        SimpleDateFormat format = new SimpleDateFormat("EEE, d MMM", Locale.getDefault());
        return format.format(calendar.getTime());
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

    private static void clearTime(Calendar calendar) {
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
    }
}

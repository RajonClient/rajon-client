package com.example.fastbeo;

import java.util.ArrayList;
import java.util.List;

/** Simple notification queue. Hud.java draws the entries. */
public class Notifications {
    public static final long LIFETIME = 2500L;
    public static final int MAX = 5;

    public static class Entry {
        public final String title;
        public final String text;
        public final boolean good;
        public final long time;

        Entry(String title, String text, boolean good) {
            this.title = title;
            this.text = text;
            this.good = good;
            this.time = System.currentTimeMillis();
        }
    }

    private static final List<Entry> list = new ArrayList<Entry>();

    public static void add(String title, String text, boolean good) {
        list.add(new Entry(title, text, good));
        while (list.size() > MAX) list.remove(0);
    }

    /** Removes expired entries and returns the live ones, oldest first. */
    public static List<Entry> active() {
        long now = System.currentTimeMillis();
        for (int i = list.size() - 1; i >= 0; i--) {
            if (now - list.get(i).time > LIFETIME) list.remove(i);
        }
        return new ArrayList<Entry>(list);
    }
}

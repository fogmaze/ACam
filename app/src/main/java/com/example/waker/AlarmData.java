package com.example.waker;

import android.util.Log;

import androidx.annotation.NonNull;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

public class AlarmData implements Serializable {
    public ArrayList<Rule> datas = new ArrayList<>();
    long timeZone = 8 * 60 * 60 * 1000;

    public void add(Rule rule) {
        for (Rule r : datas) {
            if (r.name.equals(rule.name)) {
                remove(r.name);
                datas.add(rule);
                return;
            }
        }
        datas.add(rule);
    }

    public void remove(String name) {
        for (Rule r : datas) {
            if (r.name.equals(name)) {
                datas.remove(r);
                return;
            }
        }
    }

    public long getNext() {
        long nearest = Long.MAX_VALUE;
        long now = System.currentTimeMillis();
        for (Rule rule : datas) {
            if (rule.repeat.equals("weekly")) {
                long next = getNextWeekly(rule, now);
                if (next < nearest){
                    nearest = next;
                }
            }
            if (rule.repeat.equals("daily")) {
                long next = getNextDaily(rule, now);
                if (next < nearest){
                    nearest = next;
                }
            }
            if (rule.repeat.equals("once")) {
                long next = getNextOnce(rule, now);
                if (next < nearest){
                    nearest = next;
                }
            }
        }
        return nearest;
    }

    private static long getNextOnce(Rule rule, long now) {
        long timeStamp = rule.date + (long) rule.hour * 60 * 60 * 1000 + (long) rule.minute * 60 * 1000;
        if (timeStamp > now) {
            return timeStamp;
        }
        return Long.MAX_VALUE;
    }

    private long getNextDaily(Rule rule, long now) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date(now));
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);
        int h = rule.hour;
        int m = rule.minute;
        if (h < hour || (h == hour && m < minute)) {
            return now + 24 * 60 * 60 * 1000;
        }
        int diff = (h - hour) * 60 + (m - minute);
        return now + (long) diff * 60 * 1000;
    }

    private long getNextWeekly(Rule rule, long now) {
        int dayOfWeek = (int) (((((now+timeZone) / (24*60*60*1000)) % 7) + 5) % 8);
        int minDis = 7;
        for (int d : rule.days){
            int dis = d - dayOfWeek;
            if (dis < 0) {
                dis += 7;
            }
            if (dis == 0 && now/1000 > now/(24*60*60*1000) * (24*60*60) + (long) rule.hour * 60 * 60 + (long) rule.minute * 60 - timeZone/1000) {
                dis = 7;
            }

            if (dis < minDis) {
                minDis = dis;
            }
        }
        return now/(24*60*60*1000) * (24*60*60) + (long) minDis * 24 * 60 * 60 + (long) rule.hour * 60 * 60 + (long) rule.minute * 60 - timeZone/1000;
    }

    public static class Rule implements Serializable{
        public String repeat;
        public String name;
        public int hour;
        public int minute;
        public ArrayList<Integer> days;
        public long date;
        Rule(String name, String repeat,ArrayList<Integer> days, long date, int hour, int minute) {
            this.name = name;
            this.repeat = repeat;
            this.days = days;
            this.hour = hour;
            this.minute = minute;
            this.date = date;
        }
        @NonNull
        public String toString() {
            return name + " " + repeat + " " + days + " " + date + " " + hour + " " + minute;
        }
    }
}

package com.example.waker;

import android.util.Log;

class ClockWatcher extends Thread {
    private final WakerService wakerService;
    public final Object mLock = new Object();
    public AlarmData mAlarmData = new AlarmData();
    private long next;

    public ClockWatcher(WakerService wakerService) {
        this.wakerService = wakerService;
    }

    @Override
    public void run() {
        update_sync();
        while (true) {
            try {
                Thread.sleep(5000);
                synchronized (mLock) {
                    if (System.currentTimeMillis() / 1000 >= next) {
                        wakerService.startWaking(30);
                        wakerService.sendBroadcast("Wake up!");
                        update();
                    }
                }
                Log.i(wakerService.TAG, "clock: checked, time left: " + (next - System.currentTimeMillis() / 1000) + "s");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void update_sync() {
        synchronized (mLock) {
            update();
        }
    }

    public void update() {
        next = mAlarmData.getNext();
    }
}

package com.example.waker;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.IBinder;
import android.util.Base64;
import android.util.Log;
import android.util.Size;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.example.waker.utils.Recognition;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;

public class WakerService extends Service {
    private static final int NOTIFICATION_ID = 806;
    public final String TAG = "WakerService";
    private ClockWatcher mClockWatcher = null;
    private FlashlightHolder mFlashlightHolder = null;
    private CameraHolder mCameraHolder = null;
    Analyser mAnalyser = null;
    private String CHANNEL_ID = "waker";
    private enum WakingState {
        IDLE,
        CHECK,
        ALARM_ON,
        ALARM_PAUSE,
    }
    private WakingState mWakingState = WakingState.IDLE;
    private WakingState mNextWakingState = WakingState.IDLE;
    private long mNextWaitingTime = 0;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            Log.d(TAG, "onStartCommand: intent is null");
            return super.onStartCommand(intent, flags, startId);
        }
        String msg = intent.getStringExtra("msg");
        Log.d(TAG, "onStartCommand: " + msg);
        //handle msg
        if (msg.equals("update AlarmData")) {
            updateAlarmData();
        }

        if (msg.equals("start")) {
            startWaking(30);
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        try {
            mFlashlightHolder = new FlashlightHolder(this);
            mClockWatcher = new ClockWatcher(this);
            mAnalyser = new Analyser(this, "heads");
            mCameraHolder = new CameraHolder(this, new Size(640, 640), mAnalyser);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel channel = new NotificationChannel(
                        CHANNEL_ID,
                        "Foreground Service Channel",
                        NotificationManager.IMPORTANCE_DEFAULT
                );
                NotificationManager manager = getSystemService(NotificationManager.class);
                manager.createNotificationChannel(channel);
            }
            Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentTitle("waker")
                    .setContentText("Service is running...")
                    .build();

            startForeground(NOTIFICATION_ID, notification);
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {;
                return;
            }
            mClockWatcher.mAlarmData = loadAlarmData();
            mClockWatcher.start();
            mFlashlightHolder.start();

            // for testing
            startWaking(30);

        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            Log.e(TAG, e.toString());
            resetAlarmData();
        }
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        mClockWatcher.interrupt();
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public void startWaking(int waitIndex) {
        if (waitIndex == 0) {
            return ;
        }
        try {
            Log.d(TAG, "startWaking");
            mCameraHolder.startCapture();
            Thread.sleep(1000);
            ArrayList<Recognition> recognitions = mAnalyser.getRecognitions();
            if (recognitions.size() == 0) {
                startWaking(waitIndex - 1);
            }

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    public void sendBroadcast(String msg) {
        Intent intent = new Intent("com.example.waker.WakerService");
        intent.putExtra("msg", msg);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void updateAlarmData() {
        synchronized (mClockWatcher.mLock){
            try {
                mClockWatcher.mAlarmData = loadAlarmData();
                mClockWatcher.update();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
                resetAlarmData();
            }
        }
    }

    private void saveAlarmData() throws IOException {
        SharedPreferences sharedPreferences = getSharedPreferences("WakerService", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        synchronized (mClockWatcher.mLock){
            oos.writeObject(mClockWatcher.mAlarmData);
        }
        String objectString = Base64.encodeToString(bos.toByteArray(), Base64.DEFAULT);
        editor.putString("alarm_data", objectString);
        editor.apply();
    }


    private AlarmData loadAlarmData() throws IOException, ClassNotFoundException {
        SharedPreferences sharedPreferences = getSharedPreferences("WakerService", MODE_PRIVATE);
        String objectString = sharedPreferences.getString("alarm_data", null);
        if (objectString == null) {
            return new AlarmData();
        }
        byte[] objectBytes = Base64.decode(objectString, Base64.DEFAULT);
        ByteArrayInputStream bis = new ByteArrayInputStream(objectBytes);
        ObjectInputStream ois = new ObjectInputStream(bis);
        return (AlarmData) ois.readObject();
    }

    private void resetAlarmData()  {
        try {
            SharedPreferences sharedPreferences = getSharedPreferences("WakerService", MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream oos = null;
            oos = new ObjectOutputStream(bos);
            oos.writeObject(new AlarmData());
            String objectString = Base64.encodeToString(bos.toByteArray(), Base64.DEFAULT);
            editor.putString("alarm_data", objectString);
            editor.apply();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

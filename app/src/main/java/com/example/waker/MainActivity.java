package com.example.waker;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;

public class MainActivity extends AppCompatActivity {
    private final String TAG = "MainActivity";
    private Button mSubmitButton;
    private Button mRemoveButton;
    private Button mStopButton;
    private Spinner mRepeatSpinner;
    private Spinner mRuleSpinner;
    private EditText mNameEditText;
    private EditText mTimeEditText;
    private EditText mExtraEditText;
    private TextView mRuleShowTextView;

    private AlarmData mAlarmData = null;
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String msg = intent.getStringExtra("msg");
            Log.d(TAG, "onReceive: " + msg);
            if (msg.equals("update alarm data")){
                mAlarmData = (AlarmData) intent.getSerializableExtra("alarm data");
            }
        }
    };

    private ArrayAdapter<String> mRuleAdapter;

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            for (int result: grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    finish();
                }
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //grant camera permission
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA,Manifest.permission.FOREGROUND_SERVICE}, 1);
            } else{
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 1);
            }
        }

        // Register the local receiver
        LocalBroadcastManager.getInstance(this).registerReceiver(mReceiver, new IntentFilter("com.example.waker.WakerService"));
        Intent intent = new Intent(this, WakerService.class);
        intent.putExtra("msg", "Hello from MainActivity");
        startService(intent);

        mSubmitButton = findViewById(R.id.main_summit);
        mRepeatSpinner = findViewById(R.id.main_repeatSelect);
        mTimeEditText = findViewById(R.id.main_timeInput);
        mExtraEditText = findViewById(R.id.main_extraDes);
        mNameEditText = findViewById(R.id.main_name);
        mRuleShowTextView = findViewById(R.id.main_ruleShow);
        mRuleSpinner = findViewById(R.id.main_ruleSpinner);
        mRemoveButton = findViewById(R.id.main_remove);
        mStopButton = findViewById(R.id.main_stop);
        try {
            mSubmitButton.setOnClickListener(v -> {
                try {
                    ArrayList<Integer> days = new ArrayList<>();
                    long date = 0;
                    if (mRepeatSpinner.getSelectedItem().toString().equals("weekly")) {
                        String[] daysStr = mExtraEditText.getText().toString().split(" ");
                        for (String day : daysStr) {
                            days.add(Integer.parseInt(day));
                        }
                    }
                    if (mRepeatSpinner.getSelectedItem().toString().equals("once")) {
                        Calendar calendar = Calendar.getInstance();
                        String[] dateStr = mExtraEditText.getText().toString().split("/");
                        calendar.set(Integer.parseInt(dateStr[0]), Integer.parseInt(dateStr[1]), Integer.parseInt(dateStr[2]));
                        date = calendar.getTimeInMillis();
                    }
                    mAlarmData.add(new AlarmData.Rule(
                            mNameEditText.getText().toString(),
                            mRepeatSpinner.getSelectedItem().toString(),
                            days,
                            date,
                            Integer.parseInt(mTimeEditText.getText().toString().split(":")[0]),
                            Integer.parseInt(mTimeEditText.getText().toString().split(":")[1])
                    ));
                    saveAlarmData();
                    Intent intent1 = new Intent(this, WakerService.class);
                    intent1.putExtra("msg", "update AlarmData");
                    startService(intent1);
                    updateRuleSpinner();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            mRemoveButton.setOnClickListener(v -> {
                try {
                    if (mRuleSpinner.getSelectedItem() == null) {
                        return;
                    }
                    mAlarmData.remove(mRuleSpinner.getSelectedItem().toString());
                    saveAlarmData();
                    Intent intent1 = new Intent(this, WakerService.class);
                    intent1.putExtra("msg", "update AlarmData");
                    startService(intent1);
                    updateRuleSpinner();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            mStopButton.setOnClickListener(v -> {
                Intent intent1 = new Intent(this, WakerService.class);
                intent1.putExtra("msg", "start");
                startService(intent1);
            });
            mRuleAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, new ArrayList<>());
            mRuleAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            mRuleSpinner.setAdapter(mRuleAdapter);
            updateRuleSpinner();
            mRuleSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    updateAlarmData();
                    if (mAlarmData.datas.size() == 0) {
                        return;
                    }
                    mRuleShowTextView.setText(mAlarmData.datas.get(position).toString());
                }
                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                }
            });

        } catch (IOException e) {
            e.printStackTrace();
        }

    }
    private void updateRuleSpinner() throws IOException{
        updateAlarmData();
        String[] ruleNames = new String[mAlarmData.datas.size()];
        for (int i = 0; i < mAlarmData.datas.size(); i++) {
            ruleNames[i] = mAlarmData.datas.get(i).name;
        }
        Log.i(TAG, "updateRuleSpinner: " + Arrays.toString(ruleNames));
        mRuleAdapter.clear();
        mRuleAdapter.addAll(ruleNames);
        mRuleAdapter.notifyDataSetChanged();
    }
    public void updateAlarmData(){
        try{
            mAlarmData = loadAlarmData();
        } catch (ClassNotFoundException | IOException e) {
            e.printStackTrace();
            resetAlarmData();
        }
    }

    private void saveAlarmData() throws IOException {
        SharedPreferences sharedPreferences = getSharedPreferences("WakerService", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(mAlarmData);
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
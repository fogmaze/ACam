package com.example.waker;

import android.content.Context;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;

import java.util.Objects;

class FlashlightHolder extends Thread {
    private final WakerService wakerService;
    public final Object mLock = new Object();
    public final Object mBGLock = new Object();
    private CameraManager mCameraManager;
    private boolean mBackGroundSignal = false;
    private boolean mBackGroundOn = false;
    private boolean mSignal = false;
    private String mCameraId = null;

    public FlashlightHolder(WakerService wakerService) {
        this.wakerService = wakerService;
        mCameraManager = (CameraManager) wakerService.getSystemService(Context.CAMERA_SERVICE);
        try {
            String[] cameraIds= Objects.requireNonNull(mCameraManager).getCameraIdList();
            for(String id : cameraIds){
                CameraCharacteristics cc = mCameraManager.getCameraCharacteristics(id);
                if(cc.get(CameraCharacteristics.FLASH_INFO_AVAILABLE)){
                    mCameraId = id;
                    break;
                }
            }
            // check if flash is supported
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        try {
            while (true) {
                synchronized (mBGLock) {
                    if (!mBackGroundOn) {
                        Thread.sleep(100);
                        continue;
                    }
                }
                mBackGroundSignal = true;
                updateSignal();
                Thread.sleep(100);
                mBackGroundSignal = false;
                updateSignal();
                Thread.sleep(100);
                mBackGroundSignal = true;
                updateSignal();
                Thread.sleep(100);
                mBackGroundSignal = false;
                updateSignal();
                Thread.sleep(100);
                mBackGroundSignal = true;
                updateSignal();
                Thread.sleep(100);
                mBackGroundSignal = false;
                updateSignal();
                Thread.sleep(100);
                mBackGroundSignal = true;
                updateSignal();
                Thread.sleep(2000);
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void updateSignal() {
        try {
            if (mCameraId == null) {
                Log.e(wakerService.TAG, "updateSignal: no camera");
                return;
            }
            synchronized (mLock) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    mCameraManager.setTorchMode(mCameraId, mSignal || mBackGroundSignal);
                }
            }
        } catch (CameraAccessException e) {
            if(e.getReason() == 4) {

            }
            throw new RuntimeException(e);
        }
    }

    public void setBackGroundOn(boolean mode) {
        synchronized (mBGLock) {
            mBackGroundOn = mode;
        }
    } // set

    public void setTorchMode(boolean mode) {
        synchronized (mLock) {
            mSignal = mode;
        }
        updateSignal();
    }
}

package com.example.waker;

import android.content.Context;
import android.graphics.Bitmap;
import android.media.Image;
import android.media.ImageReader;
import android.util.Log;

import com.example.waker.utils.Recognition;
import com.example.waker.utils.ImageProcess;

import java.util.ArrayList;

public class Analyser implements ImageReader.OnImageAvailableListener {
    private final String TAG = "Analyser";
    private Yolov5TFLiteDetector detector;
    private ArrayList<Recognition> recognitions;
    private final Object lock = new Object();

    private ImageProcess imageProcess = new ImageProcess();

    public Analyser(Context context, String modelName) {
        detector = new Yolov5TFLiteDetector(context, modelName);
    }
    public ArrayList<Recognition> getRecognitions() {
        synchronized (lock) {
            return recognitions;
        }
    }
    @Override
    public void onImageAvailable(ImageReader imageReader) {
        Image image = imageReader.acquireLatestImage();
        if (image == null) {
            return;
        }


        byte[][] yuvBytes = new byte[3][];
        Image.Plane[] planes = image.getPlanes();
        int imageHeight = image.getHeight();
        int imagewWidth = image.getWidth();

        Log.i(TAG, "image size: " + imageHeight + "x" + imagewWidth);

        imageProcess.fillBytes(planes, yuvBytes);
        int yRowStride = planes[0].getRowStride();
        final int uvRowStride = planes[1].getRowStride();
        final int uvPixelStride = planes[1].getPixelStride();

        int[] rgbBytes = new int[imageHeight * imagewWidth];
        imageProcess.YUV420ToARGB8888(
                yuvBytes[0],
                yuvBytes[1],
                yuvBytes[2],
                imagewWidth,
                imageHeight,
                yRowStride,
                uvRowStride,
                uvPixelStride,
                rgbBytes);

        // 原图bitmap
        Bitmap imageBitmap = Bitmap.createBitmap(imagewWidth, imageHeight, Bitmap.Config.ARGB_8888);
        imageBitmap.setPixels(rgbBytes, 0, imagewWidth, 0, 0, imagewWidth, imageHeight);

        image.close();
        ArrayList<Recognition> recognitions_buf = detector.detect(imageBitmap);
        synchronized (lock) {
            recognitions = recognitions_buf;
        }
    }
}

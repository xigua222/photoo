package com.taobao.android.mnn;

import android.graphics.Bitmap;
import android.util.Log;

public class MNNNetNative {
    private static final String TAG = "MNNRuntime";
    private static volatile Throwable loadError;

    static void loadGpuLibrary(String name) {
        try {
            System.loadLibrary(name);
        } catch (Throwable throwable) {
            Log.w(TAG, "load GPU lib failed: " + name, throwable);
        }
    }

    static {
        try {
            System.loadLibrary("MNN");
            loadGpuLibrary("MNN_CL");
            System.loadLibrary("mnncore");
        } catch (Throwable throwable) {
            loadError = throwable;
            Log.e(TAG, "MNN runtime load failed", throwable);
        }
    }

    public static boolean isRuntimeReady() {
        return loadError == null;
    }

    public static Throwable getLoadError() {
        return loadError;
    }

    public static void ensureRuntimeReady() {
        if (loadError != null) {
            UnsatisfiedLinkError error = new UnsatisfiedLinkError("MNN runtime is unavailable. Add native MNN libs.");
            error.initCause(loadError);
            throw error;
        }
    }

    protected static native long nativeCreateNetFromFile(String modelName);
    protected static native long nativeCreateNetFromBuffer(byte[] buffer);
    protected static native long nativeReleaseNet(long netPtr);

    protected static native long nativeCreateSession(long netPtr, int forwardType, int numThread, String[] saveTensors, String[] outputTensors);
    protected static native void nativeReleaseSession(long netPtr, long sessionPtr);
    protected static native int nativeRunSession(long netPtr, long sessionPtr);
    protected static native int nativeRunSessionWithCallback(long netPtr, long sessionPtr, String[] nameArray, long[] tensorAddr);
    protected static native int nativeReshapeSession(long netPtr, long sessionPtr);
    protected static native long nativeGetSessionInput(long netPtr, long sessionPtr, String name);
    protected static native long nativeGetSessionOutput(long netPtr, long sessionPtr, String name);

    protected static native void nativeReshapeTensor(long netPtr, long tensorPtr, int[] dims);
    protected static native int[] nativeTensorGetDimensions(long tensorPtr);
    protected static native void nativeSetInputIntData(long netPtr, long tensorPtr, int[] data);
    protected static native void nativeSetInputFloatData(long netPtr, long tensorPtr, float[] data);
    protected static native int nativeTensorGetData(long tensorPtr, float[] dest);
    protected static native int nativeTensorGetIntData(long tensorPtr, int[] dest);
    protected static native int nativeTensorGetUINT8Data(long tensorPtr, byte[] dest);

    protected static native boolean nativeConvertBitmapToTensor(Bitmap srcBitmap, long tensorPtr, int destFormat, int filterType, int wrap, float[] matrixValue, float[] mean, float[] normal);
    protected static native boolean nativeConvertBufferToTensor(byte[] bufferData, int width, int height, long tensorPtr, int srcFormat, int destFormat, int filterType, int wrap, float[] matrixValue, float[] mean, float[] normal);
}

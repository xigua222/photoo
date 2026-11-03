package com.taobao.android.mnn;

import android.util.Log;

public class MNNNetInstance {
    private static final String TAG = "MNNRuntime";

    public static MNNNetInstance createFromFile(String fileName) {
        MNNNetNative.ensureRuntimeReady();
        long instance = MNNNetNative.nativeCreateNetFromFile(fileName);
        if (instance == 0) {
            Log.e(TAG, "Create net failed from file " + fileName);
            return null;
        }
        return new MNNNetInstance(instance);
    }

    public static MNNNetInstance createFromBuffer(byte[] buffer) {
        MNNNetNative.ensureRuntimeReady();
        long instance = MNNNetNative.nativeCreateNetFromBuffer(buffer);
        if (instance == 0) {
            Log.e(TAG, "Create net failed from buffer");
            return null;
        }
        return new MNNNetInstance(instance);
    }

    public static class Config {
        public int forwardType = MNNForwardType.FORWARD_CPU.type;
        public int numThread = 4;
        public String[] saveTensors = null;
        public String[] outputTensors = null;
    }

    public class Session {
        public class Tensor {
            private float[] mData = null;
            private int[] mIntData = null;
            private byte[] mUINT8Data = null;
            private long mTensorInstance;

            private Tensor(long ptr) {
                mTensorInstance = ptr;
            }

            protected long instance() {
                return mTensorInstance;
            }

            public void reshape(int[] dims) {
                MNNNetNative.ensureRuntimeReady();
                MNNNetNative.nativeReshapeTensor(mNetInstance, mTensorInstance, dims);
                mData = null;
                mIntData = null;
                mUINT8Data = null;
            }

            public void setInputIntData(int[] data) {
                MNNNetNative.ensureRuntimeReady();
                MNNNetNative.nativeSetInputIntData(mNetInstance, mTensorInstance, data);
                mData = null;
            }

            public void setInputFloatData(float[] data) {
                MNNNetNative.ensureRuntimeReady();
                MNNNetNative.nativeSetInputFloatData(mNetInstance, mTensorInstance, data);
                mData = null;
            }

            public int[] getDimensions() {
                MNNNetNative.ensureRuntimeReady();
                return MNNNetNative.nativeTensorGetDimensions(mTensorInstance);
            }

            public float[] getFloatData() {
                getData();
                return mData;
            }

            public int[] getIntData() {
                MNNNetNative.ensureRuntimeReady();
                if (mIntData == null) {
                    int size = MNNNetNative.nativeTensorGetIntData(mTensorInstance, null);
                    mIntData = new int[size];
                }
                MNNNetNative.nativeTensorGetIntData(mTensorInstance, mIntData);
                return mIntData;
            }

            public void getData() {
                MNNNetNative.ensureRuntimeReady();
                if (mData == null) {
                    int size = MNNNetNative.nativeTensorGetData(mTensorInstance, null);
                    mData = new float[size];
                }
                MNNNetNative.nativeTensorGetData(mTensorInstance, mData);
            }

            public byte[] getUINT8Data() {
                MNNNetNative.ensureRuntimeReady();
                if (mUINT8Data == null) {
                    int size = MNNNetNative.nativeTensorGetUINT8Data(mTensorInstance, null);
                    mUINT8Data = new byte[size];
                }
                MNNNetNative.nativeTensorGetUINT8Data(mTensorInstance, mUINT8Data);
                return mUINT8Data;
            }
        }

        private long mSessionInstance = 0;

        private Session(long ptr) {
            mSessionInstance = ptr;
        }

        public void reshape() {
            MNNNetNative.ensureRuntimeReady();
            MNNNetNative.nativeReshapeSession(mNetInstance, mSessionInstance);
        }

        public void run() {
            MNNNetNative.ensureRuntimeReady();
            MNNNetNative.nativeRunSession(mNetInstance, mSessionInstance);
        }

        public Tensor[] runWithCallback(String[] names) {
            MNNNetNative.ensureRuntimeReady();
            long[] tensorPtr = new long[names.length];
            Tensor[] tensorReturnArray = new Tensor[names.length];
            MNNNetNative.nativeRunSessionWithCallback(mNetInstance, mSessionInstance, names, tensorPtr);
            for (int i = 0; i < names.length; i++) {
                tensorReturnArray[i] = new Tensor(tensorPtr[i]);
            }
            return tensorReturnArray;
        }

        public Tensor getInput(String name) {
            MNNNetNative.ensureRuntimeReady();
            long tensorPtr = MNNNetNative.nativeGetSessionInput(mNetInstance, mSessionInstance, name);
            if (tensorPtr == 0) {
                Log.e(TAG, "Can't find session input: " + name);
                return null;
            }
            return new Tensor(tensorPtr);
        }

        public Tensor getOutput(String name) {
            MNNNetNative.ensureRuntimeReady();
            long tensorPtr = MNNNetNative.nativeGetSessionOutput(mNetInstance, mSessionInstance, name);
            if (tensorPtr == 0) {
                Log.e(TAG, "Can't find session output: " + name);
                return null;
            }
            return new Tensor(tensorPtr);
        }

        public void release() {
            checkValid();
            MNNNetNative.ensureRuntimeReady();
            MNNNetNative.nativeReleaseSession(mNetInstance, mSessionInstance);
            mSessionInstance = 0;
        }
    }

    private long mNetInstance;

    private MNNNetInstance(long instance) {
        mNetInstance = instance;
    }

    public Session createSession(Config config) {
        checkValid();
        MNNNetNative.ensureRuntimeReady();

        if (config == null) {
            config = new Config();
        }

        long sessionId = MNNNetNative.nativeCreateSession(
                mNetInstance,
                config.forwardType,
                config.numThread,
                config.saveTensors,
                config.outputTensors
        );
        if (sessionId == 0) {
            Log.e(TAG, "Create session error");
            return null;
        }
        return new Session(sessionId);
    }

    public void release() {
        checkValid();
        MNNNetNative.ensureRuntimeReady();
        MNNNetNative.nativeReleaseNet(mNetInstance);
        mNetInstance = 0;
    }

    private void checkValid() {
        if (mNetInstance == 0) {
            throw new RuntimeException("MNNNetInstance native pointer is null, it may already be released");
        }
    }
}

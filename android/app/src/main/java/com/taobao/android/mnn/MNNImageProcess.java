package com.taobao.android.mnn;

import android.graphics.Bitmap;
import android.graphics.Matrix;

public class MNNImageProcess {

    public enum Format {
        RGBA(0),
        RGB(1),
        BGR(2),
        GRAY(3),
        BGRA(4),
        YUV_420(10),
        YUV_NV21(11);

        public final int type;

        Format(int t) {
            type = t;
        }
    }

    public enum Filter {
        NEAREST(0),
        BILINEAL(1),
        BICUBIC(2);

        public final int type;

        Filter(int t) {
            type = t;
        }
    }

    public enum Wrap {
        CLAMP_TO_EDGE(0),
        ZERO(1),
        REPEAT(2);

        public final int type;

        Wrap(int t) {
            type = t;
        }
    }

    public static class Config {
        public float[] mean = {0f, 0f, 0f, 0f};
        public float[] normal = {1f, 1f, 1f, 1f};
        public Format source = Format.RGBA;
        public Format dest = Format.BGR;
        public Filter filter = Filter.NEAREST;
        public Wrap wrap = Wrap.CLAMP_TO_EDGE;
    }

    public static boolean convertBuffer(byte[] buffer, int width, int height, MNNNetInstance.Session.Tensor tensor, Config config, Matrix matrix) {
        MNNNetNative.ensureRuntimeReady();
        if (matrix == null) {
            matrix = new Matrix();
        }
        float[] value = new float[9];
        matrix.getValues(value);

        return MNNNetNative.nativeConvertBufferToTensor(
                buffer,
                width,
                height,
                tensor.instance(),
                config.source.type,
                config.dest.type,
                config.filter.type,
                config.wrap.type,
                value,
                config.mean,
                config.normal
        );
    }

    public static boolean convertBitmap(Bitmap sourceBitmap, MNNNetInstance.Session.Tensor tensor, Config config, Matrix matrix) {
        MNNNetNative.ensureRuntimeReady();
        if (matrix == null) {
            matrix = new Matrix();
        }
        float[] value = new float[9];
        matrix.getValues(value);

        return MNNNetNative.nativeConvertBitmapToTensor(
                sourceBitmap,
                tensor.instance(),
                config.dest.type,
                config.filter.type,
                config.wrap.type,
                value,
                config.mean,
                config.normal
        );
    }
}

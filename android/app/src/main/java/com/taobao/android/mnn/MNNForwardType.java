package com.taobao.android.mnn;

public enum MNNForwardType {
    FORWARD_CPU(0),
    FORWARD_OPENCL(3),
    FORWARD_AUTO(4),
    FORWARD_OPENGL(6),
    FORWARD_VULKAN(7);

    public final int type;

    MNNForwardType(int t) {
        type = t;
    }
}

Drop your trimmed MNN native runtime here instead of using the heavyweight Maven AAR.

Recommended minimal set for this AI color-filter pipeline:

- `arm64-v8a/libMNN.so`
- `arm64-v8a/libMNN_CL.so`
- `arm64-v8a/libmnncore.so`

Notes:

- `libMNN_Express.so` is intentionally not loaded by the current wrapper.
- Vulkan / OpenGL backends are intentionally not loaded either.
- If you also want 32-bit device support, add matching `armeabi-v7a` libs.
- `libmnncore.so` is the JNI bridge used by [`MNNNetNative.java`](/D:/Dl/voicelike/android/app/src/main/java/com/taobao/android/mnn/MNNNetNative.java).

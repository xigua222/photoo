$ErrorActionPreference = "Stop"

$cmakeBin = Join-Path $env:LOCALAPPDATA "Android\Sdk\cmake\3.22.1\bin"
$ndkRoot = Join-Path $env:LOCALAPPDATA "Android\Sdk\ndk\27.0.12077973"
$repoRoot = Split-Path -Parent $PSScriptRoot
$mnnRoot = Join-Path (Split-Path -Parent $repoRoot) "MNN-master"
$buildDir = Join-Path $mnnRoot "build-android-arm64-jni3"

$env:PATH = "$cmakeBin;$env:PATH"

if (Test-Path $buildDir) {
    Remove-Item $buildDir -Recurse -Force
}

cmake -S $mnnRoot -B $buildDir -G Ninja `
  -DCMAKE_TOOLCHAIN_FILE="$ndkRoot\build\cmake\android.toolchain.cmake" `
  -DCMAKE_BUILD_TYPE=Release `
  -DANDROID_ABI=arm64-v8a `
  -DANDROID_STL=c++_shared `
  -DANDROID_NATIVE_API_LEVEL=android-21 `
  -DANDROID_TOOLCHAIN=clang `
  -DMNN_USE_LOGCAT=ON `
  -DMNN_USE_SSE=OFF `
  -DMNN_ARM82=ON `
  -DMNN_OPENCL=ON `
  -DMNN_VULKAN=OFF `
  -DMNN_OPENGL=OFF `
  -DMNN_BUILD_OPENCV=OFF `
  -DMNN_IMGCODECS=OFF `
  -DMNN_BUILD_AUDIO=OFF `
  -DMNN_BUILD_LLM=OFF `
  -DMNN_BUILD_DIFFUSION=OFF `
  -DMNN_BUILD_TRAIN=OFF `
  -DMNN_BUILD_TOOLS=OFF `
  -DMNN_BUILD_BENCHMARK=OFF `
  -DMNN_BUILD_TEST=OFF `
  -DMNN_BUILD_DEMO=OFF `
  -DMNN_BUILD_CONVERTER=OFF `
  -DMNN_REDUCE_SIZE=ON `
  -DMNN_JNI=ON `
  -DMNN_BUILD_FOR_ANDROID_COMMAND=true `
  -DNATIVE_LIBRARY_OUTPUT=. `
  -DNATIVE_INCLUDE_OUTPUT=.

cmake --build $buildDir --parallel 8

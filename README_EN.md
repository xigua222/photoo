<img src="logo.png" width="150" alt="photoo" align="center" />

# PHOTOO

> Your Memories. Private. Intelligent.

[![License](https://img.shields.io/badge/License-photoo%20Source%20License-blue.svg)](LICENSE_EN)
![Platform](https://img.shields.io/badge/Platform-Android-green.svg)
![Version](https://img.shields.io/badge/Android-v4.0-orange.svg)
![Help](https://img.shields.io/badge/Help%20Center-photooapp.com%2Fhelp-informational.svg)

[![ko-fi](https://ko-fi.com/img/githubbutton_sm.svg)](https://ko-fi.com/I2I01XARUK)

[English](README_EN.md) | [中文](README.md)

---

## 🎯 What is photoo?

**photoo** is an Android-native, minimalist photo organization tool.

Swipe cards to quickly organize your gallery—swipe left to delete, right to favorite, up to keep. Plus a bunch of "nice to have" features: photo insights with a personalized photography profile, the PCS color system, photo watermark frames, stamp collecting, immersive review of your favorites, smart detection of similar and low-quality shots, video and image compression...

**But photoo isn't just a "cleanup" tool—it's about "memories".**

---

## ✨ Key Features

### 👆 Swipe to Organize
Just like scrolling through short videos. Cards are shuffled by default—you never know what comes next. Last week, or a year ago? The mystery keeps you going.

* Swipe left to delete, right to favorite, up to keep, down to move into a folder
* Each card shows EXIF (time, location) plus on-device AI recognition of what's in the shot
* 16 cards per batch (adjustable), turning thousands of photos into quick "micro-tasks"

### 🗺️ Organize Range (new in v4.0)
No need to face the whole gallery at once. Scope each session by time (today, on this day, a random day, a picked date), album, or type (photo / Live Photo / video / screenshot / screen recording / panorama / animated / RAW). Sort by size, or hit "Organize Again" to rerun the last round.

### 🧠 Photo Insights 2.0
Your personal photography profile: favorite cameras, shooting-location heatmap, yearly and monthly trends, a photo globe, AI tag clouds—and narrative cards that actually speak human.

### 🎨 PCS Color System
* **Pick**: long-press a photo to extract 1–10 color points into your own palette (CMYK / RGB / HEX / HSB / LAB)
* **Grade**: nearly a hundred classic filters + manual grading, with AI face-tone protection
* **Match / Imitate**: borrow a palette from saved cards or any reference photo
* **Inspiration**: 10,000+ presets, from traditional Chinese colors to museum masterpieces

### 🖼️ Photo Watermark Frames
Glass panel, subject outline, breakout styles and more—so your photos look their best when shared.

### 🤖 Lensy, On-Device AI
A companion persona built in-house. Suggestions while organizing, grading references, even the help-center chat bot—all running locally, no network required.

### 🔍 Smart Detection & Compression
* **Similar Photos**: auto-find duplicates and near-identical shots
* **Low-Quality Images**: detect blurry, dark, overexposed frames
* **Video/Image Compression**: half the size, no visible quality loss

### 💾 Undo Everything
* System-level recycle bin + undo, with review before final delete
* Accidents reduced to a minimum

### 🌊 Liquid Glass (v4.0)
A fresh visual language across the startup screen, top bars and core surfaces—clear, light, and responsive.

---

## 📱 Platforms & Download

| Platform | Channel | Notes |
| --- | --- | --- |
| **Mainland China** | [Pgyer](https://www.pgyer.com/photoo-android) · [GitHub Releases](https://github.com/xigua222/photoo/releases/latest) | Open beta, free, no sign-in |
| **Global** | Huawei AppGallery · Samsung Galaxy Store | Live and free |

> Sideloading the APK may require enabling "Install from Unknown Sources"; both in-app and store updates are supported.

---

## 🔒 Privacy First

**This is photoo's bottom line—and what sets us apart.**

Photos are absolutely private. That's why photoo was built around one core principle: **privacy protection**:

* **100% On-Device**: recognition, classification and grading all run locally
* **Zero Photo Upload**: your photos are never uploaded to our servers
* **Always Ad-Free**: on every platform, in every version
* **No Sign-In**: the Android app collects no personal information

To be fully transparent about networking: core features work completely offline. Optional network access happens only for **update checks, the help center, and diagnostics you explicitly enable and submit**—never your photos.

**Features already released in Mainland China stay free forever.**

---

## 🛠️ Source & Security Audit

The first photoo release was fully open-sourced for auditing. Since then, for copyright-registration and third-party-licensing reasons, we publish **sanitized per-version source snapshots** instead (the `android/` directory in this repo is the v4.0 snapshot).

Licensed fonts, third-party color-card materials and watermark previews are replaced with 1×1 placeholders or removed—this **does not affect building or debugging everything else**. See [android/ASSETS.md](android/ASSETS.md) for the full list.

Build it yourself (Android SDK required; set `sdk.dir` in `android/local.properties`):

```bash
cd android
./gradlew assembleDebug
```

Bug reports via Issues are welcome—please include device model, OS version, reproduction steps and logcat. It helps a lot.

---

## 🤝 Feedback & Community

* **Help Center**: [photooapp.com/help](https://photooapp.com/help) (with the Lensy chat bot)
* **Xiaohongshu**: @超级西瓜 ｜ **Coolapk**: @西瓜发电机xg
* **QQ beta group**: 878648547
* **Email**: photoo@superwatermelon.cn
* **Campfire Plan**: if you like photoo, spread the word—or [buy the developer a milk tea](https://ko-fi.com/I2I01XARUK) to help it go further

photoo development is ongoing—there's always more to improve. We'd love your feedback, or your company in building it!

---

## 📄 License

This project uses the **photoo Source Code License Agreement**.

* ✅ Available for security auditing and local build & debug
* ❌ No modifications, distribution, commercial use, or derivative works allowed

---

**photoo, a gift for everyone who loves capturing and preserving moments.**

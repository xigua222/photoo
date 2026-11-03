<img src="logo.png" width="150" alt="photoo" align="center" />

# PHOTOO

[![License](https://img.shields.io/badge/License-photoo%20Source%20License-blue.svg)](LICENSE)
![Platform](https://img.shields.io/badge/Platform-Android-green.svg)
![Version](https://img.shields.io/badge/Android-v4.0-orange.svg)
![Help](https://img.shields.io/badge/帮助中心-photooapp.com%2Fhelp-informational.svg)

[English](README_EN.md) | [中文](README.md)

[![ko-fi](https://ko-fi.com/img/githubbutton_sm.svg)](https://ko-fi.com/I2I01XARUK)

---

## 🎯 photoo 是什么？

**photoo** 是一款 Android 原生的极简照片整理工具。

滑动卡片就能快速整理相册——左滑删除，右滑收藏，上滑保留。还有一系列"该有的全都有"的功能：相册洞察生成专属摄影画像、PCS 色彩系统、照片水印边框、集邮打卡、沉浸式回顾喜爱照片、智能识别相似照片和低质量图片、视频和图片压缩......

**但 photoo 不只是一个"清理"软件，它更是"回忆"。**

---

## ✨ 核心亮点

### 👆 滑动卡片整理
和刷短视频一样有趣。卡片顺序默认随机，你永远不知道下一张是什么——可能是上周拍的，也可能是一年前拍的。保持好奇，也就有了整理的动力。

* 左滑删除，右滑收藏，上滑保留，下滑唤起文件夹移动
* 卡片底部显示拍摄时间、地点等 EXIF 信息，还有端侧 AI 识别的拍摄内容
* 一组 16 张（可在设置调整），把成千上万张照片变成轻松的"微任务"

### 🗺️ 整理范围（v4.0 新增）
不用一次面对整个相册。按时间（今天、当年今日、随机一天、指定日期）、相册、类型（照片 / Live Photo / 视频 / 截图 / 录屏 / 全景 / 动图 / RAW）自由圈定本轮整理范围，支持按体积排序和一键"再整理"。

### 🧠 相册洞察 2.0
快速生成独属于你的摄影画像：常用相机、拍摄地热力、年度月度趋势、地球仪、AI 标签词云，以及会讲"人话"的数据叙事卡片。

### 🎨 PCS 色彩系统
* **取色**：长按图片提取 1-10 个颜色点，生成自己的色卡（支持 CMYK / RGB / HEX / HSB / LAB）
* **调色**：近百款经典滤镜 + 调色盘，AI 人脸调色保护
* **追色 / 仿色**：向色卡或任意参考图"偷"色调
* **色卡灵感**：10000+ 预设，从中国传统色到博物馆名画

### 🖼️ 照片水印边框
玻璃面板、主体描边、冲破边框等多种样式，让照片以更好看的姿态被分享。

### 🤖 Lensy 端侧 AI
自研的端侧陪伴虚拟形象。整理时的建议、调色时的参考，甚至偶尔的锐评——全部在设备本地完成，不联网也能工作。

### 🔍 智能识别与压缩
* **相似照片**：自动找出重复或极其相似的照片
* **低质量图片**：智能识别模糊、暗光、过曝等废片
* **视频/图片压缩**：画质几乎不变，文件轻松小一半

### 💾 后悔药
* 系统级回收站 + 撤销操作，删除前可反复调整
* 误删可能性降至最低

### 🌊 液态玻璃（v4.0）
全新的视觉语言贯穿启动页、顶栏与核心界面，通透、轻盈、跟手。

---

## 📱 平台与下载

| 平台 | 渠道 | 说明 |
| --- | --- | --- |
| **中国大陆** | [蒲公英](https://www.pgyer.com/photoo-android) · [GitHub Releases](https://github.com/xigua222/photoo/releases/latest) | 公开内测，免费，无需登录 |
| **海外** | [华为 AppGallery](https://url.cloud.huawei.com/A594Jp6ztK) · [三星 Galaxy Store](https://apps.samsung.com/appquery/appDetail.as?appId=com.photoo.app) | 已上架，免费 |

> 安卓 APK 安装时可能需要允许"安装未知来源应用"；商店更新与应用内更新均可用。

---

## 🔒 隐私承诺

**这是 photoo 的底线，也是和其他产品最大的区别。**

相册是绝对隐私的存在，因此 photoo 设计之初，一切的原点是**隐私保护**：

* **100% 本地运行**：识别、分类、调色全在手机端完成
* **照片零上传**：你的照片不会以任何方式上传到我们的服务器
* **永远无广告**：任何平台、任何版本，都不会有广告
* **无需注册登录**：Android 端不收集任何个人信息

关于联网，我们如实说明：核心功能支持完全离线使用；可选的联网仅发生在**检查更新、访问帮助中心、主动开启并提交诊断反馈**时，不会上传你的照片。

**中国大陆版本已开放的功能永久免费**，请放心使用。

---

## 🛠️ 源码与安全审计

photoo 首个版本曾全量开源以供安全审计；此后因软著与第三方授权资源等因素改为**按版本发布脱敏源码快照**（本仓库 `android/` 目录即 v4.0 快照）。

授权字体、第三方色卡素材、水印预览等以 1×1 占位图或目录剔除的方式处理，**不影响编译与其余功能的调试**，详见 [android/ASSETS.md](android/ASSETS.md)。

自行构建（需 Android SDK，在 `android/local.properties` 中配置 `sdk.dir`）：

```bash
cd android
./gradlew assembleDebug
```

欢迎通过 Issue 提交 bug：请附上机型、系统版本、复现步骤与 logcat，这能帮我们快得多。

---

## 🤝 反馈与社区

* **帮助中心**：[photooapp.com/help](https://photooapp.com/help)
* **小红书**：@超级西瓜 ｜ **酷安**：@西瓜发电机xg
* **QQ 内测群**：878648547
* **邮箱**：photoo@superwatermelon.cn
* **篝火计划**：如果你喜欢 photoo，欢迎在各平台推荐给朋友，或[请开发者喝一杯奶茶](https://ko-fi.com/I2I01XARUK)，共同让它走得更远

photoo 的开发只有进行时，没有完成时。期待你的声音，甚至一起加入到开发者的行列！

---

## 📄 开源许可

本项目采用 **photoo 源代码许可协议**。

* ✅ 可供安全审计与本地构建调试
* ❌ 严禁修改、分发、商业使用或二次创作

---

**photoo，献给所有热爱影像、热爱记录生活的人。**

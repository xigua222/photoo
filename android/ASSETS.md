# 资产脱敏说明（Sanitization Notice）

本仓库仅用于安全审计与问题反馈（bug report / debug），不是完整的可分发产品。
以下二进制素材**未随源码分发**，以 1×1 占位图或目录缺失的形式存在：

| 资产 | 处置 | 原因 |
| --- | --- | --- |
| `app/prebuilt_assets/color_cards/**`（Hald 色卡、胶片颗粒、灵感演示图） | 1×1 占位 | 含第三方授权素材，版权归原权利人 |
| `app/src/main/assets/watermark_fonts/` | 目录剔除 | 授权字体，禁止再分发 |
| `app/src/main/assets/{Harshita.ttf,Leiko-Regular.otf}` | 剔除 | 授权字体 |
| `app/src/main/assets/watermark_modern_previews/` | 目录剔除 | 水印模板预览素材 |
| `res/drawable` 中的 cutter*、stamp_one、watermark_template_demo、colorcard_entry_* | 1×1 占位 | 水印/集邮素材 |
| `app/src/main/assets/campfire_supporters/` | 目录剔除 | 含支持者头像与收款二维码（个人信息） |
| `app/src/main/assets/adi-registration.properties` | 剔除 | 设备注册凭据 |
| `app/release/`、签名文件、`local.properties` | 剔除 | 构建产物 / 本地配置 |

被占位的素材会导致对应功能（色卡滤镜效果、水印字体样式、集邮贴纸、篝火致谢墙展示）在本地构建后**表现不完整但不影响编译与其余功能的调试**。请勿尝试绕过；如需复现相关功能的崩溃，请直接在 Issue 中附上 logcat 与复现步骤。

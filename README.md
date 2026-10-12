# Modbus Float Tool

Android 離線工具，用來驗證 Modbus register 與常見資料型態的轉換結果。

## 功能

- 輸入最多四個 Modbus register，支援 Unsigned UInt16、Signed Int16、Hex、Binary 輸入格式。
- 輸入 32-bit binary 或 8 碼 hex。
- 同時顯示 ABCD、CDAB、BADC、DCBA 四種 byte / word order。
- 支援 Int16、UInt16、Int32、UInt32、Float32 IEEE754、Float64 IEEE754、ASCII、BCD 解析。
- 輸入 float 後反向計算 Float32 或 Float64 的 IEEE754 hex、binary 與對應 registers。
- 內建 0.0、1.0、10.0、100.0、-1.0 範例。
- 原生 Android UI，支援深色模式，不需要網路與登入。

## Build APK

在 Android Studio 開啟此資料夾，等待 Gradle sync 完成後：

1. 選擇 `Build > Build Bundle(s) / APK(s) > Build APK(s)`。
2. APK 會輸出到 `app/build/outputs/apk/debug/app-debug.apk`。

也可以在命令列執行：

```powershell
.\gradlew.bat assembleDebug
```

執行單元測試：

```powershell
.\gradlew.bat testDebugUnitTest
```

## 主要測試範圍

單元測試涵蓋 1.0、10.0、100.0、-1.0 在 ABCD、CDAB、BADC、DCBA 四種順序下的 register 轉換。

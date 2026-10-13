#!/usr/bin/env bash
# S1Client 模拟器/真机 自动化验证脚本
#
# 用途：解决 MIUI 真机上「adb input 被禁、无法点按、无法截图」导致的无法验证问题。
# 模拟器是标准 AOSP，adb input 可用，可完整跑 UI 验证。
#
# 用法：
#   bash tools/verify_on_device.sh            # 自动挑一个设备
#   bash tools/verify_on_device.sh emulator-5554
#   bash tools/verify_on_device.sh 127.0.0.1:7555
set -u

# Git Bash / MSYS 会把 /sdcard/xxx 这种路径自动转成 Windows 路径，导致
# "failed to stat remote object 'C:/.../sdcard/xxx'"。关掉这个转换。
export MSYS_NO_PATHCONV=1

ADB="C:/Users/Administrator/AppData/Local/Android/Sdk/platform-tools/adb.exe"
PKG="com.example.s1client"
ACT="$PKG/.MainActivity"
APK="app/build/outputs/apk/debug/app-debug.apk"
PROBE="com.example.s1client.PROBE_THREAD"

pick_device() {
  if [ $# -ge 1 ]; then echo "$1"; return; fi
  # 优先模拟器（无 input 限制），其次第一台设备
  "$ADB" devices | awk 'NR>1 && $2=="device" {print $1}' | grep -E "^emulator-" | head -1 | \
    { read -r x && [ -n "$x" ] && echo "$x"; } || true
  local d
  d=$("$ADB" devices | awk 'NR>1 && $2=="device" {print $1}' | head -1)
  [ -n "$d" ] && echo "$d"
}

DEV=$(pick_device "$@")
if [ -z "$DEV" ]; then
  echo "[x] 没有可用设备。先启动模拟器或连接真机："
  echo "    Android Studio → Device Manager → 启动 Medium_Phone_API_37.0"
  echo "    或  SDK/emulator/emulator.exe -avd Medium_Phone_API_37.0 -port 5560 -no-window &"
  exit 1
fi
# 注意：不能写成 A="$ADB -s $DEV" 再 $A 这样用 —— bash 会把整串当命令名，
# 报 "No such file or directory"。用函数转发参数才正确。
a() { "$ADB" -s "$DEV" "$@"; }
echo "== 目标设备: $DEV =="

step() { echo; echo "---- $1 ----"; }

step "1/7 安装 APK"
if [ -f "$APK" ]; then
  a install -r "$APK" 2>&1 | tail -1
else
  echo "[!] 没找到 $APK，请先 gradle :app:assembleDebug"
fi

step "2/7 启动 App"
a shell am force-stop $PKG
a logcat -c
a shell am start -n $ACT >/dev/null 2>&1
sleep 9

step "3/7 探针：验证帖子数据加载（不需要点按）"
# tid 可换任意真实帖子；2267000 是实测 127 楼 / 4 页的帖子
for TID in 2267000 2288701; do
  a shell am broadcast -a $PROBE --es tid "$TID" --ei page 1 >/dev/null 2>&1
done
sleep 5
a logcat -d -t 800 2>&1 | grep -E "S1Probe" | tail -6

step "4/7 首页截图"
a shell screencap -p /sdcard/s1_home.png >/dev/null 2>&1
a pull /sdcard/s1_home.png ./_v_home.png 2>&1 | tail -1
a shell rm -f /sdcard/s1_home.png >/dev/null 2>&1

step "5/7 点进第一个帖子（模拟器才可用；真机 MIUI 会报 INJECT_EVENTS）"
if a shell input tap 540 1200 >/dev/null 2>&1; then
  sleep 7
  echo "[ok] 点击成功，已进入帖子"
  a shell screencap -p /sdcard/s1_thread.png >/dev/null 2>&1
  a pull /sdcard/s1_thread.png ./_v_thread.png 2>&1 | tail -1
  a shell rm -f /sdcard/s1_thread.png >/dev/null 2>&1
  echo "-- 详情页日志（不应出现 CalledFromWrongThreadException / S1Thread failed）--"
  a logcat -d -t 1500 2>&1 | grep -E "S1Thread|S1Probe" | tail -6
else
  echo "[!] 该设备禁止 adb 输入（典型于 MIUI）。改用第 3 步的探针验证数据层。"
fi

step "6/7 翻到最后一页（验证 n-1 页老 bug 是否已修）"
if a shell input swipe 540 1600 540 400 260 >/dev/null 2>&1; then
  for i in 1 2 3 4 5 6; do
    a shell input swipe 540 1600 540 400 200 >/dev/null 2>&1
    sleep 1
  done
  sleep 5
  a shell screencap -p /sdcard/s1_last.png >/dev/null 2>&1
  a pull /sdcard/s1_last.png ./_v_last.png 2>&1 | tail -1
  a shell rm -f /sdcard/s1_last.png >/dev/null 2>&1
  echo "看 _v_last.png 的页码指示：应显示「最后一页 / 总页数」，不是 n-1"
else
  echo "[!] 该设备禁止 adb 输入，跳过翻页验证"
fi

step "7/7 崩溃检查"
if a logcat -d 2>&1 | grep -q "FATAL EXCEPTION"; then
  echo "[x] 有崩溃："
  a logcat -d 2>&1 | grep -A 20 "FATAL EXCEPTION" | head -25
else
  echo "[ok] 无 FATAL EXCEPTION"
fi
a shell "cat /sdcard/Android/data/$PKG/files/crash_trace.txt 2>/dev/null | tail -20"

echo
echo "== 截图落在 ./_v_home.png / _v_thread.png / _v_last.png =="
#!/usr/bin/env bash
# 模拟器上跑一段操作并截图。
#
# 为什么需要它：这台机器上的模拟器 adb daemon 会反复重启，设备在
# device / offline 之间跳，单独执行多条 adb 命令大概率中途断开。
# 稳妥做法：**把所有动作 + 截图放进同一次 `adb shell "..."` 里**，
# 断线只影响这一批，不会让流程停在半路。
#
# 用法：
#   bash tools/emu.sh "input tap 540 500; sleep 6"
#   bash tools/emu.sh "input swipe 900 1100 200 1100; sleep 3" flow.png
set -u
export MSYS_NO_PATHCONV=1

ADB="C:/Users/Administrator/AppData/Local/Android/Sdk/platform-tools/adb.exe"
DEV="${EMU_DEV:-emulator-5554}"
SCRIPT="${1:?用法: emu.sh \"要在设备上执行的动作\" [截图名]}"
SHOT="${2:-_shot}"

# 设备离线就先重启 daemon；否则直接跑
if ! "$ADB" -s "$DEV" get-state >/dev/null 2>&1; then
  echo "[*] 设备离线，重连 adb…"
  "$ADB" kill-server >/dev/null 2>&1
  sleep 4
  "$ADB" start-server >/dev/null 2>&1
  sleep 6
fi

# 动作 + 截图放同一次调用，避免中途断开
"$ADB" -s "$DEV" shell "$SCRIPT; screencap -p /sdcard/$SHOT.png; echo __SHOT_OK__" 2>&1 | tail -2

# 这台模拟器 adb daemon 极不稳，后面几步随时可能又断；每步都重试
tries=0
until "$ADB" -s "$DEV" shell true >/dev/null 2>&1; do
  tries=$((tries + 1))
  if [ $tries -gt 4 ]; then echo "[x] 设备持续离线，放弃"; exit 1; fi
  "$ADB" kill-server >/dev/null 2>&1
  sleep 4
  "$ADB" start-server >/dev/null 2>&1
  sleep 6
done

for i in 1 2 3; do
  if "$ADB" -s "$DEV" pull "/sdcard/$SHOT.png" "./_verify/$SHOT.png" >/dev/null 2>&1; then
    echo "[ok] 截图: ./_verify/$SHOT.png"
    exit 0
  fi
  sleep 3
done

echo "[x] 截图失败（设备可能又掉线），重试：emu.sh \"\" $SHOT"
exit 1
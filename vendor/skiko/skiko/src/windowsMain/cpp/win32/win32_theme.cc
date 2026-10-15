/**
 * ComposeKN Windows：系统浅/深色主题（AppsUseLightTheme）。
 * 返回：0 = Light，1 = Dark，2 = Unknown（对齐 Skiko AWT JNI）。
 */

#include "win32_bridge.h"

#include <windows.h>

extern "C" int32_t composekn_win32_system_theme(void) {
    const wchar_t* subkey = L"Software\\Microsoft\\Windows\\CurrentVersion\\Themes\\Personalize";
    const wchar_t* name = L"AppsUseLightTheme";
    DWORD result = 0;
    DWORD resultLength = sizeof(result);
    const LONG status = RegGetValueW(
        HKEY_CURRENT_USER,
        subkey,
        name,
        RRF_RT_DWORD,
        nullptr,
        &result,
        &resultLength);
    if (status != ERROR_SUCCESS) {
        return 2;
    }
    return result ? 0 : 1;
}

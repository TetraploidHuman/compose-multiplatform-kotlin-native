/**
 * ComposeKN Linux：系统浅/深色主题。
 *
 * 优先 xdg-desktop-portal Settings（org.freedesktop.appearance / color-scheme），
 * 失败时回退 gsettings org.gnome.desktop.interface color-scheme。
 * 返回：0 = Light，1 = Dark，2 = Unknown（对齐 Skiko AWT JNI）。
 */

#include "wayland_bridge.h"

#include <dbus/dbus.h>

#include <cstdio>
#include <cstring>
#include <string>

namespace {

int32_t fromPortalColorScheme(uint32_t scheme) {
    switch (scheme) {
        case 1: return 1; // Dark
        case 2: return 0; // Light
        default: return 2;
    }
}

bool readPortalColorScheme(uint32_t* out) {
    DBusError err;
    dbus_error_init(&err);
    DBusConnection* c = dbus_bus_get(DBUS_BUS_SESSION, &err);
    if (!c || dbus_error_is_set(&err)) {
        if (dbus_error_is_set(&err)) dbus_error_free(&err);
        return false;
    }

    DBusMessage* msg = dbus_message_new_method_call(
        "org.freedesktop.portal.Desktop",
        "/org/freedesktop/portal/desktop",
        "org.freedesktop.portal.Settings",
        "Read");
    if (!msg) return false;

    const char* ns = "org.freedesktop.appearance";
    const char* key = "color-scheme";
    dbus_message_append_args(
        msg,
        DBUS_TYPE_STRING, &ns,
        DBUS_TYPE_STRING, &key,
        DBUS_TYPE_INVALID);

    DBusMessage* reply = dbus_connection_send_with_reply_and_block(c, msg, 1500, &err);
    dbus_message_unref(msg);
    if (!reply) {
        if (dbus_error_is_set(&err)) dbus_error_free(&err);
        return false;
    }

    // Reply is a nested variant: v → u (uint32)
    DBusMessageIter root;
    if (!dbus_message_iter_init(reply, &root) ||
        dbus_message_iter_get_arg_type(&root) != DBUS_TYPE_VARIANT) {
        dbus_message_unref(reply);
        return false;
    }
    DBusMessageIter outer;
    dbus_message_iter_recurse(&root, &outer);
    if (dbus_message_iter_get_arg_type(&outer) != DBUS_TYPE_VARIANT) {
        dbus_message_unref(reply);
        return false;
    }
    DBusMessageIter inner;
    dbus_message_iter_recurse(&outer, &inner);
    if (dbus_message_iter_get_arg_type(&inner) != DBUS_TYPE_UINT32) {
        dbus_message_unref(reply);
        return false;
    }
    dbus_uint32_t value = 0;
    dbus_message_iter_get_basic(&inner, &value);
    dbus_message_unref(reply);
    *out = value;
    return true;
}

int32_t fromGsettings() {
    FILE* pipe = popen(
        "gsettings get org.gnome.desktop.interface color-scheme 2>/dev/null",
        "r");
    if (!pipe) return 2;
    char buf[128] = {};
    const char* got = fgets(buf, sizeof(buf), pipe);
    const int status = pclose(pipe);
    if (!got || status != 0) return 2;
    if (std::strstr(buf, "prefer-dark") != nullptr) return 1;
    if (std::strstr(buf, "prefer-light") != nullptr) return 0;
    // 'default' / empty → Unknown（交给应用默认）
    return 2;
}

} // namespace

extern "C" int32_t composekn_linux_system_theme(void) {
    uint32_t portal = 0;
    if (readPortalColorScheme(&portal)) {
        const int32_t mapped = fromPortalColorScheme(portal);
        if (mapped != 2) return mapped;
    }
    return fromGsettings();
}

package com.vanquzie.tikprofilebanner;

import android.app.Application;
import android.content.Context;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicBoolean;

import io.github.libxposed.api.XposedModule;
import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam;

public final class TikProfileBannerModule extends XposedModule {
    private static final String TAG = "TikProfileBanner";
    static final String TARGET_PACKAGE = "com.zhiliaoapp.musically";
    static final String PREFS = "module_settings";
    static final String KEY_ENABLE_PROFILE_BANNER = "enable_profile_banner";

    private final AtomicBoolean initialized = new AtomicBoolean(false);
    private volatile Context pendingContext;

    @Override
    public void onPackageReady(PackageReadyParam param) {
        if (!param.isFirstPackage()
                || !TARGET_PACKAGE.equals(param.getPackageName())) {
            return;
        }

        try {
            Method attach = Application.class.getDeclaredMethod("attach", Context.class);
            hook(attach)
                    .setId("tpb-attach")
                    .intercept(chain -> {
                        Object result = chain.proceed();
                        if (initialized.compareAndSet(false, true)) {
                            Object context = chain.getArg(0);
                            if (context instanceof Context) {
                                pendingContext = (Context) context;
                                install(param.getClassLoader());
                            }
                        }
                        return result;
                    });
        } catch (Throwable error) {
            logError("Unable to hook Application.attach", error);
        }
    }

    void install(ClassLoader classLoader) {
        boolean enabled = true;
        try {
            enabled = getRemotePreferences(PREFS)
                    .getBoolean(KEY_ENABLE_PROFILE_BANNER, true);
        } catch (Throwable error) {
            logError("Unable to read settings; defaulting to enabled", error);
        }
        logInfo("Enable profile banner setting: " + enabled);
        if (!enabled) {
            logInfo("Profile banner force disabled; skipping hooks");
            return;
        }
        String version = readTikTokVersion();
        try {
            new BannerHooks(this).install(classLoader);
            logInfo("Completed profile banner force hook installation [TikTok "
                    + version + "]");
        } catch (Throwable error) {
            logError("Unable to install profile banner hooks", error);
        }
    }

    private String readTikTokVersion() {
        try {
            Context context = pendingContext;
            if (context == null) {
                return "unknown";
            }
            String versionName = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0)
                    .versionName;
            return versionName == null ? "unknown" : versionName;
        } catch (android.content.pm.PackageManager.NameNotFoundException
                 | RuntimeException ignored) {
            return "unknown";
        }
    }

    private void logInfo(String message) {
        log(4, TAG, message);
    }

    private void logError(String message, Throwable error) {
        log(6, TAG, message, error);
    }
}

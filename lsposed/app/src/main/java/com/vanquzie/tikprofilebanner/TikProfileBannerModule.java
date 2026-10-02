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
                        // Null-guard: LSPatch has delivered null here (see LSPosed/LSPatch#178).
                        // Never let anything escape: intercept runs inside TikTok's bind.
                        try {
                            tryInstall(param.getClassLoader(), chain.getArg(0));
                        } catch (Throwable error) {
                            logError("tryInstall from attach missed gracefully", error);
                        }
                        return result;
                    });
        } catch (Throwable error) {
            logError("Unable to hook Application.attach", error);
        }

        try {
            Method onCreate = Application.class.getDeclaredMethod("onCreate");
            hook(onCreate)
                    .setId("tpb-create")
                    .intercept(chain -> {
                        Object result = chain.proceed();
                        // Never let anything escape: intercept runs inside TikTok's bind.
                        try {
                            tryInstall(param.getClassLoader(), chain.getThisObject());
                        } catch (Throwable error) {
                            logError("tryInstall from onCreate missed gracefully", error);
                        }
                        return result;
                    });
        } catch (Throwable error) {
            logError("Unable to hook Application.onCreate", error);
        }
    }

    /** Idempotent installer entry: runs the hook install at most once. */
    void tryInstall(ClassLoader fallbackLoader, Object contextObj) {
        if (initialized.get()) {
            return;
        }
        // Null-guard: a null/non-Context arg must not consume the one-shot flag;
        // return before pickLoader/CAS so the onCreate fallback can win later.
        Context context = contextObj instanceof Context ? (Context) contextObj : null;
        if (context == null) {
            return;
        }
        ClassLoader loader = pickLoader(context, fallbackLoader);
        if (loader == null) {
            return;
        }
        if (initialized.compareAndSet(false, true)) {
            if (context != null) {
                pendingContext = context;
            }
            install(loader);
        }
    }

    /** Freshest classloader first: LSPatch swaps loaders post-bootstrap. */
    private static ClassLoader pickLoader(Context context, ClassLoader fallback) {
        if (context != null) {
            try {
                ClassLoader fromContext = context.getClassLoader();
                if (fromContext != null) {
                    return fromContext;
                }
            } catch (Throwable ignored) {
            }
        }
        // Hoisted outside the context gate: the LSPatch attach(null) case still
        // tries the (already fresh) thread loader before the param-loader fallback.
        try {
            ClassLoader fromThread = Thread.currentThread().getContextClassLoader();
            if (fromThread != null) {
                return fromThread;
            }
        } catch (Throwable ignored) {
        }
        return fallback;
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
        } catch (Throwable ignored) {
            // Log-only path: IPC can throw TransactionTooLarge/DeadSystem/Security
            // variants beyond NameNotFound/RuntimeException.
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

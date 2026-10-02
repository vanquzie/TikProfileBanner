package com.vanquzie.tikprofilebanner;

import java.lang.reflect.Method;

import io.github.libxposed.api.XposedModule;

final class BannerHooks {
    private static final String TAG = "TikProfileBanner";
    private static final String MASTER_GATE = "X.0OSG";
    private static final String BACKGROUND_COMPONENT =
            "com.ss.android.ugc.profile.platform.business.background.ProfileBackgroundComponent";

    private final XposedModule module;

    BannerHooks(XposedModule module) {
        this.module = module;
    }

    void install(ClassLoader classLoader) {
        Class<?> gate;
        try {
            gate = Class.forName(MASTER_GATE, false, classLoader);
        } catch (Throwable error) {
            logInfo("TikProfileBanner: master gate class unavailable (" + error.getMessage() + ")");
            gate = null;
        }
        if (gate != null) {
            try {
                Method lizj = gate.getDeclaredMethod("LIZJ", boolean.class);
                lizj.setAccessible(true);
                module.hook(lizj)
                        .setId("tpb-master-gate")
                        .intercept(chain -> Boolean.TRUE);
                logInfo("TikProfileBanner: master gate LIZJ forced true");
            } catch (Throwable error) {
                logInfo("TikProfileBanner: master gate LIZJ unavailable (" + error.getMessage() + ")");
            }
            try {
                Method liziz = gate.getDeclaredMethod("LIZIZ");
                liziz.setAccessible(true);
                module.hook(liziz)
                        .setId("tpb-immersive-gate")
                        .intercept(chain -> Boolean.TRUE);
                logInfo("TikProfileBanner: immersive gate LIZIZ forced true");
            } catch (Throwable error) {
                logInfo("TikProfileBanner: immersive gate LIZIZ unavailable (" + error.getMessage() + ")");
            }
        }
        try {
            Class<?> component = Class.forName(BACKGROUND_COMPONENT, false, classLoader);
            Method ds = component.getDeclaredMethod("Ds");
            ds.setAccessible(true);
            module.hook(ds)
                    .setId("tpb-low-device-gate")
                    .intercept(chain -> Boolean.FALSE);
            logInfo("TikProfileBanner: low-device gate Ds forced false");
        } catch (Throwable error) {
            logInfo("TikProfileBanner: low-device gate Ds unavailable (" + error.getMessage() + ")");
        }
    }

    private void logInfo(String message) {
        module.log(4, TAG, message);
    }

    private void logError(String message, Throwable error) {
        module.log(6, TAG, message, error);
    }
}

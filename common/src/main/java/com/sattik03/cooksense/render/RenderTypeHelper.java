package com.sattik03.cooksense.render;

import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Resolves text render types dynamically to support both Minecraft 1.21.9 - 1.21.10
 * (where methods were on RenderType / class_1921) and Minecraft 1.21.11+
 * (where methods were relocated to RenderTypes / class_12249).
 */
public class RenderTypeHelper {
    private static final MethodHandle TEXT_BACKGROUND_MH;
    private static final MethodHandle TEXT_BACKGROUND_SEE_THROUGH_MH;
    private static final MethodHandle TEXT_SEE_THROUGH_MH;

    static {
        MethodHandle bg = null;
        MethodHandle bgSeeThrough = null;
        MethodHandle seeThrough = null;

        MethodHandles.Lookup lookup = MethodHandles.lookup();

        String[] classCandidates = {
                "net.minecraft.client.renderer.rendertype.RenderTypes",
                "net.minecraft.class_12249",
                "net.minecraft.client.renderer.rendertype.RenderType",
                "net.minecraft.client.renderer.RenderType",
                "net.minecraft.class_1921"
        };

        String[] bgNames = {"textBackground", "method_75999", "method_49045"};
        String[] bgSeeThroughNames = {"textBackgroundSeeThrough", "method_76001", "method_49046"};
        String[] seeThroughNames = {"textSeeThrough", "method_76028", "method_23030"};

        List<Class<?>> classes = new java.util.ArrayList<>();
        try {
            classes.add(RenderType.class);
        } catch (Throwable ignored) {}

        ClassLoader cl = RenderType.class.getClassLoader();
        if (cl == null) {
            cl = RenderTypeHelper.class.getClassLoader();
        }
        if (cl == null) {
            cl = Thread.currentThread().getContextClassLoader();
        }

        for (String className : classCandidates) {
            try {
                Class<?> clazz = Class.forName(className, false, cl);
                if (!classes.contains(clazz)) {
                    classes.add(clazz);
                }
            } catch (Throwable ignored) {}
        }

        for (Class<?> clazz : classes) {
            if (bg == null) {
                for (String name : bgNames) {
                    try {
                        Method m = clazz.getMethod(name);
                        bg = lookup.unreflect(m);
                        break;
                    } catch (Throwable ignored) {}
                }
            }
            if (bgSeeThrough == null) {
                for (String name : bgSeeThroughNames) {
                    try {
                        Method m = clazz.getMethod(name);
                        bgSeeThrough = lookup.unreflect(m);
                        break;
                    } catch (Throwable ignored) {}
                }
            }
            if (seeThrough == null) {
                for (String name : seeThroughNames) {
                    for (Method m : clazz.getMethods()) {
                        if (m.getName().equals(name) && m.getParameterCount() == 1) {
                            try {
                                seeThrough = lookup.unreflect(m);
                                break;
                            } catch (Throwable ignored) {}
                        }
                    }
                    if (seeThrough != null) break;
                }
            }
        }

        TEXT_BACKGROUND_MH = bg;
        TEXT_BACKGROUND_SEE_THROUGH_MH = bgSeeThrough;
        TEXT_SEE_THROUGH_MH = seeThrough;
    }

    public static RenderType textBackground() {
        try {
            if (TEXT_BACKGROUND_MH != null) {
                return (RenderType) TEXT_BACKGROUND_MH.invoke();
            }
        } catch (Throwable t) {
            throw new RuntimeException("CookSense: Failed to get textBackground RenderType", t);
        }
        throw new IllegalStateException("CookSense: textBackground RenderType not found on classpath");
    }

    public static RenderType textBackgroundSeeThrough() {
        try {
            if (TEXT_BACKGROUND_SEE_THROUGH_MH != null) {
                return (RenderType) TEXT_BACKGROUND_SEE_THROUGH_MH.invoke();
            }
        } catch (Throwable t) {
            throw new RuntimeException("CookSense: Failed to get textBackgroundSeeThrough RenderType", t);
        }
        throw new IllegalStateException("CookSense: textBackgroundSeeThrough RenderType not found on classpath");
    }

    public static RenderType textSeeThrough(Identifier id) {
        try {
            if (TEXT_SEE_THROUGH_MH != null) {
                return (RenderType) TEXT_SEE_THROUGH_MH.invoke(id);
            }
        } catch (Throwable t) {
            throw new RuntimeException("CookSense: Failed to get textSeeThrough RenderType", t);
        }
        throw new IllegalStateException("CookSense: textSeeThrough RenderType not found on classpath");
    }
}

package com.github.ahitm_2020_2025.blackonionbot.utils;

import java.io.File;

public class JarUtils
{
    public static String getJarName()
    {
        return new File(JarUtils.class.getProtectionDomain()
                                .getCodeSource()
                                .getLocation()
                                .getPath())
                       .getName();
    }

    public static boolean runningFromJar()
    {
        return getJarName().contains(".jar");
    }
}
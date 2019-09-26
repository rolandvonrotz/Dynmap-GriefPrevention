package org.dynmap.griefprevention;

import org.bukkit.Bukkit;

import java.util.logging.Level;

public class Log {
    public static void Info(final String msg) {
        log(Level.INFO, msg);
    }

    public static void Error(final String msg) {
        log(Level.SEVERE, msg);
    }

    private static void log(final Level level, final String msg) {
        final String logMessage = String.format("[%s] %s", DynmapGriefPreventionPlugin.INSTANCE.getName(), msg);
        Bukkit.getLogger().log(level, logMessage);
    }

}

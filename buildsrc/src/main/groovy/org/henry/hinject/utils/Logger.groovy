package org.henry.hinject.utils

public class Logger {
    def static boolean debug = true

    public static void setDebug(boolean debug) {
        Logger.@debug = debug
    }

    public static void log(Class clazz, String msg) {
        if (debug) {
            println("HInject Logger from " + clazz.name + " : " + msg)
        }
    }
}
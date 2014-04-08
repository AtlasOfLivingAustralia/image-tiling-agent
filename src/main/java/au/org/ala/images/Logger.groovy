package au.org.ala.images

import au.org.ala.images.util.CodeTimer

import java.text.SimpleDateFormat;

public class Logger {

    public static void log(String message) {
        def sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss")
        println("[${sdf.format(new Date())}] ${message}")
    }

    public static void logTimer(CodeTimer ct) {
        ct.stop()
        log("${ct.description} took ${ct.elapsedMillis} ms")
    }
}

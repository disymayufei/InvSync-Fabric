package cn.disy920.invsync.utils;

import org.slf4j.LoggerFactory;

public final class Logger {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger("Cloud Inventory");
    public static void info(Object msg){
        logger.info("[INFO] " + msg);
    }

    public static void warn(Object msg){
        logger.warn("[WARN] " + msg);
    }

    public static void error(Object msg){
        logger.error("[ERR] " + msg);
    }
}

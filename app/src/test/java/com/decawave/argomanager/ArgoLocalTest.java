package com.decawave.argomanager;

import org.junit.BeforeClass;
import org.junit.Test;

import eu.kryl.android.common.log.ComponentLog;
import eu.kryl.android.common.log.LogLevel;

/**
 *
 */
public class ArgoLocalTest {

    @BeforeClass
    public static void setUp() {
        if (ComponentLog.MAIN_PACKAGE_NAME == null) {
            ComponentLog.MAIN_PACKAGE_NAME = ArgoLocalTest.class.getPackage().getName();
            ComponentLog.APP_TAG = "ARGO";
            ComponentLog.DEFAULT_LOG_LEVEL = LogLevel.DEBUG;
        }
    }

    @Test
    public void testShortConversion() {
        long l = 0x981C;
        System.out.println("long value: " + l);
        System.out.println("short value: " + new Long(l).shortValue());
        System.out.println("shorten value: " + shortenNodeId(l));
    }


    private static long shortenNodeId(long nodeId) {
        return 0x00FFFF & nodeId;
    }


}

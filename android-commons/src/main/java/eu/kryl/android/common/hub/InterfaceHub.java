
/*
 * Copyright 2017, Pavel Kryl
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package eu.kryl.android.common.hub;

import org.jetbrains.annotations.NotNull;

import eu.kryl.android.common.log.ComponentLog;

/**
 * IH made static.
 *
 * Copes properly with a testing environment.
 */
public class InterfaceHub {
    // logging
    final static boolean LOG_ENABLED = false;
    static final ComponentLog log = new ComponentLog("hub.InterfaceHub").setEnabled(LOG_ENABLED);
    // delegation
    private static InterfaceHubContract delegate;
    private static Thread testingThread, mainThread;


    private static synchronized InterfaceHubContract getImpl() {
        Thread currentThread = Thread.currentThread();
        if (currentThread == testingThread) {
            return delegate;
        } else if (testingThread != null) {
            // we already have a reference to testing thread, but it's not this one
            // return VOID instance
            return InterfaceHubVoid.INSTANCE;
        } else if (!(testingThread == null && currentThread == mainThread)) {
            // inverting condition: this is main thread and testing thread did not appear yet
            if (currentThread.getName().contains("android.test.InstrumentationTestRunner")) {
                testingThread = currentThread;
                // this is the first use from a testing thread, reset the delegate
                delegate.reset();
            } else if (mainThread == null) {
                // this is not a testing thread, and main thread has not been set yet - it must be main thread
                mainThread = currentThread;
            }
        }
        return delegate;
    }

    public static synchronized void reset() {
        getImpl().reset();
    }

    public static synchronized boolean isHandlerRegistered(Class<? extends InterfaceHubHandler> handlerClass) {
        return getImpl().isHandlerRegistered(handlerClass);
    }

    public static synchronized boolean isHandlerRegistered(InterfaceHubHandler handlerInstance) {
        return getImpl().isHandlerRegistered(handlerInstance);
    }

    public static synchronized boolean isHandlerImplRegistered(Class<? extends InterfaceHubHandler> iface, Class<?> clazz) {
        return getImpl().isHandlerImplRegistered(iface, clazz);
    }

    public static synchronized void registerHandler(InterfaceHubHandler handler) {
        getImpl().registerHandler(handler);
    }

    // this is reliableDelivery='false' version
    public static synchronized @NotNull
    <T extends InterfaceHubHandler> T getHandlerHub(Class<T> handlerClass) {
        return getImpl().getHandlerHub(handlerClass);
    }

    public static synchronized @NotNull <T extends InterfaceHubHandler> T getHandlerHub(Class<T> handlerClass, InterfaceHubContract.Delivery delivery) {
        return getImpl().getHandlerHub(handlerClass, delivery);
    }

    public static synchronized void unregisterHandler(InterfaceHubHandler handlerInstance) {
        getImpl().unregisterHandler(handlerInstance);
    }

    /**
     * unregister all handler instances registered for type @param clazz
     */
    public static synchronized void unregisterHandler(Class<?> clazz) {
        getImpl().unregisterHandler(clazz);
    }

    /**
     * maps an interface identified by its ID (which should be declared: ID =
     * IFACE.class.getName().hashCode();) supplied as @param interfaceID, and an
     * implementation of this interface supplied in @param handler.
     *
     * @return true on success
     */
    public static synchronized void registerHandler(InterfaceHubHandler handler, boolean weak) {
        getImpl().registerHandler(handler, weak);
    }

    static {
        // the default IH will be threadsafe with UI_THREAD delivery
        delegate = InterfaceHubFactory.newThreadSafeIh(InterfaceHubContract.Delivery.UI_THREAD);
    }

}

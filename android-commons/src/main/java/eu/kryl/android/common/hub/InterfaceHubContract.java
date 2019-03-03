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

import java.util.Set;

/**
 * Interface hub interface (contract).
 */
public interface InterfaceHubContract {

    enum Delivery {
        SAME_THREAD_IMMEDIATE,
        UI_THREAD,
        UI_THREAD_POST,
        RELIABLE
    }

    void reset();

    boolean isHandlerRegistered(Class<? extends InterfaceHubHandler> handlerClass);

    boolean isHandlerRegistered(InterfaceHubHandler handlerInstance);

    boolean isHandlerImplRegistered(Class<? extends InterfaceHubHandler> iface, Class<?> clazz);

    /**
     * Same as {@link #registerHandler(InterfaceHubHandler, boolean)} with
     * weak = false.
     * @param handler
     */
    void registerHandler(InterfaceHubHandler handler);

    // this is reliableDelivery='false' version
    @NotNull <T extends InterfaceHubHandler> T getHandlerHub(Class<T> handlerClass);

    @NotNull <T extends InterfaceHubHandler> T getHandlerHub(Class<T> handlerClass, Delivery delivery);

    void unregisterHandler(InterfaceHubHandler handlerInstance);

    /**
     * unregister all handler instances registered for type @param clazz
     * @return set of removed handlers
     */
    Set<InterfaceHubHandler> unregisterHandler(Class<?> clazz);

    /**
     * maps an interface identified by its ID (which should be declared: ID =
     * IFACE.class.getName().hashCode();) supplied as @param interfaceID, and an
     * implementation of this interface supplied in @param handler.
     *
     * @return true on success
     */
    void registerHandler(InterfaceHubHandler handler, boolean weak);

}

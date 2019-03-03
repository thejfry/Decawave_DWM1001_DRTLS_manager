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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 *
 */
class InterfaceHubVoid implements InterfaceHubContract {
    public static final InterfaceHubVoid INSTANCE = new InterfaceHubVoid();

    private Map<Class<?>, Object> proxies = new HashMap<>();


    private final <T> T getProxyFor(Class<T> cls) {
        T proxy = (T) proxies.get(cls);
        if (proxy == null) {
            proxy = (T) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] { cls },
                    new InvocationHandler() {
                        @Override
                        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                            // do nothing
                            return null;
                        }
                    }
            );
        }
        return proxy;
    }

    @Override
    public void reset() {
    }

    @Override
    public boolean isHandlerRegistered(Class<? extends InterfaceHubHandler> handlerClass) {
        return false;
    }

    @Override
    public boolean isHandlerRegistered(InterfaceHubHandler handlerInstance) {
        return false;
    }

    @Override
    public boolean isHandlerImplRegistered(Class<? extends InterfaceHubHandler> iface, Class<?> clazz) {
        return false;
    }

    @Override
    public void registerHandler(InterfaceHubHandler handler) {

    }

    @NotNull
    @Override
    public <T extends InterfaceHubHandler> T getHandlerHub(Class<T> handlerClass) {
        return getHandlerHub(handlerClass, Delivery.SAME_THREAD_IMMEDIATE);
    }

    @NotNull
    @Override
    public <T extends InterfaceHubHandler> T getHandlerHub(Class<T> handlerClass, Delivery delivery) {
        return (T) getProxyFor(handlerClass);
    }

    @Override
    public void unregisterHandler(InterfaceHubHandler handlerInstance) {

    }

    @Override
    public Set<InterfaceHubHandler> unregisterHandler(Class<?> clazz) {
        return Collections.emptySet();
    }

    @Override
    public void registerHandler(InterfaceHubHandler handler, boolean weak) {

    }
}

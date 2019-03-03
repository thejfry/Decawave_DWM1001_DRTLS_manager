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

import android.os.Looper;

import com.google.common.base.Preconditions;

import org.jetbrains.annotations.NotNull;

import java.util.Set;

import eu.kryl.android.common.Constants;

/**
 * IH with a restriction that it can run on a single looper / thread.
 *
 * @see InterfaceHubFactory
 */
class InterfaceHubContractSingleLooperImpl implements InterfaceHubContract {
    private final InterfaceHubContract delegate;

    private volatile Looper owningLooper = null;


    InterfaceHubContractSingleLooperImpl(InterfaceHubContract delegate) {
        this.delegate = delegate;
    }

    @Override
    public void reset() {
        owningLooper = null;
        delegate.reset();
    }

    @Override
    public boolean isHandlerRegistered(Class<? extends InterfaceHubHandler> handlerClass) {
        if (Constants.DEBUG) {
            checkLooperThread();
        }
        return delegate.isHandlerRegistered(handlerClass);
    }

    @Override
    public boolean isHandlerRegistered(InterfaceHubHandler handlerInstance) {
        if (Constants.DEBUG) {
            checkLooperThread();
        }
        return delegate.isHandlerRegistered(handlerInstance);
    }

    @Override
    public boolean isHandlerImplRegistered(Class<? extends InterfaceHubHandler> iface, Class<?> clazz) {
        if (Constants.DEBUG) {
            checkLooperThread();
        }
        return delegate.isHandlerImplRegistered(iface, clazz);
    }

    @Override
    public void registerHandler(InterfaceHubHandler handler) {
        if (Constants.DEBUG) {
            checkLooperThread();
        }
        delegate.registerHandler(handler);
    }

    @Override
    @NotNull
    public <T extends InterfaceHubHandler> T getHandlerHub(Class<T> handlerClass) {
        if (Constants.DEBUG) {
            checkLooperThread();
        }
        return delegate.getHandlerHub(handlerClass);
    }

    @Override
    @NotNull
    public <T extends InterfaceHubHandler> T getHandlerHub(Class<T> handlerClass, Delivery delivery) {
        if (Constants.DEBUG) {
            checkLooperThread();
        }
        return delegate.getHandlerHub(handlerClass, delivery);
    }

    @Override
    public void unregisterHandler(InterfaceHubHandler handlerInstance) {
        if (Constants.DEBUG) {
            checkLooperThread();
        }
        delegate.unregisterHandler(handlerInstance);
    }

    @Override
    public Set<InterfaceHubHandler> unregisterHandler(Class<?> clazz) {
        if (Constants.DEBUG) {
            checkLooperThread();
        }
        return delegate.unregisterHandler(clazz);
    }

    @Override
    public void registerHandler(InterfaceHubHandler handler, boolean weak) {
        if (Constants.DEBUG) {
            checkLooperThread();
        }
        delegate.registerHandler(handler, weak);
    }

    private void checkLooperThread() {
        // check/initialize owning looper
        if (owningLooper == null) {
            // initialize
            owningLooper = Looper.myLooper();
            //new Exception("owning looper initialized here! thread: " + Thread.currentThread()).printStackTrace();
        } else {
            Preconditions.checkState(
                    owningLooper == Looper.myLooper(),
                    "InterfaceHub is not thread-safe, always use it from one thread only! thread: " + Thread.currentThread());

        }
    }

}


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

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import eu.kryl.android.common.Constants;
import eu.kryl.android.common.async.SbHandler;

/**
 * Default InterfaceHubContract thread safe implementation.
 * nowOrNever is the default behavior!
 *
 * @see InterfaceHubFactory
 */
class InterfaceHubContractThreadSafeImpl implements InterfaceHubContract {
    private static SbHandler uiHandler;

    static void setUiHandler(SbHandler uiHandler) {
        InterfaceHubContractThreadSafeImpl.uiHandler = uiHandler;
    }

    private Map<Class<?>, List<HandlerReference>> hub = new HashMap<>();

    //
    private Delivery defaultDelivery;

    // proxy caches
    private Map<Class<?>, InterfaceHubHandler>[] proxyCaches;

    // cache which maps instance classes to implemented interface hub interfaces
    private Map<Class<? extends InterfaceHubHandler>, List<Class<?>>> ihIfacesByIhClassCache = new HashMap<Class<? extends InterfaceHubHandler>, List<Class<?>>>();

    private List<MethodInvocation> pendingInvocations = new LinkedList<MethodInvocation>();

    public interface HandlerHub {

        public boolean anyHandlers();

        public Class<? extends InterfaceHubHandler> getHandlerClass();

    }

    public InterfaceHubContractThreadSafeImpl(Delivery defaultDeliveryPolicy) {
        //noinspection unchecked
        proxyCaches = new Map[Delivery.values().length];
        for (int i = 0; i < proxyCaches.length; i++) {
            proxyCaches[i] = Maps.newHashMap();
        }
        this.defaultDelivery = defaultDeliveryPolicy;
    }

    @Override
    public synchronized void reset() {
        if (InterfaceHub.log.isEnabled()) {
            InterfaceHub.log.d("reset() called from " + Thread.currentThread().getName());
        }
        pendingInvocations.clear();
        ihIfacesByIhClassCache.clear();
        for (Map<Class<?>, InterfaceHubHandler> proxyCach : proxyCaches) {
            proxyCach.clear();
        }
        hub.clear();
    }

    @Override
    public synchronized boolean isHandlerRegistered(Class<? extends InterfaceHubHandler> handlerClass) {
        return !getHandlerRefs(handlerClass, false).isEmpty();
    }

    @Override
    public synchronized boolean isHandlerRegistered(InterfaceHubHandler handlerInstance) {
        for (Map.Entry<Class<?>, List<HandlerReference>> entry : hub.entrySet()) {
            if (entry.getKey().isAssignableFrom(handlerInstance.getClass())) {
                // there is a chance that we will find instance in this chain
                for (HandlerReference hr : entry.getValue()) {
                    if (hr.refersToHandler(handlerInstance))
                        return true;
                }
            }
        }
        return false;
    }

    private @NotNull
    <T extends InterfaceHubHandler> List<Class<?>> getHandlerClassIfaces(Class<T> handlerClass) {
        List<Class<?>> retVal = ihIfacesByIhClassCache.get(handlerClass);
        if (retVal == null) {
            HashSet<Class<?>> ifaceSet = new HashSet<Class<?>>();
            collectAllIhInterfaces(handlerClass, ifaceSet);
            if (!ifaceSet.isEmpty()) {
                retVal = new LinkedList<Class<?>>(ifaceSet);
            } else {
                retVal = Collections.emptyList();
            }
            ihIfacesByIhClassCache.put(handlerClass, retVal);
        }
        return retVal;
    }
    
    @SuppressWarnings("unchecked")
    private void collectAllIhInterfaces(Class<? extends InterfaceHubHandler> handlerClass, Set<Class<?>> ihInterfaces) {
        Class<?>[] ifaces = handlerClass.getInterfaces();
        // check interfaces first
        for (Class<?> i : ifaces) {
            if (/* proguard: i.getSimpleName().startsWith("Ih") && */ InterfaceHubHandler.class.isAssignableFrom(i) && i.getDeclaredMethods().length != 0) {
                ihInterfaces.add(i);
                // check if this interface does form Ih hierarchy
                collectAllIhInterfaces((Class<? extends InterfaceHubHandler>) i, ihInterfaces);
            }
        }
        // now invoke recursively for super class
        Class<?> superclass = handlerClass.getSuperclass();
        if (superclass != null && !Object.class.equals(superclass) && InterfaceHubHandler.class.isAssignableFrom(superclass)) {
            // invoke recursively
            collectAllIhInterfaces((Class<? extends InterfaceHubHandler>) superclass, ihInterfaces);
        }
    }
    
    /**
     * @return true if @param clazz implementing @param iface has been
     *         registered
     */
    @Override
    public synchronized boolean isHandlerImplRegistered(Class<? extends InterfaceHubHandler> iface, Class<?> clazz) {
        for (Map.Entry<Class<?>, List<HandlerReference>> entry : hub.entrySet()) {
            if (entry.getKey().isAssignableFrom(iface)) {
                for (HandlerReference hr : entry.getValue()) {
                    if (hr.getHandler().getClass().isAssignableFrom(clazz))
                        return true;
                }
            }
        }
        return false;
    }

    @Override
    public synchronized void registerHandler(InterfaceHubHandler handler) {
        registerHandler(handler, false);
    }

    // this is reliableDelivery='false' version
    @Override
    public synchronized @NotNull
    <T extends InterfaceHubHandler> T getHandlerHub(Class<T> handlerClass) {
        return getProxyForHandlerClass(handlerClass, defaultDelivery);
    }

    @Override
    public synchronized @NotNull
    <T extends InterfaceHubHandler> T getHandlerHub(Class<T> handlerClass, Delivery delivery) {
        return getProxyForHandlerClass(handlerClass, delivery);
    }

    /**
     *
     */
    @Override
    public synchronized void unregisterHandler(InterfaceHubHandler handlerInstance) {
        List<Class<?>> hIfaces = getHandlerClassIfaces(handlerInstance.getClass());
        for (Class<?> hIface : hIfaces) {
            final List<HandlerReference> handlerRefs = getHandlerRefs(hIface, false);
            boolean removed = false;
            Iterator<HandlerReference> it = handlerRefs.iterator();
            while (it.hasNext()) {
                final HandlerReference hr = it.next();
                if (hr.refersToHandler(handlerInstance)) {
                    if (hr.isWeak())
                        hr.clear(); // for sure
                    removed = true;
                    it.remove();
                    break;
                }
            }
            if (!removed) {
                // not found
                InterfaceHub.log.w("trying to unregister not-registered handler " + getHandlerInstanceName(handlerInstance) + " skipping");
                return;
            } else if (InterfaceHub.LOG_ENABLED) {
                InterfaceHub.log.d("handler " + getHandlerInstanceName(handlerInstance) + " unregistered for " + hIface.getSimpleName());
            }
        }
    }

    /**
     * unregister all handler instances registered for type @param clazz
     */
    @Override
    public synchronized Set<InterfaceHubHandler> unregisterHandler(Class<?> clazz) {
        Set<InterfaceHubHandler> retVal = new HashSet<>();
        for (Map.Entry<Class<?>, List<HandlerReference>> entry : hub.entrySet()) {
            if (entry.getKey().isAssignableFrom(clazz)) {
                for (HandlerReference hr : new ArrayList<HandlerReference>(entry.getValue())) {
                    if (hr.getHandler().getClass().isAssignableFrom(clazz))
                        retVal.add(hr.getHandler());
                        unregisterHandler(hr.getHandler());
                }
            }
        }
        return retVal;
    }

    private final String getHandlerInstanceName(InterfaceHubHandler handler) {
        final String hdsc = handler.toString();
        return hdsc.substring(hdsc.lastIndexOf('.') + 1);
    }

    /**
     * maps an interface identified by its ID (which should be declared: ID =
     * IFACE.class.getName().hashCode();) supplied as @param interfaceID, and an
     * implementation of this interface supplied in @param handler.
     * 
     * @return true on success
     */
    @Override
    public synchronized void registerHandler(InterfaceHubHandler handler, boolean weak) {
        List<Class<?>> hIfaces = getHandlerClassIfaces(handler.getClass());
        if (hIfaces.isEmpty()) {
            throw new IllegalStateException("handler does not implement any InterfaceHubHandler: " + handler);
        }
        for (Class<?> hIface : hIfaces) {
            final List<HandlerReference> handlerChain = getHandlerRefs(hIface, true);
            if (SingletonHandler.class.isAssignableFrom(hIface) && !handlerChain.isEmpty()) {
                // ////////////////////////////////////////////////////////////////////////
                // there must be only one instance of SingletonHandler
                HandlerReference hr = handlerChain.get(0);
                InterfaceHub.log.w("replacing SingletonHandler " + hr.getHandler() + " registered with a new one " + handler, hr.allocateStackTrace);
                if (hr.isWeak())
                    hr.clear(); // for sure
                handlerChain.remove(0);
                if (Constants.DEBUG) {
                    Preconditions.checkState(handlerChain.isEmpty());
                }
            } else {
                // ////////////////////////////////////////////////////////////////////////
                // check that the instance we are going to register is not already
                // registered
                for (HandlerReference hr : handlerChain) {
                    if (hr.refersToHandler(handler)) {
                        // if we have an instance already registered terminate
                        if (InterfaceHub.LOG_ENABLED)
                            InterfaceHub.log.d("handler " + getHandlerInstanceName(handler) + " already registered, skipping");
                        return;
                    }
                }
            }
            // register the handler into the existing queue
            handlerChain.add(weak ? new HandlerReference(new WeakReference<InterfaceHubHandler>(handler)) : new HandlerReference(handler));
            if (InterfaceHub.LOG_ENABLED)
                InterfaceHub.log.d("handler " + getHandlerInstanceName(handler) + " registered [" + (weak ? "weak" : "hard") + "] for "
                        + hIface.getSimpleName());
        }
        // do dispatch - in case we have any message for the just registered
        // handler
        dispatchPendingInvocations(handler);
    }

    private @NotNull
    List<HandlerReference> getHandlerRefs(Class<?> handlerClass, boolean initIfMissing) {
        // returns handler chain associated with given handler class
        final List<HandlerReference> existingChain = hub.get(handlerClass);
        if (existingChain != null) {
            return existingChain;
        } else if (initIfMissing) {
            final LinkedList<HandlerReference> retVal = new LinkedList<HandlerReference>();
            hub.put(handlerClass, retVal);
            return retVal;
        } else {
            return Collections.emptyList();
        }
    }

    @SuppressWarnings("unchecked")
    private <T extends InterfaceHubHandler> T getProxyForHandlerClass(Class<T> handlerClass, Delivery delivery) {
        final Map<Class<?>, InterfaceHubHandler> proxyCache = proxyCaches[delivery.ordinal()];
        T retVal = (T) proxyCache.get(handlerClass);
        if (retVal == null) {
            retVal = createProxyForHandlerClass(handlerClass, delivery);
            proxyCache.put(handlerClass, retVal);
        }
        return retVal;
    }

    private static class MethodInvocation {
        private Class<? extends InterfaceHubHandler> handlerClass;
        private Method method;
        private Object args[];

        public MethodInvocation(Class<? extends InterfaceHubHandler> handlerClass, Method method, Object[] args) {
            this.handlerClass = handlerClass;
            this.method = method;
            this.args = args;
        }

        public Object invokeHandler(final InterfaceHubHandler handlerInstance) {
            return _invoke(handlerInstance);
        }

        private Object _invoke(InterfaceHubHandler handlerInstance) {
            try {
                return method.invoke(handlerInstance, args);
            } catch (IllegalArgumentException e) {
                throw new RuntimeException(e);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            } catch (InvocationTargetException e) {
                Throwable sourceException = e.getTargetException();
                if (sourceException != null) {
                    if (sourceException instanceof RuntimeException) {
                        throw (RuntimeException) sourceException;
                    } else {
                        sourceException.printStackTrace();
                        throw new RuntimeException(e);
                    }
                } else {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    private class IhInvocationHandler implements InvocationHandler, HandlerHub {
        final Delivery delivery;
        final Class<? extends InterfaceHubHandler> handlerClass;

        public IhInvocationHandler(Class<? extends InterfaceHubHandler> handlerClass, Delivery delivery) {
            this.handlerClass = handlerClass;
            this.delivery = delivery;
        }

        @Override
        public Object invoke(Object proxy, final Method method, final Object[] args) throws Throwable {
            if (method.getDeclaringClass().equals(HandlerHub.class)) {
                return method.invoke(this, args);
            }
            Object retVal = null;
            switch (delivery) {
                case RELIABLE:
                case SAME_THREAD_IMMEDIATE:
                    // do it now
                    retVal = processInvocation(method, args, delivery == Delivery.RELIABLE);
                    break;
                case UI_THREAD_POST:
                case UI_THREAD:
                    // needs special handling
                    if (delivery == Delivery.UI_THREAD_POST || !uiHandler.currentThreadHandler()) {
                        // we are on a different thread, or post explicitly requested
                        uiHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                processInvocation(method, args, false);
                                // we have nowhere to put the result, simply ignore it
                            }
                        });
                    } else {
                        // do it now
                        retVal = processInvocation(method, args, false);
                    }
                    break;
            }
            // beware, the result might be null in case of postponed invocation
            return retVal;
        }

        /**
         * Processes the IH invocation - distributes the invocation to all registered handlers.
         * @param method method descriptor
         * @param args method invocation arguments
         * @return return value of handler invocation, if there are multiple handlers, the return value is chosen
         * randomly among the registered once
         */
        @Nullable
        private Object processInvocation(Method method, Object[] args, boolean reliableDelivery) {
            boolean wasDispatched = false;
            // retrieve all registered instances of handlerClass
            List<HandlerReference> handlers;
            synchronized (InterfaceHubContractThreadSafeImpl.this) {
                // we need to copy the list (to avoid concurrent modification)
                handlers = new ArrayList<>(getHandlerRefs(handlerClass, false));
            }
            final MethodInvocation mi = new MethodInvocation(handlerClass, method, args);
            Object retVal = null;
            boolean oneOff = OneOffHandler.class.isAssignableFrom(handlerClass);
            // working variable
            InterfaceHubHandler handler;
            if (SingleDispatchHandler.class.isAssignableFrom(handlerClass)) {
                if (InterfaceHub.LOG_ENABLED)
                    InterfaceHub.log.d("detected single dispatch handler invocation");
                // invoke only once - the latest registered handler
                if (!handlers.isEmpty()) {
                    HandlerReference handlerRef = handlers.get(handlers.size() - 1);
                    handler = handlerRef.getHandler();
                    if (handler == null) {
                        handleMissingHandler(handlerRef);
                        // pretend as if dispatched - if exception was not raised
                    } else {
                        retVal = mi.invokeHandler(handler);
                        if (oneOff)
                            unregisterHandler(handler);
                    }
                    wasDispatched = true;
                }
            } else {
                // first copy the handler list - we could register another
                // handler of this kind
                // when going through the loop (then we get
                // ConcurrentModificationException),
                // therefore we create a copy of the registered handlers
                List<HandlerReference> _handlers = new ArrayList<HandlerReference>(handlers);
                for (HandlerReference handlerRef : _handlers) {
                    // invoke handlers one-by-one
                    handler = handlerRef.getHandler();
                    if (handler == null) {
                        handleMissingHandler(handlerRef);
                        continue;
                    }
                    if (InterfaceHub.LOG_ENABLED)
                        InterfaceHub.log.d("invoking method " + method + " on handler " + handler);
                    retVal = mi.invokeHandler(handler);
                    if (oneOff)
                        unregisterHandler(handler);
                    wasDispatched = true;
                }
            }
            if (!wasDispatched && reliableDelivery) {
                // enqueue the invocation
                if (InterfaceHub.LOG_ENABLED)
                    InterfaceHub.log.d("no handler found for " + method.getDeclaringClass().getSimpleName() + "." + method.getName()
                            + " method invocation -> pending invocations");
                synchronized (InterfaceHubContractThreadSafeImpl.this) {
                    pendingInvocations.add(mi);
                }
            }
            return retVal;
        }

        @Override
        public boolean anyHandlers() {
            return getHandlerRefs(handlerClass, false).size() > 0;
        }

        @Override
        public Class<? extends InterfaceHubHandler> getHandlerClass() {
            return handlerClass;
        }
        
        private void handleMissingHandler(HandlerReference handlerRef) {
            // doublecheck: is the handler still registered
            boolean stillRegistered;
            synchronized (InterfaceHubContractThreadSafeImpl.this) {
                stillRegistered = getHandlerRefs(handlerClass, false).contains(handlerRef);
            }
            if (stillRegistered) {
                if (Constants.DEBUG) {
                    handlerRef.throwNullHandlerReference();
                } else {
                    // silently report (wrapped both the current exception, both the registration stacktrace)
                    InterfaceHub.log.e("InterfaceHub: null handler reference, forgot to unregister? handlerClass = " + handlerClass.toString());
                }
            }

        }

    }

    
    @SuppressWarnings("unchecked")
    private <T extends InterfaceHubHandler> T createProxyForHandlerClass(final Class<T> handlerClass,
            final Delivery delivery) {
        return (T) Proxy.newProxyInstance(handlerClass.getClassLoader(),
                new Class[] {
                        handlerClass, HandlerHub.class
                },
                new IhInvocationHandler(handlerClass, delivery));
    }

    protected void dispatchPendingInvocations(InterfaceHubHandler handlerInstance) {
        Iterator<MethodInvocation> it = pendingInvocations.iterator();
        while (it.hasNext()) {
            MethodInvocation mi = it.next();
            if (mi.handlerClass.isAssignableFrom(handlerInstance.getClass())) {
                mi.invokeHandler(handlerInstance);
                // remove from queue
                it.remove();
                if (InterfaceHub.LOG_ENABLED)
                    InterfaceHub.log.d("dispatched pending message " + mi.method.getName() + " via handler " + getHandlerInstanceName(handlerInstance));
                // if this is one-off handler we remove also the handler
                // instance
                if (handlerInstance instanceof OneOffHandler) {
                    if (InterfaceHub.LOG_ENABLED)
                        InterfaceHub.log.d("one-off handler " + getHandlerInstanceName(handlerInstance) + " - removing");
                    unregisterHandler(handlerInstance);
                    break;
                }
            }
        }
    }

}

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

import org.jetbrains.annotations.Nullable;

import java.lang.ref.WeakReference;

class HandlerReference {
    private final InterfaceHubHandler hardRef;
    private final WeakReference<InterfaceHubHandler> weakRef;
    Exception allocateStackTrace;
    private int hashCode;
    
    
    public HandlerReference(InterfaceHubHandler hardRef) {
        Preconditions.checkNotNull(hardRef);
        this.hardRef = hardRef;
        this.weakRef = null;
        this.allocateStackTrace = new Exception(hardRef.getClass().toString());
        this.hashCode = hardRef.hashCode();
    }

    public HandlerReference(WeakReference<InterfaceHubHandler> weakRef) {
        Preconditions.checkNotNull(weakRef);
        this.weakRef = weakRef;
        this.hardRef = null;
        this.allocateStackTrace = new Exception(weakRef.getClass().toString());
        this.hashCode = weakRef.get().hashCode();
    }

    public boolean refersToHandler(InterfaceHubHandler handler) {
        if(weakRef != null) {
            return weakRef.get() == handler;
        } else {
            return hardRef == handler;
        }
    }
    
    public boolean isWeak() {
        return weakRef != null;
    }
    
    public void clear() {
        if(weakRef != null) weakRef.clear();
    }
    
    public @Nullable InterfaceHubHandler getHandler() {
        InterfaceHubHandler retVal = null;
        if(weakRef != null) {
            // the retval might be null
            retVal = weakRef.get();
        } else {
            retVal = hardRef;
        }
        return retVal;
    }
    
    public Exception getAllocateStackTrace() {
        return allocateStackTrace;
    }
    
    void throwNullHandlerReference() {
        InterfaceHub.log.e("handler was registered here (hashCode=" + hashCode + "): ", allocateStackTrace);
        throw new IllegalStateException("null handler reference, did it get garbage collected meanwhile? forgot to unregister?");
    }
}
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

import eu.kryl.android.common.async.SbHandler;

/**
 * Factory for creating new instances of IHs.
 */
public class InterfaceHubFactory {

    /**
     * Initialization routine.
     * @param uiSbHandler
     */
    public static void setUiSbHandler(SbHandler uiSbHandler) {
        InterfaceHubContractThreadSafeImpl.setUiHandler(uiSbHandler);
    }

    @NotNull
    public static InterfaceHubContract newSingleLooperIh(InterfaceHubContract.Delivery defaultDeliveryPolicy) {
        return new InterfaceHubContractSingleLooperImpl(newThreadSafeIh(defaultDeliveryPolicy));
    }

    @NotNull
    public static InterfaceHubContractThreadSafeImpl newThreadSafeIh(InterfaceHubContract.Delivery defaultDeliveryPolicy) {
        return new InterfaceHubContractThreadSafeImpl(defaultDeliveryPolicy);
    }

}

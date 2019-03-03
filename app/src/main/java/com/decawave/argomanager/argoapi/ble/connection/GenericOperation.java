/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.argoapi.ble.connection;

import com.decawave.argomanager.argoapi.ble.GattInteractionFsm;

/**
 * Argo project.
 */
interface GenericOperation {

    SequentialGattOperationQueue.Token dependsOn();

    enum Result {
        ASYNC_NOT_KNOWN,
        SYNCHRONOUS_SUCCESS,
        SYNCHRONOUS_FAIL
    }

    /**
     * @param gattInteractionFsm gatt interaction layer
     * @return result status
     */
    Result execute(GattInteractionFsm gattInteractionFsm);

}

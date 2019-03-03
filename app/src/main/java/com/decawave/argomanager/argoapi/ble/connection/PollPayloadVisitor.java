/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.argoapi.ble.connection;

/**
 * Visitor capable of visiting all poll commands.
 */
interface PollPayloadVisitor {

    void visit(FwPollCommand.UploadRefused uploadRefused);

    void visit(FwPollCommand.BufferRequest bufferRequest);

    void visit(FwPollCommand.UploadComplete uploadComplete);

    void visit(FwPollCommand.SaveFailed saveFailed);
}

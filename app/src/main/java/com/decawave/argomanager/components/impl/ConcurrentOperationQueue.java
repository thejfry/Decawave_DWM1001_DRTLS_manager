/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.components.impl;

import com.decawave.argo.api.YesNoAsync;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import rx.functions.Action0;
import rx.functions.Action1;

/**
 * Operation queue able to limit number of concurrently executing operations/actions.
 *
 * Whenever the limit is reached, the new enqueued operation {@link #operationEnqueue(Action1, Priority, String)} is
 * postponed until one of the currently executing operations finishes {@link #onOperationFinished(Token)}.
 *
 * The passed operations may be asynchronous activities - they are not considered to be finished, once executed.
 * Instead the queue needs to be explicitly notified about finished execution.
 *
 * Capable of operation prioritization as well as dynamic priority adjustment.
 */
public interface ConcurrentOperationQueue {

    enum Priority {
        LOW,
        MEDIUM,
        HIGH
    }

    /**
     * Uniquely identifies enqueue operation request.
     */
    interface Token {

    }

    /**
     * Enqueues a new operation to be executed.
     * Generates a new unique token upon each enqueue request.
     * Either executes the operation directly (if the limit is not reached yet), or waits till one of the
     * executing operations finishes.
     *
     * @param tokenAwareOperation operation to be executed, may use the passed token
     * @param priority operation priority
     * @param operationResource identifies resource which this operation uses - this is needed to ensure that there
     *                          are never two concurrently executing operations using the same resource
     * @return unique per-enqueue token
     * @see #onOperationFinished(Token)
     */
    Token operationEnqueue(@NotNull Action1<Token> tokenAwareOperation,
                           @NotNull Priority priority,
                           @Nullable String operationResource);

    /**
     * Used by the upper layers to notify this queue, that the operation has finished.
     * Using this explicit notification, an operation might be an asynchronous activity.
     *
     * @param operationToken identifies the previously enqueued, now executed operation
     * @see #operationEnqueue(Action1, Priority, String)
     */
    void onOperationFinished(Token operationToken);

    void onOperationFinished(Token operationToken, long delayBeforeSameResourceOperation);

    /**
     * Limit the number of concurrently executed operations of the given priority.
     * Does not terminate currently executing tasks - let them finish first.
     *
     * @param priority identifies priority
     * @param limit determines the limit
     * @return the old limit
     */
    @SuppressWarnings("UnusedReturnValue")
    int limitOperationExecutionByPriority(Priority priority, int limit);

    void blockProcessing(Action0 onBlockCompleted);

    void unblockProcessing();

    YesNoAsync isProcessingBlocked();

}

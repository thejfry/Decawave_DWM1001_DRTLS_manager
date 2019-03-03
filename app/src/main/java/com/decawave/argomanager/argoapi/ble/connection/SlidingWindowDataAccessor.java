/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.argoapi.ble.connection;

/**
 * Wraps binary data providing forward-sliding view/window.
 * Initialization depends on particular implementation.
 */
interface SlidingWindowDataAccessor {

    /**
     * Configures the window above the wrapped binary data.
     * @param absoluteOffset absolute offset in the data, never decreases (either same or greater)
     * @param maxWindowSize window size, once the window size has been reached, {@link #nextChunk()} returns null
     */
    void setWindow(int absoluteOffset, int maxWindowSize);

    /**
     * Configures the returned chunk size.
     * @param maxChunkSize limits the maximum returned chunk size
     */
    void setChunkSize(int maxChunkSize);

    /**
     * @return the next chunk based upon previous calls to {@link #setChunkSize(int)} and {@link #setWindow(int, int)},
     *         please note that the chunk might be smaller than configured chunkSize - if we have reached the end of the stream,
     *         null if the previously set buffer window is exhausted,
     *         empty array if there is no more data in the stream
     */
    byte[] nextChunk();

    /**
     * @return current position/offset in the data
     */
    int getCurrentPosition();

}

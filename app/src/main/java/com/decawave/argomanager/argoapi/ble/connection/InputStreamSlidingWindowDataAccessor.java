/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.argoapi.ble.connection;

import com.decawave.argomanager.Constants;
import com.google.common.base.Preconditions;

import org.apache.commons.lang3.ArrayUtils;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Argo project.
 */
class InputStreamSlidingWindowDataAccessor implements SlidingWindowDataAccessor {
    // maximal size of unconfirmed buffer - we can go that long back from current
    private static final int MAX_READ_AHEAD_BUFFER_SIZE = 8196;

    private final BufferedInputStream stream;
    // state
    private int streamPosition;
    private int resetPosition;      // reset position never decreases
    private int remainingMaxBytes;
    private int chunkSize;
    private byte[] wrkChunk;


    InputStreamSlidingWindowDataAccessor(InputStream stream) {
        // make sure we have buffered input stream
        if (!(stream instanceof BufferedInputStream)) {
            this.stream = new BufferedInputStream(stream);
        } else {
            this.stream = (BufferedInputStream) stream;
        }
        Preconditions.checkState(stream.markSupported());
        // mark the zero position
        stream.mark(MAX_READ_AHEAD_BUFFER_SIZE);
        wrkChunk = ArrayUtils.EMPTY_BYTE_ARRAY;
        resetPosition = 0;
        streamPosition = 0;
    }

    @Override
    public void setWindow(int absoluteOffset, int maxWindowSize) {
        Preconditions.checkState(absoluteOffset >= resetPosition,
                "cannot go back before previously requested offset, previouslyRequested = " + resetPosition
                + ", now requested = " + absoluteOffset);
        try {
            int diff = absoluteOffset - streamPosition;
            // rewind the buffer to proper position
            if (diff == 0) {
                // we are at the desired position
                stream.mark(MAX_READ_AHEAD_BUFFER_SIZE);
                resetPosition = absoluteOffset;
                // the streamPosition is already set properly
            } else if (diff > 0) {
                // the desired offset is after the current position
                skipAhead(diff, absoluteOffset);
            } else if (diff < 0) {
                // go back to previous mark
                stream.reset();
                // we are now at resetPosition, compute how many bytes we need to skip
                diff = absoluteOffset - resetPosition;
                if (diff > 0) {
                    // we need to skip
                    skipAhead(diff, absoluteOffset);
                } else {
                    // we are at the proper position: resetPosition == offset
                    streamPosition = resetPosition;
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        // set the remaining max bytes
        this.remainingMaxBytes = maxWindowSize;
    }

    private void skipAhead(int diff, int absolutePosition) throws IOException {
        if (Constants.DEBUG) {
            // reset position never decreases
            Preconditions.checkNotNull(resetPosition <= absolutePosition);
        }
        long skipped = stream.skip(diff);
        if (skipped != diff) {
            throw new RuntimeException("trying to skip stream behind its end");
        }
        // mark the new position
        stream.mark(MAX_READ_AHEAD_BUFFER_SIZE);
        resetPosition = absolutePosition;
        streamPosition = absolutePosition;
    }

    @Override
    public void setChunkSize(int maxChunkSize) {
        if (this.chunkSize != maxChunkSize) {
            this.wrkChunk = new byte[maxChunkSize];
            this.chunkSize = maxChunkSize;
        }
    }

    @Override
    public int getCurrentPosition() {
        return streamPosition;
    }

    @Override
    public byte[] nextChunk() {
        if (Constants.DEBUG) {
            Preconditions.checkState(chunkSize == wrkChunk.length);
        }
        // fill wrkChunk with data
        if (remainingMaxBytes == 0) {
            return null;
        }
        int size = Math.min(remainingMaxBytes, chunkSize);
        //
        try {
            int read = stream.read(wrkChunk, 0, size);
            if (read == -1) {
                // we've reached the end of the stream
                return ArrayUtils.EMPTY_BYTE_ARRAY;
            } // else:
            // adjust stream position and remaining bytes
            streamPosition += read;
            remainingMaxBytes -= read;
            // return the read data
            if (read != chunkSize) {
                // we will return a freshly allocated byte array
                return ArrayUtils.subarray(wrkChunk, 0, read);
            } else {
                // read == chunkSize, we can return the pre-allocated chunk
                return wrkChunk;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

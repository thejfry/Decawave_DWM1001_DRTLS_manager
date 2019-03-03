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

package eu.kryl.android.common.async;

import android.os.Handler;

import com.google.common.base.Preconditions;

import eu.kryl.android.common.Constants;

/**
 * Async Job is like Android's AsyncTask but a bit simpler.
 *
 * Needs static initialization {@link #setCleanupWorkerHandler(SbHandler)}
 * (similarly as ComponentLog).
 */
public abstract class AsyncJob<Input, Output> {
    // reference to worker handler on which we can wait some time
    private static volatile SbHandler cleanupWorkerHandler;

    // properties
    final private String name;
    // variable state
    private int executionCounter;

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //
    // methods to be implemented
    //

    /**
     * This is performed in a separate thread.
     *
     * Beware: do not touch your stateful representations here, instead pass them
     * through Input
     *
     * Beware: do not use the result immediately, wait to be notified
     * via {@link #onResult(Object)}.
     *
     * @param input input of the task (see {@link #start(Object)})
     * @return computed/retrieved output (further passed to {@link #onResult(Object)}
     */
    protected abstract Output jobExecute(Input input) throws Exception;

    /**
     * Performed on the caller's thread/handler.
     * Use this to propagate the result to your stateful representations.
     * 
     * @param result notifies about the AsyncJob result
     */
    protected abstract void onResult(Output result);

    /**
     * Performed on the caller's thread/handler.
     * Notifies about fault in the async job.
     * @param t thrown exception
     */
    protected void onException(Throwable t) {
        // ignore by default
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //
    // public API
    //


    public AsyncJob() {
        this.executionCounter = 0;
        this.name = null;
    }

    public AsyncJob(String name) {
        this.executionCounter = 0;
        this.name = name;
    }

    /**
     * Start execution of this asynchronous job.
     * Creates a new separate thread, notifies about the result on callers handler
     * (the callers thread must be prepared Looper-aware).
     * @param i
     */
    public synchronized final void start(final Input i) {
        Preconditions.checkNotNull(cleanupWorkerHandler, "FIXME: worker handler cannot be null! forgot to call: setCleanupWorkerHandler()?");
        // note down that we are running
        this.executionCounter++;
        // save caller 'thread'
        final Handler callingThreadHandler = new Handler();
        Thread t = new Thread() {

            @Override
            public void run() {
                // do the job on async thread
                Throwable t = null;
                Output output = null;
                try {
                    output = jobExecute(i);
                } catch (Throwable _t) {
                    t = _t;
                }
                // process the result
                final Output fOutput = output;
                final Throwable fThrowable = t;
                // notify the caller
                callingThreadHandler.post(new Runnable() {

                    @Override
                    public void run() {
                        // do this on the caller's 'thread'
                        // let us know that the job is done - so that if we ask in onResult or onException if
                        // we are executing, we got false
                        synchronized (AsyncJob.this) {
                            executionCounter--;
                        }
                        // notify about the result
                        if (fThrowable != null) {
                            onException(fThrowable);
                        } else {
                            onResult(fOutput);
                        }
                    }
                });
                // make sure we cleanup the thread properly
                final Thread th = Thread.currentThread();
                cleanupWorkerHandler.post(new Runnable() {

                    @Override
                    public void run() {
                        try {
                            th.join(500);
                        } catch (InterruptedException e) {}

                        if (th.isAlive() && Constants.DEBUG) {
                            throw new RuntimeException("FixMe: post this only when the thread is finishing..");
                        }
                    }
                });


            }
        };
        if (name != null) {
            t.setName(name);
        }
        // start the thread now!
        t.start();
    }


    public static void setCleanupWorkerHandler(SbHandler cleanupWorkerHandler) {
        AsyncJob.cleanupWorkerHandler = cleanupWorkerHandler;
    }

    public synchronized boolean isExecuting() {
        return executionCounter > 0;
    }
}
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
import android.os.Looper;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Worker thread used to run operations which requires to run on a non-UI thread.<br>
 * NOTE: Please note that this thread is not joined at the end so should be used for threads which are running during
 * the whole application lifetime!
 */
public class WorkerThread extends Thread {

    /** Counter for unique thread name */
    private static AtomicInteger mThreadNameCounter = new AtomicInteger(0);

    /** Handler of this thread */
    public volatile Handler mHandler;

    /** Used to notify the caller that the thread is up and running */
    private CountDownLatch mDoneSignal;

    /** Private constructor */
    private WorkerThread(CountDownLatch doneSignal) {
        // set name
        setName("WorkerThread_" + mThreadNameCounter.incrementAndGet());

        // set context classloader
        Thread.currentThread().setContextClassLoader(WorkerThread.class.getClassLoader());

        mDoneSignal = doneSignal;
    }

    /**
     * Run method
     */
    public void run() {
        Looper.prepare();
        mHandler = new Handler();
        mDoneSignal.countDown();
        Looper.loop();
    }

    /**
     * Stop this {@link WorkerThread}
     */
    public void stopThisWorkerThread() {
        if (mHandler != null) {
            mHandler.getLooper().quit();
        }
    }

    /**
     * Start a new {@link WorkerThread} and wait until not started
     */
    public static WorkerThread startNewWorkerThreadAndWait() {
        CountDownLatch doneSignal = new CountDownLatch(1);
        WorkerThread thread = new WorkerThread(doneSignal);
        thread.start();

        // wait to be started
        try {
            doneSignal.await();
        } catch (InterruptedException e) {
            // should never happen
        }
        return thread;
    }
}

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

package eu.kryl.android.common.async.impl;

import java.util.Iterator;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import eu.kryl.android.common.async.SbHandler;

/**
 * Handler for testing purposes.
 * Does not execute the runnables directly, instead
 * is executes them on request.
 *
 */
public class SbTestHandler implements SbHandler {
    private BlockingQueue<Runnable> runnables;
    private final boolean emulatesUiHandler;

    public SbTestHandler(boolean emulatesUiHandler) {
        this.runnables = new ArrayBlockingQueue<>(1024);
        this.emulatesUiHandler = emulatesUiHandler;
    }

    @Override
    public void post(Runnable runnable) {
        this.runnables.offer(runnable);
    }

    @Override
    public void postDelayed(Runnable runnable, long delayInMillis) {
        this.runnables.offer(runnable);
    }

    @Override
    public void postDelayed(Runnable runnable, long delayInMillis, Object token) {
        throw new UnsupportedOperationException("FIXME");
    }

    @Override
    public boolean postAtFrontOfQueue(Runnable runnable) {
        return this.runnables.offer(runnable);
    }

    public int getNumberOfTasks() {
        return runnables.size();
    }

    public void executeClearAll() {
        executeClearAll(0);
    }

    // executes all tasks with a given timeout
    private void executeClearAll(long timeout) {
        long deadline = System.currentTimeMillis() + timeout;
        Runnable r;
        while (true) {
            while ((r = runnables.poll()) != null) {
                r.run();
            }
            if (deadline < System.currentTimeMillis()) {
                break;
            }
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public boolean currentThreadHandler() {
        //noinspection RedundantIfStatement
        if (emulatesUiHandler && Thread.currentThread().getName().contains("InstrumentationTestRunner")) {
            // pretend that this is the main application thread/UI handler
            return true;
        } // else:
        return false;
    }

    @Override
    public void clear() {
        runnables.clear();
    }

    @Override
    public void removeCallbacks(Runnable runnable) {
        Iterator<Runnable> it = runnables.iterator();
        while (it.hasNext()) {
            if (runnable == it.next()) {
                it.remove();
            }
        }
    }

    @Override
    public void removeCallbacks(Object runnableToken) {
        throw new UnsupportedOperationException("FIXME");
    }

    public void executeTillFalse(AtomicBoolean flag) {
        try {
            do {
                runnables.take().run();
            } while (!flag.get());
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}

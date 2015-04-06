/***************************************************************************************
 * Attribution Notice
 *
 * This file is imported from Metrics (https://github.com/codahale/metrics subproject metrics-core).
 * Metrics is Copyright (c) 2010-2012 Coda Hale, Yammer.com
 * Metrics is Published under Apache Software License 2.0, see LICENSE in root folder.
 *
 * Thank you for the Metrics developers efforts in making their library available under an Apache license.
 * EsperTech incorporates Metrics version 0.2.2 in source code form since Metrics depends on SLF4J
 * and this dependency is not possible to introduce for Esper.
 * *************************************************************************************
 */

package com.espertech.esper.metrics.codahale_metrics.metrics.util;

import com.espertech.esper.metrics.codahale_metrics.metrics.core.Counter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * When a thread throws an Exception that was not caught, a DeathRattleExceptionHandler will
 * increment a counter signalling a thread has died and print out the name and stack trace of the
 * thread.
 * <p/>
 * This makes it easy to build alerts on unexpected Thread deaths and fine grained used quickens
 * debugging in production.
 * <p/>
 * You can also set a DeathRattleExceptionHandler as the default exception handler on all threads,
 * allowing you to get information on Threads you do not have direct control over.
 * <p/>
 * Usage is straightforward:
 * <p/>
 * <pre><code>
 * final Counter c = Metrics.newCounter(MyRunnable.class, "thread-deaths");
 * Thread.UncaughtExceptionHandler exHandler = new DeathRattleExceptionHandler(c);
 * final Thread myThread = new Thread(myRunnable, "MyRunnable");
 * myThread.setUncaughtExceptionHandler(exHandler);
 * </code></pre>
 * <p/>
 * Setting the global default exception handler should be done first, like so:
 * <p/>
 * <pre><code>
 * final Counter c = Metrics.newCounter(MyMainClass.class, "unhandled-thread-deaths");
 * Thread.UncaughtExceptionHandler ohNoIDidntKnowAboutThis = new DeathRattleExceptionHandler(c);
 * Thread.setDefaultUncaughtExceptionHandler(ohNoIDidntKnowAboutThis);
 * </code></pre>
 */
public class DeathRattleExceptionHandler implements Thread.UncaughtExceptionHandler {
    private static final Log log = LogFactory.getLog(DeathRattleExceptionHandler.class);

    private final Counter counter;

    /**
     * Creates a new {@link DeathRattleExceptionHandler} with the given {@link Counter}.
     *
     * @param counter    the {@link Counter} which will be used to record the number of uncaught
     *                   exceptions
     */
    public DeathRattleExceptionHandler(Counter counter) {
        this.counter = counter;
    }

    @Override
    public void uncaughtException(Thread t, Throwable e) {
        counter.inc();
        log.error("Uncaught exception on thread " + t + ": " + e.getMessage(), e);
    }
}

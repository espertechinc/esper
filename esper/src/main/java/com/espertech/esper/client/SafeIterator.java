/*
 ***************************************************************************************
 *  Copyright (C) 2006 EsperTech, Inc. All rights reserved.                            *
 *  http://www.espertech.com/esper                                                     *
 *  http://www.espertech.com                                                           *
 *  ---------------------------------------------------------------------------------- *
 *  The software in this package is published under the terms of the GPL license       *
 *  a copy of which has been included with this distribution in the license.txt file.  *
 ***************************************************************************************
 */
package com.espertech.esper.client;

import java.util.Iterator;

/**
 * A concurrency-safe iterator that iterates over events representing statement results (pull API)
 * in the face of concurrent event processing by further threads.
 * <p>
 * In comparison to the regular iterator, the safe iterator guarantees correct results even
 * as events are being processed by other threads. The cost is that the iterator holds
 * one or more locks that must be released via the close method. Any locks are acquired
 * at the time an instance is created.
 * <p>
 * NOTE: An application MUST explicitly close the safe iterator instance using the close method, to release locks held by the
 * iterator. The call to the close method should be done in a finally block to make sure
 * the iterator gets closed.
 * <p>
 * Multiple safe iterators may be not be used at the same time by different application threads.
 * A single application thread may hold and use multiple safe iterators however this is discouraged.
 */
public interface SafeIterator<E> extends Iterator<E> {
    /**
     * Close the safe itertor, releasing locks. This is a required call and should
     * preferably occur in a finally block.
     */
    public void close();
}

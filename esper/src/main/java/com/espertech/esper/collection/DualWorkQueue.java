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
package com.espertech.esper.collection;

import java.util.ArrayDeque;

/**
 * Work queue wherein items can be added to the front and to the back, wherein both front and back
 * have a given order, with the idea that all items of the front queue get processed before any
 * given single item of the back queue gets processed.
 */
public class DualWorkQueue<Object> {

    private ArrayDeque<Object> frontQueue;
    private ArrayDeque<Object> backQueue;

    /**
     * Ctor.
     */
    public DualWorkQueue() {
        frontQueue = new ArrayDeque<Object>();
        backQueue = new ArrayDeque<Object>();
    }

    /**
     * Items to be processed first, in the order to be processed.
     * @return front queue
     */
    public ArrayDeque<Object> getFrontQueue() {
        return frontQueue;
    }

    /**
     * Items to be processed after front-queue is empty, in the order to be processed.
     * @return back queue
     */
    public ArrayDeque<Object> getBackQueue() {
        return backQueue;
    }
}

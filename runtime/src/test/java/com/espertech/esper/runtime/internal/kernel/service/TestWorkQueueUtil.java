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
package com.espertech.esper.runtime.internal.kernel.service;

import junit.framework.TestCase;

import java.io.StringWriter;
import java.util.ArrayList;

public class TestWorkQueueUtil extends TestCase {

    public void testAdd() {
        ArrayList<WorkQueueItemPrecedenced> queue = new ArrayList<>();
        addAssert(queue, 10, "A", "A", 0);
        addAssert(queue, 11, "B", "BA", 0);
        addAssert(queue, 11, "C", "BCA", 1);
        addAssert(queue, 10, "D", "BCAD", 3);
        addAssert(queue, 11, "E", "BCEAD", 2);
        addAssert(queue, 10, "F", "BCEADF", 5);
        addAssert(queue, 9, "G", "BCEADFG", 6);
        addAssert(queue, 12, "H", "HBCEADFG", 0);
        addAssert(queue, 10, "I", "HBCEADFIG", 7);
        addAssert(queue, 11, "J", "HBCEJADFIG", 4);
        addAssert(queue, 9, "K", "HBCEJADFIGK", 10);
        addAssert(queue, 12, "L", "HLBCEJADFIGK", 1);
    }

    private void addAssert(ArrayList<WorkQueueItemPrecedenced> queue, int precedence, String id, String expected, int insertionIndex) {
        int result = WorkQueueUtil.insert(new WorkQueueItemPrecedenced(id, precedence), queue);
        assertEquals(insertionIndex, result);
        StringWriter writer = new StringWriter();
        for (WorkQueueItemPrecedenced item : queue) {
            writer.append(item.getLatchOrBean().toString());
        }
        assertEquals(expected, writer.toString());
    }
}

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
package com.espertech.esper.common.internal.statement.dispatch;

import junit.framework.TestCase;

import java.util.LinkedList;
import java.util.List;

public class TestDispatchService extends TestCase {
    private DispatchService service;

    public void setUp() {
        service = new DispatchService();
    }

    public void testAddAndDispatch() {
        // Dispatch without work to do, should complete
        service.dispatch();

        SupportDispatchable disOne = new SupportDispatchable();
        SupportDispatchable disTwo = new SupportDispatchable();
        service.addExternal(disOne);
        service.addExternal(disTwo);

        assertEquals(0, disOne.getAndResetNumExecuted());
        assertEquals(0, disTwo.getAndResetNumExecuted());

        service.dispatch();

        service.addExternal(disTwo);
        assertEquals(1, disOne.getAndResetNumExecuted());
        assertEquals(1, disTwo.getAndResetNumExecuted());

        service.dispatch();
        assertEquals(0, disOne.getAndResetNumExecuted());
        assertEquals(1, disTwo.getAndResetNumExecuted());
    }

    public void testAddDispatchTwice() {
        SupportDispatchable disOne = new SupportDispatchable();
        service.addExternal(disOne);

        service.dispatch();
        assertEquals(1, disOne.getAndResetNumExecuted());

        service.dispatch();
        assertEquals(0, disOne.getAndResetNumExecuted());
    }

    public void testAdd() {
        SupportDispatchable dispatchables[] = new SupportDispatchable[2];
        for (int i = 0; i < dispatchables.length; i++) {
            dispatchables[i] = new SupportDispatchable();
        }
        SupportDispatchable.getAndResetInstanceList();

        service.addExternal(dispatchables[0]);
        service.addExternal(dispatchables[1]);

        service.dispatch();

        List<SupportDispatchable> dispatchList = SupportDispatchable.getAndResetInstanceList();
        assertSame(dispatchables[0], dispatchList.get(0));
        assertSame(dispatchables[1], dispatchList.get(1));
    }

    public static class SupportDispatchable implements Dispatchable {
        private static List<SupportDispatchable> instanceList = new LinkedList<SupportDispatchable>();
        private int numExecuted;

        public void execute() {
            numExecuted++;
            instanceList.add(this);
        }

        public int getAndResetNumExecuted() {
            int val = numExecuted;
            numExecuted = 0;
            return val;
        }

        public static List<SupportDispatchable> getAndResetInstanceList() {
            List<SupportDispatchable> instances = instanceList;
            instanceList = new LinkedList<SupportDispatchable>();
            return instances;
        }

        public UpdateDispatchView getView() {
            throw new UnsupportedOperationException();
        }

        public void cancelled() {
            throw new UnsupportedOperationException();
        }
    }
}

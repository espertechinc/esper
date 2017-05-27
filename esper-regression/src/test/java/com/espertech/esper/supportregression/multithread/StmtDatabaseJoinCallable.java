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
package com.espertech.esper.supportregression.multithread;

import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.util.SupportMTUpdateListener;
import com.espertech.esper.util.ThreadLogUtil;
import junit.framework.AssertionFailedError;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.Callable;

public class StmtDatabaseJoinCallable implements Callable {
    private final EPServiceProvider engine;
    private final EPStatement stmt;
    private final int numRepeats;
    private final String[] MYVARCHAR_VALUES = {"A", "B", "C", "D", "E", "F", "G", "H", "I", "J"};

    public StmtDatabaseJoinCallable(EPServiceProvider engine, EPStatement stmt, int numRepeats) {
        this.engine = engine;
        this.stmt = stmt;
        this.numRepeats = numRepeats;
    }

    public Object call() throws Exception {
        try {
            // Add assertListener
            SupportMTUpdateListener assertListener = new SupportMTUpdateListener();
            ThreadLogUtil.trace("adding listeners ", assertListener);
            stmt.addListener(assertListener);

            for (int loop = 0; loop < numRepeats; loop++) {
                int intPrimitive = loop % 10 + 1;
                Object eventS0 = makeEvent(intPrimitive);

                engine.getEPRuntime().sendEvent(eventS0);

                // Should have received one that's mine, possible multiple since the statement is used by other threads
                boolean found = false;
                EventBean[] events = assertListener.getNewDataListFlattened();
                for (EventBean theEvent : events) {
                    Object s0Received = theEvent.get("s0");
                    Map s1Received = (Map) theEvent.get("s1");
                    if ((s0Received == eventS0) || (s1Received.get("myvarchar").equals(MYVARCHAR_VALUES[intPrimitive - 1]))) {
                        found = true;
                    }
                }
                if (!found) {
                }
                Assert.assertTrue(found);
                assertListener.reset();
            }
        } catch (AssertionFailedError ex) {
            log.error("Assertion error in thread " + Thread.currentThread().getId(), ex);
            return false;
        } catch (Exception ex) {
            log.error("Error in thread " + Thread.currentThread().getId(), ex);
            return false;
        }
        return true;
    }

    private SupportBean makeEvent(int intPrimitive) {
        SupportBean theEvent = new SupportBean();
        theEvent.setIntPrimitive(intPrimitive);
        return theEvent;
    }

    private static final Logger log = LoggerFactory.getLogger(StmtDatabaseJoinCallable.class);
}

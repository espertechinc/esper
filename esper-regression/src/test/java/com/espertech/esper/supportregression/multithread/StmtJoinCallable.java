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

import java.util.concurrent.Callable;

public class StmtJoinCallable implements Callable {
    private final int threadNum;
    private final EPServiceProvider engine;
    private final EPStatement stmt;
    private final int numRepeats;

    public StmtJoinCallable(int threadNum, EPServiceProvider engine, EPStatement stmt, int numRepeats) {
        this.threadNum = threadNum;
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
                long id = threadNum * 100000000 + loop;
                Object eventS0 = makeEvent("s0", id);
                Object eventS1 = makeEvent("s1", id);

                ThreadLogUtil.trace("SENDING s0 event ", id, eventS0);
                engine.getEPRuntime().sendEvent(eventS0);
                ThreadLogUtil.trace("SENDING s1 event ", id, eventS1);
                engine.getEPRuntime().sendEvent(eventS1);

                //ThreadLogUtil.info("sent", eventS0, eventS1);
                // Should have received one that's mine, possible multiple since the statement is used by other threads
                boolean found = false;
                EventBean[] events = assertListener.getNewDataListFlattened();
                for (EventBean theEvent : events) {
                    Object s0Received = theEvent.get("s0");
                    Object s1Received = theEvent.get("s1");
                    //ThreadLogUtil.info("received", event.get("s0"), event.get("s1"));
                    if ((s0Received == eventS0) && (s1Received == eventS1)) {
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

    private SupportBean makeEvent(String theString, long longPrimitive) {
        SupportBean theEvent = new SupportBean();
        theEvent.setLongPrimitive(longPrimitive);
        theEvent.setTheString(theString);
        return theEvent;
    }

    private static final Logger log = LoggerFactory.getLogger(StmtJoinCallable.class);
}

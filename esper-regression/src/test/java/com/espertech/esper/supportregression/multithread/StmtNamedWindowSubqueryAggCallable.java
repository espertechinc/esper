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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

public class StmtNamedWindowSubqueryAggCallable implements Callable<Boolean> {
    private final EPServiceProvider engine;
    private final int numRepeats;
    private final int threadNum;
    private final EPStatement targetStatement;

    public StmtNamedWindowSubqueryAggCallable(int threadNum, EPServiceProvider engine, int numRepeats, EPStatement targetStatement) {
        this.numRepeats = numRepeats;
        this.threadNum = threadNum;
        this.engine = engine;
        this.targetStatement = targetStatement;
    }

    public Boolean call() throws Exception {
        try {
            SupportMTUpdateListener listener = new SupportMTUpdateListener();
            targetStatement.addListener(listener);

            for (int loop = 0; loop < numRepeats; loop++) {
                String generalKey = "Key";
                int valueExpected = threadNum * 1000000000 + loop + 1;

                // send insert event with string-value NOT specific to thread
                sendEvent(generalKey, valueExpected);

                // send subquery trigger event
                engine.getEPRuntime().sendEvent(new SupportBean(generalKey, -1));

                // assert trigger event received
                List<EventBean[]> events = listener.getNewDataListCopy();
                boolean found = false;
                for (EventBean[] arr : events) {
                    for (EventBean item : arr) {
                        List<Integer> value = (List<Integer>) item.get("val");
                        for (Integer valueReceived : value) {
                            if (valueReceived == valueExpected) {
                                found = true;
                                break;
                            }
                        }
                        if (found) {
                            break;
                        }
                    }
                    if (found) {
                        break;
                    }
                }
                listener.reset();

                if (!found) {
                    return false;
                }

                // send delete event with string-value specific to thread
                sendEvent(generalKey, valueExpected);
            }
        } catch (Exception ex) {
            log.error("Error in thread " + Thread.currentThread().getId(), ex);
            return false;
        }
        return true;
    }

    private void sendEvent(String key, int intupd) {
        Map<String, Object> theEvent = new HashMap<String, Object>();
        theEvent.put("uekey", key);
        theEvent.put("ueint", intupd);
        engine.getEPRuntime().sendEvent(theEvent, "UpdateEvent");
    }

    private static final Logger log = LoggerFactory.getLogger(StmtNamedWindowSubqueryAggCallable.class);
}

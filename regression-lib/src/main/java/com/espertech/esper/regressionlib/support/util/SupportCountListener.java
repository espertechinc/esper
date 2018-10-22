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
package com.espertech.esper.regressionlib.support.util;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.runtime.client.EPRuntime;
import com.espertech.esper.runtime.client.EPStatement;
import com.espertech.esper.runtime.client.UpdateListener;

import java.util.concurrent.atomic.AtomicInteger;

public class SupportCountListener implements UpdateListener {
    private AtomicInteger countNew = new AtomicInteger();
    private AtomicInteger countOld = new AtomicInteger();

    public void update(EventBean[] newEvents, EventBean[] oldEvents, EPStatement statement, EPRuntime runtime) {
        if (newEvents != null) {
            countNew.addAndGet(newEvents.length);
        }
        if (oldEvents != null) {
            countOld.addAndGet(oldEvents.length);
        }
    }

    public int getCountNew() {
        return countNew.get();
    }

    public int getCountOld() {
        return countOld.get();
    }
}

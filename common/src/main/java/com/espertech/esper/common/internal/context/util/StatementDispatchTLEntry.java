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
package com.espertech.esper.common.internal.context.util;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.collection.UniformPair;

import java.util.ArrayDeque;

public class StatementDispatchTLEntry {
    private final ArrayDeque<UniformPair<EventBean[]>> results = new ArrayDeque<>();
    private boolean isDispatchWaiting;

    public ArrayDeque<UniformPair<EventBean[]>> getResults() {
        return results;
    }

    public boolean isDispatchWaiting() {
        return isDispatchWaiting;
    }

    public void setDispatchWaiting(boolean dispatchWaiting) {
        isDispatchWaiting = dispatchWaiting;
    }
}

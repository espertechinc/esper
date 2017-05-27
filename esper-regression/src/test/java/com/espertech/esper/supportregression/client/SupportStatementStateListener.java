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
package com.espertech.esper.supportregression.client;

import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.EPStatementStateListener;
import org.junit.Assert;

import java.util.ArrayList;
import java.util.List;

public class SupportStatementStateListener implements EPStatementStateListener {
    private List<EPStatement> createdEvents = new ArrayList<EPStatement>();
    private List<EPStatement> stateChangeEvents = new ArrayList<EPStatement>();

    public void onStatementCreate(EPServiceProvider serviceProvider, EPStatement statement) {
        createdEvents.add(statement);
    }

    public void onStatementStateChange(EPServiceProvider serviceProvider, EPStatement statement) {
        stateChangeEvents.add(statement);
    }

    public EPStatement assertOneGetAndResetCreatedEvents() {
        Assert.assertEquals(1, createdEvents.size());
        EPStatement item = createdEvents.get(0);
        createdEvents.clear();
        return item;
    }

    public EPStatement assertOneGetAndResetStateChangeEvents() {
        Assert.assertEquals(1, stateChangeEvents.size());
        Assert.assertEquals(0, createdEvents.size());
        EPStatement item = stateChangeEvents.get(0);
        stateChangeEvents.clear();
        return item;
    }

    public List<EPStatement> getCreatedEvents() {
        return createdEvents;
    }

    public List<EPStatement> getStateChangeEvents() {
        return stateChangeEvents;
    }
}

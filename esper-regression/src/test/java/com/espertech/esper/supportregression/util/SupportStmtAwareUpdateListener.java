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
package com.espertech.esper.supportregression.util;

import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.StatementAwareUpdateListener;
import com.espertech.esper.collection.UniformPair;
import org.junit.Assert;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class SupportStmtAwareUpdateListener implements StatementAwareUpdateListener {
    private final List<EPStatement> statementList;
    private final List<EPServiceProvider> svcProviderList;
    private final List<EventBean[]> newDataList;
    private final List<EventBean[]> oldDataList;
    private EventBean[] lastNewData;
    private EventBean[] lastOldData;
    private boolean isInvoked;

    public SupportStmtAwareUpdateListener() {
        newDataList = new LinkedList<EventBean[]>();
        oldDataList = new LinkedList<EventBean[]>();
        statementList = new ArrayList<EPStatement>();
        svcProviderList = new ArrayList<EPServiceProvider>();
    }

    public void update(EventBean[] newData, EventBean[] oldData, EPStatement statement, EPServiceProvider serviceProvider) {
        statementList.add(statement);
        svcProviderList.add(serviceProvider);

        this.oldDataList.add(oldData);
        this.newDataList.add(newData);

        this.lastNewData = newData;
        this.lastOldData = oldData;

        isInvoked = true;
    }

    public void reset() {
        statementList.clear();
        svcProviderList.clear();
        this.oldDataList.clear();
        this.newDataList.clear();
        this.lastNewData = null;
        this.lastOldData = null;
        isInvoked = false;
    }

    public EventBean[] getLastNewData() {
        return lastNewData;
    }

    public EventBean[] getAndResetLastNewData() {
        EventBean[] lastNew = lastNewData;
        reset();
        return lastNew;
    }

    public List<EPStatement> getStatementList() {
        return statementList;
    }

    public List<EPServiceProvider> getSvcProviderList() {
        return svcProviderList;
    }

    public EventBean assertOneGetNewAndReset() {
        Assert.assertTrue(isInvoked);

        Assert.assertEquals(1, newDataList.size());
        Assert.assertEquals(1, oldDataList.size());

        Assert.assertEquals(1, lastNewData.length);
        Assert.assertNull(lastOldData);

        EventBean lastNew = lastNewData[0];
        reset();
        return lastNew;
    }

    public EventBean assertOneGetOldAndReset() {
        Assert.assertTrue(isInvoked);

        Assert.assertEquals(1, newDataList.size());
        Assert.assertEquals(1, oldDataList.size());

        Assert.assertEquals(1, lastOldData.length);
        Assert.assertNull(lastNewData);

        EventBean lastNew = lastOldData[0];
        reset();
        return lastNew;
    }

    public EventBean[] getLastOldData() {
        return lastOldData;
    }

    public List<EventBean[]> getNewDataList() {
        return newDataList;
    }

    public List<EventBean[]> getOldDataList() {
        return oldDataList;
    }

    public boolean isInvoked() {
        return isInvoked;
    }

    public boolean getAndClearIsInvoked() {
        boolean invoked = isInvoked;
        isInvoked = false;
        return invoked;
    }

    public void setLastNewData(EventBean[] lastNewData) {
        this.lastNewData = lastNewData;
    }

    public void setLastOldData(EventBean[] lastOldData) {
        this.lastOldData = lastOldData;
    }

    public EventBean[] getNewDataListFlattened() {
        return flatten(newDataList);
    }

    private EventBean[] flatten(List<EventBean[]> list) {
        int count = 0;
        for (EventBean[] events : list) {
            if (events != null) {
                count += events.length;
            }
        }

        EventBean[] array = new EventBean[count];
        count = 0;
        for (EventBean[] events : list) {
            if (events != null) {
                for (int i = 0; i < events.length; i++) {
                    array[count++] = events[i];
                }
            }
        }
        return array;
    }

    public void assertUnderlyingAndReset(Object[] expectedUnderlyingNew, Object[] expectedUnderlyingOld) {
        Assert.assertEquals(1, getNewDataList().size());
        Assert.assertEquals(1, getOldDataList().size());

        EventBean[] newEvents = getLastNewData();
        EventBean[] oldEvents = getLastOldData();

        if (expectedUnderlyingNew != null) {
            Assert.assertEquals(expectedUnderlyingNew.length, newEvents.length);
            for (int i = 0; i < expectedUnderlyingNew.length; i++) {
                Assert.assertSame(expectedUnderlyingNew[i], newEvents[i].getUnderlying());
            }
        } else {
            Assert.assertNull(newEvents);
        }

        if (expectedUnderlyingOld != null) {
            Assert.assertEquals(expectedUnderlyingOld.length, oldEvents.length);
            for (int i = 0; i < expectedUnderlyingOld.length; i++) {
                Assert.assertSame(expectedUnderlyingOld[i], oldEvents[i].getUnderlying());
            }
        } else {
            Assert.assertNull(oldEvents);
        }

        reset();
    }

    public void assertFieldEqualsAndReset(String fieldName, Object[] expectedNew, Object[] expectedOld) {
        Assert.assertEquals(1, getNewDataList().size());
        Assert.assertEquals(1, getOldDataList().size());

        EventBean[] newEvents = getLastNewData();
        EventBean[] oldEvents = getLastOldData();

        if (expectedNew != null) {
            Assert.assertEquals(expectedNew.length, newEvents.length);
            for (int i = 0; i < expectedNew.length; i++) {
                Object result = newEvents[i].get(fieldName);
                Assert.assertEquals(expectedNew[i], result);
            }
        } else {
            Assert.assertNull(newEvents);
        }

        if (expectedOld != null) {
            Assert.assertEquals(expectedOld.length, oldEvents.length);
            for (int i = 0; i < expectedOld.length; i++) {
                Assert.assertEquals(expectedOld[i], oldEvents[i].get(fieldName));
            }
        } else {
            Assert.assertNull(oldEvents);
        }

        reset();
    }

    public UniformPair<EventBean[]> getDataListsFlattened() {
        return new UniformPair<EventBean[]>(flatten(newDataList), flatten(oldDataList));
    }
}

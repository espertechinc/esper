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
package com.espertech.esper.core.service;

import com.espertech.esper.client.ConfigurationEngineDefaults;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.collection.UniformPair;
import com.espertech.esper.core.thread.ThreadingServiceImpl;
import com.espertech.esper.dispatch.DispatchService;
import com.espertech.esper.dispatch.DispatchServiceImpl;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.metric.MetricReportingPath;
import com.espertech.esper.supportunit.core.SupportEPStatementSPI;
import com.espertech.esper.supportunit.event.SupportEventBeanFactory;
import com.espertech.esper.support.SupportExprEvaluatorContext;
import junit.framework.TestCase;

public class TestUpdateDispatchView extends TestCase {
    private UpdateDispatchViewBlockingWait updateDispatchView;
    private SupportUpdateListener listenerOne;
    private SupportUpdateListener listenerTwo;
    private DispatchService dispatchService;
    private StatementResultServiceImpl statementResultService;

    public void setUp() {
        MetricReportingPath.setMetricsEnabled(false);
        listenerOne = new SupportUpdateListener();
        listenerTwo = new SupportUpdateListener();

        EPStatementListenerSet listenerSet = new EPStatementListenerSet();
        listenerSet.addListener(listenerOne);
        listenerSet.addListener(listenerTwo);

        dispatchService = new DispatchServiceImpl();

        statementResultService = new StatementResultServiceImpl("name", null, null, new ThreadingServiceImpl(new ConfigurationEngineDefaults.Threading()));
        statementResultService.setUpdateListeners(listenerSet, false);
        statementResultService.setSelectClause(new Class[1], new String[1], false, new ExprEvaluator[1], new SupportExprEvaluatorContext(null));
        statementResultService.setContext(new SupportEPStatementSPI(), null, false, false, false, false, null);

        updateDispatchView = new UpdateDispatchViewBlockingWait(statementResultService, dispatchService, 1000);
    }

    public void testUpdateOnceAndDispatch() {
        EventBean[] oldData = makeEvents("old");
        EventBean[] newData = makeEvents("new");
        updateDispatchView.newResult(new UniformPair<EventBean[]>(newData, oldData));

        assertFalse(listenerOne.isInvoked() || listenerTwo.isInvoked());
        dispatchService.dispatch();
        assertTrue(listenerOne.isInvoked() && listenerTwo.isInvoked());
        assertTrue(listenerOne.getLastNewData()[0] == newData[0]);
        assertTrue(listenerTwo.getLastOldData()[0] == oldData[0]);
    }

    public void testUpdateTwiceAndDispatch() {
        EventBean[] oldDataOne = makeEvents("old1");
        EventBean[] newDataOne = makeEvents("new1");
        updateDispatchView.newResult(new UniformPair<EventBean[]>(newDataOne, oldDataOne));

        EventBean[] oldDataTwo = makeEvents("old2");
        EventBean[] newDataTwo = makeEvents("new2");
        updateDispatchView.newResult(new UniformPair<EventBean[]>(newDataTwo, oldDataTwo));

        assertFalse(listenerOne.isInvoked() || listenerTwo.isInvoked());
        dispatchService.dispatch();
        assertTrue(listenerOne.isInvoked() && listenerTwo.isInvoked());
        assertTrue(listenerOne.getLastNewData()[1] == newDataTwo[0]);
        assertTrue(listenerTwo.getLastOldData()[1] == oldDataTwo[0]);
    }

    private EventBean[] makeEvents(String text) {
        return new EventBean[]{SupportEventBeanFactory.createObject(text)};
    }
}

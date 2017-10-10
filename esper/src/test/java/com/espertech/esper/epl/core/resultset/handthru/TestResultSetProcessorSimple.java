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
package com.espertech.esper.epl.core.resultset.handthru;

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.collection.MultiKey;
import com.espertech.esper.collection.UniformPair;
import com.espertech.esper.core.service.StatementContext;
import com.espertech.esper.core.service.StatementEventTypeRefImpl;
import com.espertech.esper.core.support.SupportEventAdapterService;
import com.espertech.esper.core.support.SupportStatementContextFactory;
import com.espertech.esper.epl.core.orderby.OrderByProcessor;
import com.espertech.esper.epl.core.resultset.core.ResultSetProcessorFactory;
import com.espertech.esper.epl.core.resultset.core.ResultSetProcessorOutputConditionType;
import com.espertech.esper.epl.core.resultset.core.ResultSetProcessorUtil;
import com.espertech.esper.epl.core.select.SelectExprEventTypeRegistry;
import com.espertech.esper.epl.core.select.SelectExprProcessor;
import com.espertech.esper.epl.core.select.SelectExprProcessorForge;
import com.espertech.esper.epl.core.select.SelectExprProcessorHelper;
import com.espertech.esper.epl.core.select.eval.SelectExprStreamDesc;
import com.espertech.esper.epl.table.mgmt.TableServiceImpl;
import com.espertech.esper.supportunit.bean.SupportBean;
import com.espertech.esper.supportunit.epl.SupportSelectExprFactory;
import com.espertech.esper.supportunit.epl.SupportStreamTypeSvc1Stream;
import com.espertech.esper.supportunit.event.SupportEventBeanFactory;
import junit.framework.TestCase;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

public class TestResultSetProcessorSimple extends TestCase {
    private ResultSetProcessorSimple outputProcessorAll;
    private SelectExprProcessor selectExprProcessor;
    private OrderByProcessor orderByProcessor;

    public void setUp() throws Exception {
        SelectExprEventTypeRegistry selectExprEventTypeRegistry = new SelectExprEventTypeRegistry("abc", new StatementEventTypeRefImpl());
        StatementContext statementContext = SupportStatementContextFactory.makeContext();

        SelectExprProcessorHelper selectFactory = new SelectExprProcessorHelper(Collections.<Integer>emptyList(), SupportSelectExprFactory.makeNoAggregateSelectList(), Collections.<SelectExprStreamDesc>emptyList(), null, null, false, new SupportStreamTypeSvc1Stream(), SupportEventAdapterService.getService(), null, selectExprEventTypeRegistry, statementContext.getEngineImportService(), 1, "stmtname", null, new Configuration(), null, new TableServiceImpl(), null);
        SelectExprProcessorForge selectForge = selectFactory.getForge();
        selectExprProcessor = selectForge.getSelectExprProcessor(statementContext.getEngineImportService(), false, "abc");
        orderByProcessor = null;

        ResultSetProcessorSimpleForge forge = new ResultSetProcessorSimpleForge(selectForge.getResultEventType(), selectForge, null, true, null, null, null, false, 1);
        ResultSetProcessorFactory factory = forge.getResultSetProcessorFactory(statementContext, false);
        outputProcessorAll = (ResultSetProcessorSimple) factory.instantiate(null, null, null);
    }

    public void testUpdateAll() throws Exception {
        assertNull(ResultSetProcessorUtil.getSelectEventsNoHavingWithOrderBy(null, selectExprProcessor, orderByProcessor, (EventBean[]) null, true, false, null));

        EventBean testEvent1 = makeEvent(10, 5, 6);
        EventBean testEvent2 = makeEvent(11, 6, 7);
        EventBean[] newData = new EventBean[]{testEvent1, testEvent2};

        EventBean testEvent3 = makeEvent(20, 1, 2);
        EventBean testEvent4 = makeEvent(21, 3, 4);
        EventBean[] oldData = new EventBean[]{testEvent3, testEvent4};

        UniformPair<EventBean[]> result = outputProcessorAll.processViewResult(newData, oldData, false);
        EventBean[] newEvents = result.getFirst();
        EventBean[] oldEvents = result.getSecond();

        assertEquals(2, newEvents.length);
        assertEquals(10d, newEvents[0].get("resultOne"));
        assertEquals(30, newEvents[0].get("resultTwo"));

        assertEquals(11d, newEvents[1].get("resultOne"));
        assertEquals(42, newEvents[1].get("resultTwo"));

        assertEquals(2, oldEvents.length);
        assertEquals(20d, oldEvents[0].get("resultOne"));
        assertEquals(2, oldEvents[0].get("resultTwo"));

        assertEquals(21d, oldEvents[1].get("resultOne"));
        assertEquals(12, oldEvents[1].get("resultTwo"));
    }

    public void testProcessAll() throws Exception {
        assertNull(ResultSetProcessorUtil.getSelectJoinEventsNoHavingWithOrderBy(null, selectExprProcessor, orderByProcessor, new HashSet<MultiKey<EventBean>>(), true, false, null));

        EventBean testEvent1 = makeEvent(10, 5, 6);
        EventBean testEvent2 = makeEvent(11, 6, 7);
        Set<MultiKey<EventBean>> newEventSet = makeEventSet(testEvent1);
        newEventSet.add(new MultiKey<EventBean>(new EventBean[]{testEvent2}));

        EventBean testEvent3 = makeEvent(20, 1, 2);
        EventBean testEvent4 = makeEvent(21, 3, 4);
        Set<MultiKey<EventBean>> oldEventSet = makeEventSet(testEvent3);
        oldEventSet.add(new MultiKey<EventBean>(new EventBean[]{testEvent4}));

        UniformPair<EventBean[]> result = outputProcessorAll.processJoinResult(newEventSet, oldEventSet, false);
        EventBean[] newEvents = result.getFirst();
        EventBean[] oldEvents = result.getSecond();

        assertEquals(2, newEvents.length);
        assertEquals(10d, newEvents[0].get("resultOne"));
        assertEquals(30, newEvents[0].get("resultTwo"));

        assertEquals(11d, newEvents[1].get("resultOne"));
        assertEquals(42, newEvents[1].get("resultTwo"));

        assertEquals(2, oldEvents.length);
        assertEquals(20d, oldEvents[0].get("resultOne"));
        assertEquals(2, oldEvents[0].get("resultTwo"));

        assertEquals(21d, oldEvents[1].get("resultOne"));
        assertEquals(12, oldEvents[1].get("resultTwo"));
    }

    private Set<MultiKey<EventBean>> makeEventSet(EventBean theEvent) {
        Set<MultiKey<EventBean>> result = new LinkedHashSet<MultiKey<EventBean>>();
        result.add(new MultiKey<EventBean>(new EventBean[]{theEvent}));
        return result;
    }

    private EventBean makeEvent(double doubleBoxed, int intBoxed, int intPrimitive) {
        SupportBean bean = new SupportBean();
        bean.setDoubleBoxed(doubleBoxed);
        bean.setIntBoxed(intBoxed);
        bean.setIntPrimitive(intPrimitive);
        return SupportEventBeanFactory.createObject(bean);
    }
}

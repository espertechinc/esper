/*
 * *************************************************************************************
 *  Copyright (C) 2006-2015 EsperTech, Inc. All rights reserved.                       *
 *  http://www.espertech.com/esper                                                     *
 *  http://www.espertech.com                                                           *
 *  ---------------------------------------------------------------------------------- *
 *  The software in this package is published under the terms of the GPL license       *
 *  a copy of which has been included with this distribution in the license.txt file.  *
 * *************************************************************************************
 */

package com.espertech.esper.epl.core;

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.collection.UniformPair;
import com.espertech.esper.core.context.util.AgentInstanceContext;
import com.espertech.esper.core.service.StatementEventTypeRefImpl;
import com.espertech.esper.epl.core.eval.SelectExprStreamDesc;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.table.mgmt.TableServiceImpl;
import com.espertech.esper.support.bean.SupportBean;
import com.espertech.esper.support.epl.SupportAggregationService;
import com.espertech.esper.support.epl.SupportExprNodeFactory;
import com.espertech.esper.support.epl.SupportSelectExprFactory;
import com.espertech.esper.support.epl.SupportStreamTypeSvc1Stream;
import com.espertech.esper.core.support.SupportEventAdapterService;
import com.espertech.esper.support.event.SupportEventBeanFactory;
import com.espertech.esper.core.support.SupportStatementContextFactory;
import junit.framework.TestCase;

import java.util.Collections;

public class TestResultSetProcessorRowPerGroup extends TestCase
{
    private ResultSetProcessorRowPerGroup processor;
    private SupportAggregationService supportAggregationService;
    private AgentInstanceContext agentInstanceContext;

    public void setUp() throws Exception
    {
        agentInstanceContext = SupportStatementContextFactory.makeAgentInstanceContext();

        SelectExprEventTypeRegistry selectExprEventTypeRegistry = new SelectExprEventTypeRegistry("abc", new StatementEventTypeRefImpl());
        SelectExprProcessorHelper factory = new SelectExprProcessorHelper(Collections.<Integer>emptyList(), SupportSelectExprFactory.makeSelectListFromIdent("theString", "s0"),
        		Collections.<SelectExprStreamDesc>emptyList(), null, null, false, new SupportStreamTypeSvc1Stream(), SupportEventAdapterService.getService(), null, selectExprEventTypeRegistry, agentInstanceContext.getStatementContext().getMethodResolutionService(), null, null, new Configuration(), null, new TableServiceImpl());
        SelectExprProcessor selectProcessor = factory.getEvaluator();
        supportAggregationService = new SupportAggregationService();

        ExprEvaluator[] groupKeyNodes = new ExprEvaluator[2];
        groupKeyNodes[0] = SupportExprNodeFactory.makeIdentNode("intPrimitive", "s0").getExprEvaluator();
        groupKeyNodes[1] = SupportExprNodeFactory.makeIdentNode("intBoxed", "s0").getExprEvaluator();

        ResultSetProcessorRowPerGroupFactory prototype = new ResultSetProcessorRowPerGroupFactory(selectProcessor, null, groupKeyNodes, null, true, false, null, false, false, false, false, null, false, 1, null);
        processor = (ResultSetProcessorRowPerGroup) prototype.instantiate(null, supportAggregationService, agentInstanceContext);
    }

    public void testProcess()
    {
        EventBean[] newData = new EventBean[] {makeEvent(1, 2), makeEvent(3, 4)};
        EventBean[] oldData = new EventBean[] {makeEvent(1, 2), makeEvent(1, 10)};

        UniformPair<EventBean[]> result = processor.processViewResult(newData, oldData, false);

        assertEquals(2, supportAggregationService.getEnterList().size());
        assertEquals(2, supportAggregationService.getLeaveList().size());

        assertEquals(3, result.getFirst().length);
        assertEquals(3, result.getSecond().length);
    }

    private EventBean makeEvent(int intPrimitive, int intBoxed)
    {
        SupportBean bean = new SupportBean();
        bean.setIntPrimitive(intPrimitive);
        bean.setIntBoxed(intBoxed);
        return SupportEventBeanFactory.createObject(bean);
    }
}

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
package com.espertech.esper.epl.core.resultset;

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.collection.UniformPair;
import com.espertech.esper.core.context.util.AgentInstanceContext;
import com.espertech.esper.core.service.StatementEventTypeRefImpl;
import com.espertech.esper.core.support.SupportEngineImportServiceFactory;
import com.espertech.esper.core.support.SupportEventAdapterService;
import com.espertech.esper.core.support.SupportStatementContextFactory;
import com.espertech.esper.epl.core.select.eval.SelectExprStreamDesc;
import com.espertech.esper.epl.core.select.SelectExprEventTypeRegistry;
import com.espertech.esper.epl.core.select.SelectExprProcessor;
import com.espertech.esper.epl.core.select.SelectExprProcessorHelper;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.table.mgmt.TableServiceImpl;
import com.espertech.esper.supportunit.bean.SupportBean;
import com.espertech.esper.supportunit.epl.SupportAggregationService;
import com.espertech.esper.supportunit.epl.SupportExprNodeFactory;
import com.espertech.esper.supportunit.epl.SupportSelectExprFactory;
import com.espertech.esper.supportunit.epl.SupportStreamTypeSvc1Stream;
import com.espertech.esper.supportunit.event.SupportEventBeanFactory;
import junit.framework.TestCase;

import java.util.Collections;

public class TestResultSetProcessorRowPerGroup extends TestCase {
    private ResultSetProcessorRowPerGroup processor;
    private SupportAggregationService supportAggregationService;
    private AgentInstanceContext agentInstanceContext;

    public void setUp() throws Exception {
        agentInstanceContext = SupportStatementContextFactory.makeAgentInstanceContext();

        SelectExprEventTypeRegistry selectExprEventTypeRegistry = new SelectExprEventTypeRegistry("abc", new StatementEventTypeRefImpl());
        SelectExprProcessorHelper factory = new SelectExprProcessorHelper(Collections.<Integer>emptyList(), SupportSelectExprFactory.makeSelectListFromIdent("theString", "s0"),
                Collections.<SelectExprStreamDesc>emptyList(), null, null, false, new SupportStreamTypeSvc1Stream(), SupportEventAdapterService.getService(), null, selectExprEventTypeRegistry, agentInstanceContext.getStatementContext().getEngineImportService(), 1, "stmtname", null, new Configuration(), null, new TableServiceImpl(), null);
        SelectExprProcessor selectProcessor = factory.getForge().getSelectExprProcessor(SupportEngineImportServiceFactory.make(), false, "abc");
        supportAggregationService = new SupportAggregationService();

        ExprEvaluator[] groupKeyNodes = new ExprEvaluator[2];
        groupKeyNodes[0] = SupportExprNodeFactory.makeIdentNode("intPrimitive", "s0").getForge().getExprEvaluator();
        groupKeyNodes[1] = SupportExprNodeFactory.makeIdentNode("intBoxed", "s0").getForge().getExprEvaluator();

        ResultSetProcessorRowPerGroupFactory prototype = new ResultSetProcessorRowPerGroupFactory(factory.getForge().getResultEventType(), selectProcessor, null, groupKeyNodes, null, true, false, null, false, false, false, false, null, false, 1, null);
        processor = (ResultSetProcessorRowPerGroup) prototype.instantiate(null, supportAggregationService, agentInstanceContext);
    }

    public void testProcess() {
        EventBean[] newData = new EventBean[]{makeEvent(1, 2), makeEvent(3, 4)};
        EventBean[] oldData = new EventBean[]{makeEvent(1, 2), makeEvent(1, 10)};

        UniformPair<EventBean[]> result = processor.processViewResult(newData, oldData, false);

        assertEquals(2, supportAggregationService.getEnterList().size());
        assertEquals(2, supportAggregationService.getLeaveList().size());

        assertEquals(3, result.getFirst().length);
        assertEquals(3, result.getSecond().length);
    }

    private EventBean makeEvent(int intPrimitive, int intBoxed) {
        SupportBean bean = new SupportBean();
        bean.setIntPrimitive(intPrimitive);
        bean.setIntBoxed(intBoxed);
        return SupportEventBeanFactory.createObject(bean);
    }
}

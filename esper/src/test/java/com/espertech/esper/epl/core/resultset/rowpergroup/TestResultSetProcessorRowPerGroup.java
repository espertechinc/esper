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
package com.espertech.esper.epl.core.resultset.rowpergroup;

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.collection.UniformPair;
import com.espertech.esper.core.context.util.AgentInstanceContext;
import com.espertech.esper.core.service.StatementEventTypeRefImpl;
import com.espertech.esper.core.support.SupportEventAdapterService;
import com.espertech.esper.core.support.SupportStatementContextFactory;
import com.espertech.esper.epl.core.resultset.core.ResultSetProcessorFactory;
import com.espertech.esper.epl.core.select.SelectExprEventTypeRegistry;
import com.espertech.esper.epl.core.select.SelectExprProcessorForge;
import com.espertech.esper.epl.core.select.SelectExprProcessorHelper;
import com.espertech.esper.epl.core.select.eval.SelectExprStreamDesc;
import com.espertech.esper.epl.expression.core.ExprNode;
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

    public void setUp() throws Exception {
        AgentInstanceContext agentInstanceContext = SupportStatementContextFactory.makeAgentInstanceContext();

        SelectExprEventTypeRegistry selectExprEventTypeRegistry = new SelectExprEventTypeRegistry("abc", new StatementEventTypeRefImpl());
        SelectExprProcessorHelper selectFactory = new SelectExprProcessorHelper(Collections.<Integer>emptyList(), SupportSelectExprFactory.makeSelectListFromIdent("theString", "s0"),
                Collections.<SelectExprStreamDesc>emptyList(), null, null, false, new SupportStreamTypeSvc1Stream(), SupportEventAdapterService.getService(), null, selectExprEventTypeRegistry, agentInstanceContext.getEngineImportService(), 1, "stmtname", null, new Configuration(), null, new TableServiceImpl(), null);
        SelectExprProcessorForge selectForge = selectFactory.getForge();
        supportAggregationService = new SupportAggregationService();

        ExprNode[] groupKeyNodes = new ExprNode[2];
        groupKeyNodes[0] = SupportExprNodeFactory.makeIdentNode("intPrimitive", "s0");
        groupKeyNodes[1] = SupportExprNodeFactory.makeIdentNode("intBoxed", "s0");

        ResultSetProcessorRowPerGroupForge forge = new ResultSetProcessorRowPerGroupForge(selectForge.getResultEventType(), selectForge, groupKeyNodes, null, true, false, null, false, false, false, false, null, null, 1, null);
        ResultSetProcessorFactory factory = forge.getResultSetProcessorFactory(agentInstanceContext.getStatementContext(), false);
        processor = (ResultSetProcessorRowPerGroup) factory.instantiate(null, supportAggregationService, agentInstanceContext);
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

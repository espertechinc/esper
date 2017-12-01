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
package com.espertech.esper.view.std;

import com.espertech.esper.core.context.util.AgentInstanceContext;
import com.espertech.esper.core.support.SupportStatementContextFactory;
import com.espertech.esper.epl.expression.core.ExprNodeUtilityCore;
import com.espertech.esper.supportunit.bean.SupportMarketDataBean;
import com.espertech.esper.supportunit.epl.SupportExprNodeFactory;
import com.espertech.esper.supportunit.event.SupportEventTypeFactory;
import com.espertech.esper.view.TestViewSupport;
import com.espertech.esper.view.ViewFactory;
import com.espertech.esper.view.ViewFactoryContext;
import com.espertech.esper.view.ViewParameterException;
import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.List;

public class TestMergeViewFactory extends TestCase {
    private MergeViewFactory factory;
    private List<ViewFactory> parents;
    private ViewFactoryContext viewFactoryContext = new ViewFactoryContext(SupportStatementContextFactory.makeContext(), 1, null, null, false, -1, false);

    public void setUp() throws Exception {
        factory = new MergeViewFactory();

        parents = new ArrayList<ViewFactory>();
        GroupByViewFactory groupByView = new GroupByViewFactory();
        groupByView.setViewParameters(viewFactoryContext, TestViewSupport.toExprListMD(new Object[]{"symbol", "feed"}));
        groupByView.attach(SupportEventTypeFactory.createBeanType(SupportMarketDataBean.class), SupportStatementContextFactory.makeContext(), null, null);
        parents.add(groupByView);
    }

    public void testSetParameters() throws Exception {
        tryParameter(new Object[]{"symbol", "feed"}, new String[]{"symbol", "feed"});

        tryInvalidParameter(new Object[]{"symbol", 1.1d});
        tryInvalidParameter(new Object[]{1.1d});
        tryInvalidParameter(new Object[]{new String[]{}});
        tryInvalidParameter(new Object[]{new String[]{}, new String[]{}});
    }

    public void testCanReuse() throws Exception {
        AgentInstanceContext agentInstanceContext = SupportStatementContextFactory.makeAgentInstanceContext();
        factory.setViewParameters(viewFactoryContext, TestViewSupport.toExprListMD(new Object[]{"symbol", "feed"}));
        factory.attach(SupportEventTypeFactory.createBeanType(SupportMarketDataBean.class), SupportStatementContextFactory.makeContext(), null, parents);
        assertFalse(factory.canReuse(new FirstElementView(null), agentInstanceContext));
        assertFalse(factory.canReuse(new MergeView(SupportStatementContextFactory.makeAgentInstanceViewFactoryContext(), SupportExprNodeFactory.makeIdentNodesMD("symbol"), null, true), agentInstanceContext));
        assertTrue(factory.canReuse(new MergeView(SupportStatementContextFactory.makeAgentInstanceViewFactoryContext(), SupportExprNodeFactory.makeIdentNodesMD("symbol", "feed"), null, true), agentInstanceContext));
    }

    private void tryInvalidParameter(Object[] parameters) throws Exception {
        try {
            MergeViewFactory factory = new MergeViewFactory();
            factory.setViewParameters(viewFactoryContext, TestViewSupport.toExprListMD(parameters));
            factory.attach(SupportEventTypeFactory.createBeanType(SupportMarketDataBean.class), SupportStatementContextFactory.makeContext(), null, parents);
            fail();
        } catch (ViewParameterException ex) {
            // expected
        }
    }

    private void tryParameter(Object[] parameters, String[] fieldNames) throws Exception {
        MergeViewFactory factory = new MergeViewFactory();
        factory.setViewParameters(viewFactoryContext, TestViewSupport.toExprListMD(parameters));
        factory.attach(SupportEventTypeFactory.createBeanType(SupportMarketDataBean.class), SupportStatementContextFactory.makeContext(), null, parents);
        MergeView view = (MergeView) factory.makeView(SupportStatementContextFactory.makeAgentInstanceViewFactoryContext());
        assertEquals(fieldNames[0], ExprNodeUtilityCore.toExpressionStringMinPrecedenceSafe(view.getGroupFieldNames()[0]));
        if (fieldNames.length > 0) {
            assertEquals(fieldNames[1], ExprNodeUtilityCore.toExpressionStringMinPrecedenceSafe(view.getGroupFieldNames()[1]));
        }
    }
}

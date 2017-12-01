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
package com.espertech.esper.view.ext;

import com.espertech.esper.client.EventType;
import com.espertech.esper.core.context.util.AgentInstanceContext;
import com.espertech.esper.core.service.StatementContext;
import com.espertech.esper.core.support.SupportStatementContextFactory;
import com.espertech.esper.epl.expression.core.ExprNodeUtilityCore;
import com.espertech.esper.supportunit.bean.SupportMarketDataBean;
import com.espertech.esper.supportunit.event.SupportEventTypeFactory;
import com.espertech.esper.view.TestViewSupport;
import com.espertech.esper.view.ViewParameterException;
import com.espertech.esper.view.std.FirstElementView;
import junit.framework.TestCase;

public class TestSortWindowViewFactory extends TestCase {
    private SortWindowViewFactory factory;

    public void setUp() {
        factory = new SortWindowViewFactory();
    }

    public void testSetParameters() throws Exception {
        tryParameter(new Object[]{100, "price", "volume"},
                new String[]{"price", "volume"}, 100);

        tryInvalidParameter(new Object[]{"price", "symbol", "volume"});
        tryInvalidParameter(new Object[]{});
        tryInvalidParameter(new Object[]{100, "price", 100});
        tryInvalidParameter(new Object[]{100, 100});
        tryInvalidParameter(new Object[]{100, "price", true});
    }

    public void testAttaches() throws Exception {
        // Should attach to anything as long as the fields exists
        EventType parentType = SupportEventTypeFactory.createBeanType(SupportMarketDataBean.class);

        factory.setViewParameters(null, TestViewSupport.toExprListMD(new Object[]{100, "price"}));
        factory.attach(parentType, SupportStatementContextFactory.makeContext(), null, null);

        try {
            factory.setViewParameters(null, TestViewSupport.toExprListMD(new Object[]{true, "price"}));
            factory.attach(parentType, SupportStatementContextFactory.makeContext(), null, null);
            fail();
        } catch (ViewParameterException ex) {
            // expected;
        }
    }

    public void testCanReuse() throws Exception {
        StatementContext context = SupportStatementContextFactory.makeContext();

        AgentInstanceContext agentInstanceContext = SupportStatementContextFactory.makeAgentInstanceContext();
        factory.setViewParameters(null, TestViewSupport.toExprListMD(new Object[]{100, "price"}));
        factory.attach(SupportEventTypeFactory.createBeanType(SupportMarketDataBean.class), SupportStatementContextFactory.makeContext(), null, null);
        assertFalse(factory.canReuse(new FirstElementView(null), agentInstanceContext));
        assertTrue(factory.canReuse(new SortWindowView(factory, 100, null, null), agentInstanceContext));
        assertFalse(factory.canReuse(new SortWindowView(factory, 99, null, null), agentInstanceContext));

        factory.setViewParameters(null, TestViewSupport.toExprListMD(new Object[]{100, "price", "volume"}));
        factory.attach(SupportEventTypeFactory.createBeanType(SupportMarketDataBean.class), SupportStatementContextFactory.makeContext(), null, null);
        assertTrue(factory.canReuse(new SortWindowView(factory, 100, null, null), agentInstanceContext));
    }

    private void tryInvalidParameter(Object[] parameters) throws Exception {
        try {
            factory.setViewParameters(null, TestViewSupport.toExprListMD(parameters));
            factory.attach(SupportEventTypeFactory.createBeanType(SupportMarketDataBean.class), SupportStatementContextFactory.makeContext(), null, null);
            fail();
        } catch (ViewParameterException ex) {
            // expected
        }
    }

    private void tryParameter(Object[] parameters, String[] fieldNames, int size) throws Exception {
        factory.setViewParameters(null, TestViewSupport.toExprListMD(parameters));
        factory.attach(SupportEventTypeFactory.createBeanType(SupportMarketDataBean.class), SupportStatementContextFactory.makeContext(), null, null);
        SortWindowView view = (SortWindowView) factory.makeView(SupportStatementContextFactory.makeAgentInstanceViewFactoryContext());
        assertEquals(size, view.getSortWindowSize());
        assertEquals(fieldNames[0], ExprNodeUtilityCore.toExpressionStringMinPrecedenceSafe(view.getSortCriteriaExpressions()[0]));
        if (fieldNames.length > 0) {
            assertEquals(fieldNames[1], ExprNodeUtilityCore.toExpressionStringMinPrecedenceSafe(view.getSortCriteriaExpressions()[1]));
        }
    }
}

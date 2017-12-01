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
package com.espertech.esper.view.window;

import com.espertech.esper.client.EventType;
import com.espertech.esper.core.context.util.AgentInstanceContext;
import com.espertech.esper.core.support.SupportStatementContextFactory;
import com.espertech.esper.epl.expression.core.ExprNodeUtilityCore;
import com.espertech.esper.epl.expression.time.ExprTimePeriodEvalDeltaConstGivenDelta;
import com.espertech.esper.supportunit.bean.SupportBean;
import com.espertech.esper.supportunit.epl.SupportExprNodeFactory;
import com.espertech.esper.supportunit.event.SupportEventTypeFactory;
import com.espertech.esper.view.TestViewSupport;
import com.espertech.esper.view.ViewParameterException;
import com.espertech.esper.view.std.FirstElementView;
import junit.framework.TestCase;

public class TestExternallyTimedWindowViewFactory extends TestCase {
    private ExternallyTimedWindowViewFactory factory;

    public void setUp() {
        factory = new ExternallyTimedWindowViewFactory();
    }

    public void testSetParameters() throws Exception {
        tryParameter(new Object[]{"longPrimitive", 2d}, "longPrimitive", 2000);
        tryParameter(new Object[]{"longPrimitive", 10L}, "longPrimitive", 10000);
        tryParameter(new Object[]{"longPrimitive", 11}, "longPrimitive", 11000);
        tryParameter(new Object[]{"longPrimitive", 2.2}, "longPrimitive", 2200);

        tryInvalidParameter(new Object[]{"a"});
    }

    public void testCanReuse() throws Exception {
        EventType parentType = SupportEventTypeFactory.createBeanType(SupportBean.class);
        AgentInstanceContext agentInstanceContext = SupportStatementContextFactory.makeAgentInstanceContext();

        factory.setViewParameters(SupportStatementContextFactory.makeViewContext(), TestViewSupport.toExprListBean(new Object[]{"longBoxed", 1000}));
        factory.attach(parentType, SupportStatementContextFactory.makeContext(), null, null);
        assertFalse(factory.canReuse(new FirstElementView(null), agentInstanceContext));
        assertFalse(factory.canReuse(new ExternallyTimedWindowView(factory, SupportExprNodeFactory.makeIdentNodeBean("longPrimitive"), null, new ExprTimePeriodEvalDeltaConstGivenDelta(1000), null, SupportStatementContextFactory.makeAgentInstanceViewFactoryContext()), agentInstanceContext));
        assertFalse(factory.canReuse(new ExternallyTimedWindowView(factory, SupportExprNodeFactory.makeIdentNodeBean("longBoxed"), null, new ExprTimePeriodEvalDeltaConstGivenDelta(999), null, SupportStatementContextFactory.makeAgentInstanceViewFactoryContext()), agentInstanceContext));
        assertTrue(factory.canReuse(new ExternallyTimedWindowView(factory, SupportExprNodeFactory.makeIdentNodeBean("longBoxed"), null, new ExprTimePeriodEvalDeltaConstGivenDelta(1000000), null, SupportStatementContextFactory.makeAgentInstanceViewFactoryContext()), agentInstanceContext));
    }

    public void testInvalid() throws Exception {
        EventType parentType = SupportEventTypeFactory.createBeanType(SupportBean.class);

        try {
            factory.setViewParameters(null, TestViewSupport.toExprListBean(new Object[]{50, 20}));
            factory.attach(parentType, SupportStatementContextFactory.makeContext(), null, null);
            fail();
        } catch (ViewParameterException ex) {
            // expected
        }

        try {
            factory.setViewParameters(null, TestViewSupport.toExprListBean(new Object[]{"theString", 20}));
            factory.attach(parentType, SupportStatementContextFactory.makeContext(), null, null);
            fail();
        } catch (ViewParameterException ex) {
            // expected
        }

        factory.setViewParameters(null, TestViewSupport.toExprListBean(new Object[]{"longPrimitive", 20}));
        factory.attach(parentType, SupportStatementContextFactory.makeContext(), null, null);

        assertSame(parentType, factory.getEventType());
    }

    private void tryInvalidParameter(Object[] param) throws Exception {
        try {
            ExternallyTimedWindowViewFactory factory = new ExternallyTimedWindowViewFactory();
            factory.setViewParameters(null, TestViewSupport.toExprListBean(new Object[]{param}));
            factory.attach(SupportEventTypeFactory.createBeanType(SupportBean.class), SupportStatementContextFactory.makeContext(), null, null);
            fail();
        } catch (ViewParameterException ex) {
            // expected
        }
    }

    private void tryParameter(Object[] parameters, String fieldName, long msec) throws Exception {
        ExternallyTimedWindowViewFactory factory = new ExternallyTimedWindowViewFactory();
        factory.setViewParameters(SupportStatementContextFactory.makeViewContext(), TestViewSupport.toExprListBean(parameters));
        factory.attach(SupportEventTypeFactory.createBeanType(SupportBean.class), SupportStatementContextFactory.makeContext(), null, null);
        ExternallyTimedWindowView view = (ExternallyTimedWindowView) factory.makeView(SupportStatementContextFactory.makeAgentInstanceViewFactoryContext());
        assertEquals(fieldName, ExprNodeUtilityCore.toExpressionStringMinPrecedenceSafe(view.getTimestampExpression()));
        assertTrue(new ExprTimePeriodEvalDeltaConstGivenDelta(msec).equalsTimePeriod(view.getTimeDeltaComputation()));
    }
}

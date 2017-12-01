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
package com.espertech.esper.view.stat;

import com.espertech.esper.client.EventType;
import com.espertech.esper.core.context.util.AgentInstanceContext;
import com.espertech.esper.core.support.SupportStatementContextFactory;
import com.espertech.esper.epl.expression.core.ExprNodeUtilityCore;
import com.espertech.esper.supportunit.bean.SupportMarketDataBean;
import com.espertech.esper.supportunit.epl.SupportExprNodeFactory;
import com.espertech.esper.supportunit.event.SupportEventTypeFactory;
import com.espertech.esper.view.TestViewSupport;
import com.espertech.esper.view.ViewFactoryContext;
import com.espertech.esper.view.ViewFieldEnum;
import com.espertech.esper.view.ViewParameterException;
import com.espertech.esper.view.std.FirstElementView;
import junit.framework.TestCase;

public class TestCorrelationViewFactory extends TestCase {
    private CorrelationViewFactory factory;
    private ViewFactoryContext viewFactoryContext = new ViewFactoryContext(null, 1, null, null, false, -1, false);

    public void setUp() {
        factory = new CorrelationViewFactory();
    }

    public void testSetParameters() throws Exception {
        tryParameter(new Object[]{"price", "volume"}, "price", "volume");

        tryInvalidParameter(new Object[]{"symbol", 1.1d});
        tryInvalidParameter(new Object[]{1.1d, "symbol"});
        tryInvalidParameter(new Object[]{1.1d});
        tryInvalidParameter(new Object[]{"symbol", "symbol", "symbol"});
        tryInvalidParameter(new Object[]{new String[]{"symbol", "feed"}});
    }

    public void testCanReuse() throws Exception {
        AgentInstanceContext agentInstanceContext = SupportStatementContextFactory.makeAgentInstanceContext();
        factory.setViewParameters(new ViewFactoryContext(null, 1, null, null, false, -1, false), TestViewSupport.toExprListMD(new Object[]{"price", "volume"}));
        factory.attach(SupportEventTypeFactory.createBeanType(SupportMarketDataBean.class), SupportStatementContextFactory.makeContext(), null, null);
        assertFalse(factory.canReuse(new FirstElementView(null), agentInstanceContext));
        EventType type = CorrelationView.createEventType(SupportStatementContextFactory.makeContext(), null, 1);
        assertFalse(factory.canReuse(new CorrelationView(null, SupportStatementContextFactory.makeAgentInstanceContext(), SupportExprNodeFactory.makeIdentNodeMD("volume"), null, SupportExprNodeFactory.makeIdentNodeMD("price"), null, type, null), agentInstanceContext));
        assertFalse(factory.canReuse(new CorrelationView(null, SupportStatementContextFactory.makeAgentInstanceContext(), SupportExprNodeFactory.makeIdentNodeMD("feed"), null, SupportExprNodeFactory.makeIdentNodeMD("volume"), null, type, null), agentInstanceContext));
        assertTrue(factory.canReuse(new CorrelationView(null, SupportStatementContextFactory.makeAgentInstanceContext(), SupportExprNodeFactory.makeIdentNodeMD("price"), null, SupportExprNodeFactory.makeIdentNodeMD("volume"), null, type, null), agentInstanceContext));
    }

    public void testAttaches() throws Exception {
        // Should attach to anything as long as the fields exists
        EventType parentType = SupportEventTypeFactory.createBeanType(SupportMarketDataBean.class);

        factory.setViewParameters(viewFactoryContext, TestViewSupport.toExprListMD(new Object[]{"price", "volume"}));
        factory.attach(parentType, SupportStatementContextFactory.makeContext(), null, null);
        assertEquals(Double.class, factory.getEventType().getPropertyType(ViewFieldEnum.CORRELATION__CORRELATION.getName()));

        try {
            factory.setViewParameters(viewFactoryContext, TestViewSupport.toExprListMD(new Object[]{"symbol", "volume"}));
            factory.attach(parentType, SupportStatementContextFactory.makeContext(), null, null);
            fail();
        } catch (ViewParameterException ex) {
            // expected;
        }
    }

    private void tryInvalidParameter(Object[] parameters) throws Exception {
        try {
            factory.setViewParameters(viewFactoryContext, TestViewSupport.toExprListMD(parameters));
            factory.attach(SupportEventTypeFactory.createBeanType(SupportMarketDataBean.class), SupportStatementContextFactory.makeContext(), null, null);
            fail();
        } catch (ViewParameterException ex) {
            // expected
        }
    }

    private void tryParameter(Object[] parameters, String fieldNameX, String fieldNameY) throws Exception {
        factory.setViewParameters(viewFactoryContext, TestViewSupport.toExprListMD(parameters));
        factory.attach(SupportEventTypeFactory.createBeanType(SupportMarketDataBean.class), SupportStatementContextFactory.makeContext(), null, null);
        CorrelationView view = (CorrelationView) factory.makeView(SupportStatementContextFactory.makeAgentInstanceViewFactoryContext());
        assertEquals(fieldNameX, ExprNodeUtilityCore.toExpressionStringMinPrecedenceSafe(view.getExpressionX()));
        assertEquals(fieldNameY, ExprNodeUtilityCore.toExpressionStringMinPrecedenceSafe(view.getExpressionY()));
    }
}

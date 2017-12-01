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

public class TestWeightedAverageViewFactory extends TestCase {
    private WeightedAverageViewFactory factory;
    private ViewFactoryContext viewFactoryContext = new ViewFactoryContext(null, 1, null, null, false, -1, false);

    public void setUp() {
        factory = new WeightedAverageViewFactory();
    }

    public void testSetParameters() throws Exception {
        tryParameter(new Object[]{"price", "volume"}, "price", "volume");

        tryInvalidParameter(new Object[]{"symbol", 1.1d});
        tryInvalidParameter(new Object[]{1.1d, "feed"});
        tryInvalidParameter(new Object[]{1.1d});
        tryInvalidParameter(new Object[]{"feed", "symbol", "feed"});
        tryInvalidParameter(new Object[]{new String[]{"volume", "price"}});
    }

    public void testAttaches() throws Exception {
        // Should attach to anything as long as the fields exists
        EventType parentType = SupportEventTypeFactory.createBeanType(SupportMarketDataBean.class);

        factory.setViewParameters(viewFactoryContext, TestViewSupport.toExprListMD(new Object[]{"price", "volume"}));
        factory.attach(parentType, SupportStatementContextFactory.makeContext(), null, null);
        assertEquals(Double.class, factory.getEventType().getPropertyType(ViewFieldEnum.WEIGHTED_AVERAGE__AVERAGE.getName()));

        try {
            factory.setViewParameters(viewFactoryContext, TestViewSupport.toExprListMD(new Object[]{"symbol", "feed"}));
            factory.attach(parentType, SupportStatementContextFactory.makeContext(), null, null);
            fail();
        } catch (ViewParameterException ex) {
            // expected;
        }
    }

    public void testCanReuse() throws Exception {
        AgentInstanceContext agentInstanceContext = SupportStatementContextFactory.makeAgentInstanceContext();
        factory.setViewParameters(viewFactoryContext, TestViewSupport.toExprListMD(new Object[]{"price", "volume"}));
        factory.attach(SupportEventTypeFactory.createBeanType(SupportMarketDataBean.class), SupportStatementContextFactory.makeContext(), null, null);
        assertFalse(factory.canReuse(new FirstElementView(null), agentInstanceContext));
        EventType type = WeightedAverageView.createEventType(SupportStatementContextFactory.makeContext(), null, 1);
        WeightedAverageViewFactory factoryTwo = new WeightedAverageViewFactory();
        factoryTwo.setFieldNameX(SupportExprNodeFactory.makeIdentNodeMD("price"));
        factoryTwo.setEventType(type);
        factoryTwo.setFieldNameWeight(SupportExprNodeFactory.makeIdentNodeMD("price"));
        assertFalse(factory.canReuse(new WeightedAverageView(factoryTwo, SupportStatementContextFactory.makeAgentInstanceViewFactoryContext()), agentInstanceContext));
        factoryTwo.setFieldNameWeight(SupportExprNodeFactory.makeIdentNodeMD("volume"));
        assertTrue(factory.canReuse(new WeightedAverageView(factoryTwo, SupportStatementContextFactory.makeAgentInstanceViewFactoryContext()), agentInstanceContext));
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

    private void tryParameter(Object[] parameters, String fieldNameX, String fieldNameW) throws Exception {
        factory.setViewParameters(viewFactoryContext, TestViewSupport.toExprListMD(parameters));
        factory.attach(SupportEventTypeFactory.createBeanType(SupportMarketDataBean.class), SupportStatementContextFactory.makeContext(), null, null);
        WeightedAverageView view = (WeightedAverageView) factory.makeView(SupportStatementContextFactory.makeAgentInstanceViewFactoryContext());
        assertEquals(fieldNameX, ExprNodeUtilityCore.toExpressionStringMinPrecedenceSafe(view.getFieldNameX()));
        assertEquals(fieldNameW, ExprNodeUtilityCore.toExpressionStringMinPrecedenceSafe(view.getFieldNameWeight()));
    }
}

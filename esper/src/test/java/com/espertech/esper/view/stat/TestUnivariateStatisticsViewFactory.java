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

public class TestUnivariateStatisticsViewFactory extends TestCase {
    private UnivariateStatisticsViewFactory factory;
    private ViewFactoryContext viewFactoryContext = new ViewFactoryContext(null, 1, null, null, false, -1, false);

    public void setUp() {
        factory = new UnivariateStatisticsViewFactory();
    }

    public void testSetParameters() throws Exception {
        tryParameter(new Object[]{"price"}, "price");

        tryInvalidParameter(new Object[]{});
    }

    public void testCanReuse() throws Exception {
        AgentInstanceContext agentInstanceContext = SupportStatementContextFactory.makeAgentInstanceContext();
        factory.setViewParameters(viewFactoryContext, TestViewSupport.toExprListMD(new Object[]{"price"}));
        factory.attach(SupportEventTypeFactory.createBeanType(SupportMarketDataBean.class), SupportStatementContextFactory.makeContext(), null, null);
        assertFalse(factory.canReuse(new FirstElementView(null), agentInstanceContext));
        EventType type = UnivariateStatisticsView.createEventType(SupportStatementContextFactory.makeContext(), null, 1);
        UnivariateStatisticsViewFactory factoryOne = new UnivariateStatisticsViewFactory();
        factoryOne.setEventType(type);
        factoryOne.setFieldExpression(SupportExprNodeFactory.makeIdentNodeMD("volume"));
        UnivariateStatisticsViewFactory factoryTwo = new UnivariateStatisticsViewFactory();
        factoryTwo.setEventType(type);
        factoryTwo.setFieldExpression(SupportExprNodeFactory.makeIdentNodeMD("price"));
        assertFalse(factory.canReuse(new UnivariateStatisticsView(factoryOne, SupportStatementContextFactory.makeAgentInstanceViewFactoryContext()), agentInstanceContext));
        assertTrue(factory.canReuse(new UnivariateStatisticsView(factoryTwo, SupportStatementContextFactory.makeAgentInstanceViewFactoryContext()), agentInstanceContext));
    }

    public void testAttaches() throws Exception {
        // Should attach to anything as long as the fields exists
        EventType parentType = SupportEventTypeFactory.createBeanType(SupportMarketDataBean.class);

        factory.setViewParameters(viewFactoryContext, TestViewSupport.toExprListMD(new Object[]{"price"}));
        factory.attach(parentType, SupportStatementContextFactory.makeContext(), null, null);
        assertEquals(Double.class, factory.getEventType().getPropertyType(ViewFieldEnum.UNIVARIATE_STATISTICS__AVERAGE.getName()));

        try {
            factory.setViewParameters(viewFactoryContext, TestViewSupport.toExprListMD(new Object[]{"symbol"}));
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

    private void tryParameter(Object[] parameters, String fieldName) throws Exception {
        factory.setViewParameters(viewFactoryContext, TestViewSupport.toExprListMD(parameters));
        factory.attach(SupportEventTypeFactory.createBeanType(SupportMarketDataBean.class), SupportStatementContextFactory.makeContext(), null, null);
        UnivariateStatisticsView view = (UnivariateStatisticsView) factory.makeView(SupportStatementContextFactory.makeAgentInstanceViewFactoryContext());
        assertEquals(fieldName, ExprNodeUtilityCore.toExpressionStringMinPrecedenceSafe(view.getFieldExpression()));
    }
}

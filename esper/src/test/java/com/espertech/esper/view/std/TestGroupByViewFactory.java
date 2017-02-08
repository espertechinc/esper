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

import com.espertech.esper.client.EventType;
import com.espertech.esper.core.context.util.AgentInstanceContext;
import com.espertech.esper.epl.expression.core.ExprNodeUtility;
import com.espertech.esper.supportunit.bean.SupportBean;
import com.espertech.esper.supportunit.epl.SupportExprNodeFactory;
import com.espertech.esper.supportunit.event.SupportEventTypeFactory;
import com.espertech.esper.core.support.SupportStatementContextFactory;
import com.espertech.esper.view.TestViewSupport;
import com.espertech.esper.view.ViewFactoryContext;
import com.espertech.esper.view.ViewParameterException;
import junit.framework.TestCase;

public class TestGroupByViewFactory extends TestCase
{
    private GroupByViewFactory factory;
    private ViewFactoryContext viewFactoryContext = new ViewFactoryContext(SupportStatementContextFactory.makeContext(), 1, null, null, false, -1, false);

    public void setUp()
    {
        factory = new GroupByViewFactory();
    }

    public void testSetParameters() throws Exception
    {
        tryParameter(new Object[] {"doublePrimitive"}, new String[] {"doublePrimitive"});
        tryParameter(new Object[] {"doublePrimitive", "longPrimitive"}, new String[] {"doublePrimitive", "longPrimitive"});

        tryInvalidParameter(new Object[] {"theString", 1.1d});
        tryInvalidParameter(new Object[] {1.1d});
        tryInvalidParameter(new Object[] {new String[] {}});
        tryInvalidParameter(new Object[] {new String[] {}, new String[] {}});
    }

    public void testCanReuse() throws Exception
    {
        AgentInstanceContext agentInstanceContext = SupportStatementContextFactory.makeAgentInstanceContext();
        factory.setViewParameters(viewFactoryContext, TestViewSupport.toExprListBean(new Object[] {"theString", "longPrimitive"}));
        factory.attach(SupportEventTypeFactory.createBeanType(SupportBean.class), SupportStatementContextFactory.makeContext(), null, null);
        assertFalse(factory.canReuse(new FirstElementView(null), agentInstanceContext));
        assertFalse(factory.canReuse(new GroupByViewImpl(SupportStatementContextFactory.makeAgentInstanceViewFactoryContext(), SupportExprNodeFactory.makeIdentNodesBean("theString"), null), agentInstanceContext));
        assertTrue(factory.canReuse(new GroupByViewImpl(SupportStatementContextFactory.makeAgentInstanceViewFactoryContext(), SupportExprNodeFactory.makeIdentNodesBean("theString", "longPrimitive"), null), agentInstanceContext));

        factory.setViewParameters(viewFactoryContext, TestViewSupport.toExprListBean(new Object[] {SupportExprNodeFactory.makeIdentNodesBean("theString", "longPrimitive")}));
        assertFalse(factory.canReuse(new GroupByViewImpl(SupportStatementContextFactory.makeAgentInstanceViewFactoryContext(), SupportExprNodeFactory.makeIdentNodesBean("theString"), null), agentInstanceContext));
        assertTrue(factory.canReuse(new GroupByViewImpl(SupportStatementContextFactory.makeAgentInstanceViewFactoryContext(), SupportExprNodeFactory.makeIdentNodesBean("theString", "longPrimitive"), null), agentInstanceContext));
    }

    public void testAttaches() throws Exception
    {
        // Should attach to anything as long as the fields exists
        EventType parentType = SupportEventTypeFactory.createBeanType(SupportBean.class);

        factory.setViewParameters(viewFactoryContext, TestViewSupport.toExprListBean(new Object[] {"intBoxed"}));
        factory.attach(parentType, SupportStatementContextFactory.makeContext(), null, null);
    }

    private void tryInvalidParameter(Object[] parameters) throws Exception
    {
        try
        {
            GroupByViewFactory factory = new GroupByViewFactory();
            factory.setViewParameters(viewFactoryContext, TestViewSupport.toExprListBean(parameters));
            factory.attach(SupportEventTypeFactory.createBeanType(SupportBean.class), SupportStatementContextFactory.makeContext(), null, null);
            fail();
        }
        catch (ViewParameterException ex)
        {
            // expected
        }
    }

    private void tryParameter(Object[] parameters, String[] fieldNames) throws Exception
    {
        GroupByViewFactory factory = new GroupByViewFactory();
        factory.setViewParameters(viewFactoryContext, TestViewSupport.toExprListBean(parameters));
        factory.attach(SupportEventTypeFactory.createBeanType(SupportBean.class), SupportStatementContextFactory.makeContext(), null, null);
        GroupByView view = (GroupByView) factory.makeView(SupportStatementContextFactory.makeAgentInstanceViewFactoryContext());
        assertEquals(fieldNames[0], ExprNodeUtility.toExpressionStringMinPrecedenceSafe(view.getCriteriaExpressions()[0]));
    }
}

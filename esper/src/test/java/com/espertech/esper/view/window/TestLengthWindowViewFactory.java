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

import com.espertech.esper.core.context.util.AgentInstanceContext;
import com.espertech.esper.core.support.SupportStatementContextFactory;
import com.espertech.esper.view.TestViewSupport;
import com.espertech.esper.view.ViewParameterException;
import com.espertech.esper.view.std.FirstElementView;
import junit.framework.TestCase;

public class TestLengthWindowViewFactory extends TestCase {
    private LengthWindowViewFactory factory;

    public void setUp() {
        factory = new LengthWindowViewFactory();
    }

    public void testSetParameters() throws Exception {
        tryParameter(new Object[]{10}, 10);

        tryInvalidParameter("theString");
        tryInvalidParameter(true);
        tryInvalidParameter(1.1d);
        tryInvalidParameter(0);
    }

    public void testCanReuse() throws Exception {
        AgentInstanceContext agentInstanceContext = SupportStatementContextFactory.makeAgentInstanceContext();
        factory.setViewParameters(SupportStatementContextFactory.makeViewContext(), TestViewSupport.toExprListBean(new Object[]{1000}));
        assertFalse(factory.canReuse(new FirstElementView(null), agentInstanceContext));
        assertFalse(factory.canReuse(new LengthWindowView(SupportStatementContextFactory.makeAgentInstanceViewFactoryContext(), factory, 1, null), agentInstanceContext));
        assertTrue(factory.canReuse(new LengthWindowView(SupportStatementContextFactory.makeAgentInstanceViewFactoryContext(), factory, 1000, null), agentInstanceContext));
    }

    private void tryInvalidParameter(Object param) throws Exception {
        try {

            factory.setViewParameters(SupportStatementContextFactory.makeViewContext(), TestViewSupport.toExprListBean(new Object[]{param}));
            fail();
        } catch (ViewParameterException ex) {
            // expected
        }
    }

    private void tryParameter(Object[] param, int size) throws Exception {
        LengthWindowViewFactory factory = new LengthWindowViewFactory();
        factory.setViewParameters(SupportStatementContextFactory.makeViewContext(), TestViewSupport.toExprListBean(param));
        LengthWindowView view = (LengthWindowView) factory.makeView(SupportStatementContextFactory.makeAgentInstanceViewFactoryContext());
        assertEquals(size, view.getSize());
    }
}

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
import com.espertech.esper.core.support.SupportStatementContextFactory;
import com.espertech.esper.view.TestViewSupport;
import junit.framework.TestCase;

public class TestSizeViewFactory extends TestCase {
    private SizeViewFactory factory;

    public void setUp() {
        factory = new SizeViewFactory();
    }

    public void testSetParameters() throws Exception {
        tryParameter(new Object[]{});
    }

    public void testCanReuse() throws Exception {
        AgentInstanceContext agentInstanceContext = SupportStatementContextFactory.makeAgentInstanceContext();
        assertFalse(factory.canReuse(new LastElementView(null), agentInstanceContext));
        EventType type = SizeView.createEventType(SupportStatementContextFactory.makeContext(), null, 1);
        assertTrue(factory.canReuse(new SizeView(SupportStatementContextFactory.makeAgentInstanceContext(), type, null), agentInstanceContext));
    }

    private void tryParameter(Object[] param) throws Exception {
        SizeViewFactory factory = new SizeViewFactory();
        factory.setViewParameters(SupportStatementContextFactory.makeViewContext(), TestViewSupport.toExprListBean(param));
        assertTrue(factory.makeView(SupportStatementContextFactory.makeAgentInstanceViewFactoryContext()) instanceof SizeView);
    }
}

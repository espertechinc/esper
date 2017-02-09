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
import com.espertech.esper.epl.expression.time.ExprTimePeriodEvalDeltaConstGivenDelta;
import com.espertech.esper.view.TestViewSupport;
import com.espertech.esper.view.ViewParameterException;
import com.espertech.esper.view.std.FirstElementView;
import junit.framework.TestCase;

public class TestTimeWindowViewFactory extends TestCase {
    private TimeWindowViewFactory factory;

    public void setUp() {
        factory = new TimeWindowViewFactory();
    }

    public void testSetParameters() throws Exception {
        tryParameter(2d, 2000);
        tryParameter(4, 4000);
        tryParameter(3.3d, 3300);
        tryParameter(new Float(1.1f), 1100);

        tryInvalidParameter("theString");
        tryInvalidParameter(true);
    }

    public void testCanReuse() throws Exception {
        AgentInstanceContext agentInstanceContext = SupportStatementContextFactory.makeAgentInstanceContext();
        factory.setViewParameters(SupportStatementContextFactory.makeViewContext(), TestViewSupport.toExprListBean(new Object[]{1000}));
        assertFalse(factory.canReuse(new FirstElementView(null), agentInstanceContext));
        assertFalse(factory.canReuse(new TimeBatchView(null, SupportStatementContextFactory.makeAgentInstanceViewFactoryContext(), new ExprTimePeriodEvalDeltaConstGivenDelta(1000), null, false, false, null), agentInstanceContext));
        assertTrue(factory.canReuse(new TimeWindowView(SupportStatementContextFactory.makeAgentInstanceViewFactoryContext(), factory, new ExprTimePeriodEvalDeltaConstGivenDelta(1000000), null), agentInstanceContext));
    }

    private void tryInvalidParameter(Object param) throws Exception {
        try {
            TimeWindowViewFactory factory = new TimeWindowViewFactory();
            factory.setViewParameters(SupportStatementContextFactory.makeViewContext(), TestViewSupport.toExprListBean(new Object[]{param}));
            fail();
        } catch (ViewParameterException ex) {
            // expected
        }
    }

    private void tryParameter(Object param, long msec) throws Exception {
        TimeWindowViewFactory factory = new TimeWindowViewFactory();
        factory.setViewParameters(SupportStatementContextFactory.makeViewContext(), TestViewSupport.toExprListBean(new Object[]{param}));
        TimeWindowView view = (TimeWindowView) factory.makeView(SupportStatementContextFactory.makeAgentInstanceViewFactoryContext());
        assertEquals(msec, view.getTimeDeltaComputation().deltaAdd(0));
    }
}

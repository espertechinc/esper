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

public class TestTimeBatchViewFactory extends TestCase {
    private TimeBatchViewFactory factory;

    public void setUp() {
        factory = new TimeBatchViewFactory();
    }

    public void testSetParameters() throws Exception {
        tryParameter(new Object[]{2d}, 2000, null);
        tryParameter(new Object[]{4}, 4000, null);
        tryParameter(new Object[]{3.3d}, 3300, null);
        tryParameter(new Object[]{new Float(1.1f)}, 1100, null);
        tryParameter(new Object[]{99.9d, 364466464L}, 99900, 364466464L);

        tryInvalidParameter("theString");
        tryInvalidParameter(true);
        tryInvalidParameter(0);
    }

    public void testCanReuse() throws Exception {
        AgentInstanceContext agentInstanceContext = SupportStatementContextFactory.makeAgentInstanceContext();
        factory.setViewParameters(SupportStatementContextFactory.makeViewContext(), TestViewSupport.toExprListBean(new Object[]{1000}));
        assertFalse(factory.canReuse(new FirstElementView(null), agentInstanceContext));
        assertFalse(factory.canReuse(new TimeBatchView(factory, SupportStatementContextFactory.makeAgentInstanceViewFactoryContext(), new ExprTimePeriodEvalDeltaConstGivenDelta(1), null, false, false, null), agentInstanceContext));
        assertTrue(factory.canReuse(new TimeBatchView(factory, SupportStatementContextFactory.makeAgentInstanceViewFactoryContext(), new ExprTimePeriodEvalDeltaConstGivenDelta(1000000), null, false, false, null), agentInstanceContext));

        factory.setViewParameters(SupportStatementContextFactory.makeViewContext(), TestViewSupport.toExprListBean(new Object[]{1000, 2000L}));
        assertFalse(factory.canReuse(new TimeBatchView(factory, SupportStatementContextFactory.makeAgentInstanceViewFactoryContext(), new ExprTimePeriodEvalDeltaConstGivenDelta(1), null, false, false, null), agentInstanceContext));
        assertTrue(factory.canReuse(new TimeBatchView(factory, SupportStatementContextFactory.makeAgentInstanceViewFactoryContext(), new ExprTimePeriodEvalDeltaConstGivenDelta(1000000), 2000L, false, false, null), agentInstanceContext));
    }

    private void tryInvalidParameter(Object param) throws Exception {
        try {
            TimeBatchViewFactory factory = new TimeBatchViewFactory();
            factory.setViewParameters(SupportStatementContextFactory.makeViewContext(), TestViewSupport.toExprListBean(new Object[]{param}));
            fail();
        } catch (ViewParameterException ex) {
            // expected
        }
    }

    private void tryParameter(Object[] param, long msec, Long referencePoint) throws Exception {
        TimeBatchViewFactory factory = new TimeBatchViewFactory();
        factory.setViewParameters(SupportStatementContextFactory.makeViewContext(), TestViewSupport.toExprListBean(param));
        TimeBatchView view = (TimeBatchView) factory.makeView(SupportStatementContextFactory.makeAgentInstanceViewFactoryContext());
        assertTrue(new ExprTimePeriodEvalDeltaConstGivenDelta(msec).equalsTimePeriod(view.getTimeDeltaComputation()));
        assertEquals(referencePoint, view.getInitialReferencePoint());
    }
}

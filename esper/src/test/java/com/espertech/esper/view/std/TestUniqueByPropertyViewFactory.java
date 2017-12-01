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

import com.espertech.esper.core.context.util.AgentInstanceContext;
import com.espertech.esper.core.support.SupportStatementContextFactory;
import com.espertech.esper.epl.expression.core.ExprNodeUtilityCore;
import com.espertech.esper.supportunit.bean.SupportBean;
import com.espertech.esper.supportunit.event.SupportEventTypeFactory;
import com.espertech.esper.view.TestViewSupport;
import com.espertech.esper.view.ViewParameterException;
import junit.framework.TestCase;

public class TestUniqueByPropertyViewFactory extends TestCase {
    private UniqueByPropertyViewFactory factory;

    public void setUp() {
        factory = new UniqueByPropertyViewFactory();
    }

    public void testSetParameters() throws Exception {
        tryParameter("longPrimitive", "longPrimitive");
        tryInvalidParameter(1.1d);
    }

    public void testCanReuse() throws Exception {
        AgentInstanceContext agentInstanceContext = SupportStatementContextFactory.makeAgentInstanceContext();
        factory.setViewParameters(null, TestViewSupport.toExprListBean(new Object[]{"intPrimitive"}));
        factory.attach(SupportEventTypeFactory.createBeanType(SupportBean.class), SupportStatementContextFactory.makeContext(), null, null);
        assertFalse(factory.canReuse(new FirstElementView(null), agentInstanceContext));
    }

    private void tryInvalidParameter(Object param) throws Exception {
        try {
            UniqueByPropertyViewFactory factory = new UniqueByPropertyViewFactory();
            factory.setViewParameters(null, TestViewSupport.toExprListBean(new Object[]{param}));
            factory.attach(SupportEventTypeFactory.createBeanType(SupportBean.class), SupportStatementContextFactory.makeContext(), null, null);
            fail();
        } catch (ViewParameterException ex) {
            // expected
        }
    }

    private void tryParameter(Object param, String fieldName) throws Exception {
        UniqueByPropertyViewFactory factory = new UniqueByPropertyViewFactory();
        factory.setViewParameters(null, TestViewSupport.toExprListBean(new Object[]{param}));
        factory.attach(SupportEventTypeFactory.createBeanType(SupportBean.class), SupportStatementContextFactory.makeContext(), null, null);
        UniqueByPropertyView view = (UniqueByPropertyView) factory.makeView(SupportStatementContextFactory.makeAgentInstanceViewFactoryContext());
        assertEquals(fieldName, ExprNodeUtilityCore.toExpressionStringMinPrecedenceSafe(view.getCriteriaExpressions()[0]));
    }
}

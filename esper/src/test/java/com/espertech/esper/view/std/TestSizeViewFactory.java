/*
 * *************************************************************************************
 *  Copyright (C) 2006-2015 EsperTech, Inc. All rights reserved.                       *
 *  http://www.espertech.com/esper                                                     *
 *  http://www.espertech.com                                                           *
 *  ---------------------------------------------------------------------------------- *
 *  The software in this package is published under the terms of the GPL license       *
 *  a copy of which has been included with this distribution in the license.txt file.  *
 * *************************************************************************************
 */

package com.espertech.esper.view.std;

import com.espertech.esper.client.EventType;
import junit.framework.TestCase;

import com.espertech.esper.view.ViewParameterException;
import com.espertech.esper.view.TestViewSupport;
import com.espertech.esper.support.view.SupportStatementContextFactory;

public class TestSizeViewFactory extends TestCase
{
    private SizeViewFactory factory;

    public void setUp()
    {
        factory = new SizeViewFactory();
    }

    public void testSetParameters() throws Exception
    {
        tryParameter(new Object[] {});
    }

    public void testCanReuse() throws Exception
    {
        assertFalse(factory.canReuse(new LastElementView(null)));
        EventType type = SizeView.createEventType(SupportStatementContextFactory.makeContext(), null, 1);
        assertTrue(factory.canReuse(new SizeView(SupportStatementContextFactory.makeAgentInstanceContext(), type, null)));
    }

    private void tryParameter(Object[] param) throws Exception
    {
        SizeViewFactory factory = new SizeViewFactory();
        factory.setViewParameters(SupportStatementContextFactory.makeViewContext(), TestViewSupport.toExprListBean(param));
        assertTrue(factory.makeView(SupportStatementContextFactory.makeAgentInstanceViewFactoryContext()) instanceof SizeView);
    }
}

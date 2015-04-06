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

package com.espertech.esper.view.window;

import com.espertech.esper.support.view.SupportStatementContextFactory;
import com.espertech.esper.view.TestViewSupport;
import com.espertech.esper.view.ViewParameterException;
import com.espertech.esper.view.std.FirstElementView;
import junit.framework.TestCase;

public class TestLengthWindowViewFactory extends TestCase
{
    private LengthWindowViewFactory factory;

    public void setUp()
    {
        factory = new LengthWindowViewFactory();
    }

    public void testSetParameters() throws Exception
    {
        tryParameter(new Object[] {10}, 10);

        tryInvalidParameter("theString");
        tryInvalidParameter(true);
        tryInvalidParameter(1.1d);
        tryInvalidParameter(0);
    }

    public void testCanReuse() throws Exception
    {
        factory.setViewParameters(SupportStatementContextFactory.makeViewContext(), TestViewSupport.toExprListBean(new Object[] {1000}));
        assertFalse(factory.canReuse(new FirstElementView(null)));
        assertFalse(factory.canReuse(new LengthWindowView(SupportStatementContextFactory.makeAgentInstanceViewFactoryContext(), factory, 1, null)));
        assertTrue(factory.canReuse(new LengthWindowView(SupportStatementContextFactory.makeAgentInstanceViewFactoryContext(), factory, 1000, null)));
    }

    private void tryInvalidParameter(Object param) throws Exception
    {
        try
        {

            factory.setViewParameters(SupportStatementContextFactory.makeViewContext(), TestViewSupport.toExprListBean(new Object[] {param}));
            fail();
        }
        catch (ViewParameterException ex)
        {
            // expected
        }
    }

    private void tryParameter(Object[] param, int size) throws Exception
    {
        LengthWindowViewFactory factory = new LengthWindowViewFactory();
        factory.setViewParameters(SupportStatementContextFactory.makeViewContext(), TestViewSupport.toExprListBean(param));
        LengthWindowView view = (LengthWindowView) factory.makeView(SupportStatementContextFactory.makeAgentInstanceViewFactoryContext());
        assertEquals(size, view.getSize());
    }
}

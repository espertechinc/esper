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

package com.espertech.esper.regression.view;

import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import junit.framework.TestCase;
import com.espertech.esper.client.*;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.client.SupportConfigFactory;

public class TestViewGroupWinTypes extends TestCase
{
    private EPServiceProvider epService;

    public void setUp()
    {
        epService = EPServiceProviderManager.getDefaultProvider(SupportConfigFactory.getConfiguration());
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}
    }

    public void tearDown() {
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
    }

    public void testType()
    {
        String viewStmt = "select * from " + SupportBean.class.getName() +
                "#groupwin(intPrimitive)#length(4)#groupwin(longBoxed)#uni(doubleBoxed)";
        EPStatement stmt = epService.getEPAdministrator().createEPL(viewStmt);

        assertEquals(int.class, stmt.getEventType().getPropertyType("intPrimitive"));
        assertEquals(Long.class, stmt.getEventType().getPropertyType("longBoxed"));
        assertEquals(Double.class, stmt.getEventType().getPropertyType("stddev"));
        assertEquals(8, stmt.getEventType().getPropertyNames().length);
    }
}

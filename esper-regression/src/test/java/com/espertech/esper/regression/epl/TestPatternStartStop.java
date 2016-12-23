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

package com.espertech.esper.regression.epl;

import com.espertech.esper.client.scopetest.SupportUpdateListener;
import junit.framework.TestCase;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPServiceProviderManager;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportBeanComplexProps;
import com.espertech.esper.supportregression.client.SupportConfigFactory;

public class TestPatternStartStop extends TestCase
{
    private EPServiceProvider epService;
    private SupportUpdateListener updateListener;

    public void setUp()
    {
        epService = EPServiceProviderManager.getDefaultProvider(SupportConfigFactory.getConfiguration());
        epService.initialize();
        updateListener = new SupportUpdateListener();
    }

    protected void tearDown() throws Exception {
        updateListener = null;
    }

    public void testStartStop()
    {
        String stmtText = "select * from pattern [every(a=" + SupportBean.class.getName() +
                " or b=" + SupportBeanComplexProps.class.getName() + ")]";
        EPStatement statement = epService.getEPAdministrator().createEPL(stmtText);
        statement.addListener(updateListener);

        for (int i = 0; i < 100; i++)
        {
            sendAndAssert();

            statement.stop();

            epService.getEPRuntime().sendEvent(new SupportBean());
            epService.getEPRuntime().sendEvent(SupportBeanComplexProps.makeDefaultBean());
            assertFalse(updateListener.isInvoked());

            statement.start();
        }
    }

    private void sendAndAssert()
    {
        for (int i = 0; i < 1000; i++)
        {
            Object theEvent = null;
            if (i % 3 == 0)
            {
                theEvent = new SupportBean();
            }
            else
            {
                theEvent = SupportBeanComplexProps.makeDefaultBean();
            }

            epService.getEPRuntime().sendEvent(theEvent);

            EventBean eventBean = updateListener.assertOneGetNewAndReset();
            if (theEvent instanceof SupportBean)
            {
                assertSame(theEvent, eventBean.get("a"));
                assertNull(eventBean.get("b"));
            }
            else
            {
                assertSame(theEvent, eventBean.get("b"));
                assertNull(eventBean.get("a"));
            }
        }
    }
}

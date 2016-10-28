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
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import junit.framework.TestCase;
import com.espertech.esper.client.*;
import com.espertech.esper.support.bean.SupportBean;
import com.espertech.esper.support.client.SupportConfigFactory;
import com.espertech.esper.client.EventBean;

import java.util.Map;
import java.util.HashMap;

public class TestJoinMapType extends TestCase
{
    private EPServiceProvider epService;
    private SupportUpdateListener listener;

    public void setUp()
    {
        Map<String, Object> typeInfo = new HashMap<String, Object>();
        typeInfo.put("id", String.class);
        typeInfo.put("p00", int.class);
        
        Configuration config = SupportConfigFactory.getConfiguration();
        config.addEventType("MapS0", typeInfo);
        config.addEventType("MapS1", typeInfo);

        config.getEngineDefaults().getLogging().setEnableQueryPlan(true);
        epService = EPServiceProviderManager.getDefaultProvider(config);
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}
        listener = new SupportUpdateListener();
    }

    protected void tearDown() throws Exception {
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
        listener = null;
    }

    public void testJoinMapEvent()
    {
        String joinStatement = "select S0.id, S1.id, S0.p00, S1.p00 from MapS0#keepall() as S0, MapS1#keepall() as S1" +
                " where S0.id = S1.id";

        EPStatement stmt = epService.getEPAdministrator().createEPL(joinStatement);
        stmt.addListener(listener);

        runAssertion();

        stmt.destroy();
        joinStatement = "select * from MapS0#keepall() as S0, MapS1#keepall() as S1 where S0.id = S1.id";
        stmt = epService.getEPAdministrator().createEPL(joinStatement);
        stmt.addListener(listener);

        runAssertion();
    }

    private void runAssertion()
    {
        sendMapEvent("MapS0", "a", 1);
        assertFalse(listener.isInvoked());
        
        sendMapEvent("MapS1", "a", 2);
        EventBean theEvent = listener.assertOneGetNewAndReset();
        assertEquals("a", theEvent.get("S0.id"));
        assertEquals("a", theEvent.get("S1.id"));
        assertEquals(1, theEvent.get("S0.p00"));
        assertEquals(2, theEvent.get("S1.p00"));

        sendMapEvent("MapS1", "b", 3);
        sendMapEvent("MapS0", "c", 4);
        assertFalse(listener.isInvoked());
    }

    public void testJoinMapEventNotUnique()
    {
        // Test for Esper-122 
        String joinStatement = "select S0.id, S1.id, S0.p00, S1.p00 from MapS0#keepall() as S0, MapS1#keepall() as S1" +
                " where S0.id = S1.id";

        EPStatement stmt = epService.getEPAdministrator().createEPL(joinStatement);
        stmt.addListener(listener);

        for (int i = 0; i < 100; i++)
        {
            if (i % 2 == 1)
            {
                sendMapEvent("MapS0", "a", 1);
            }
            else
            {
                sendMapEvent("MapS1", "a", 1);
            }
        }
    }

    public void testJoinWrapperEventNotUnique()
    {
        // Test for Esper-122
        epService.getEPAdministrator().createEPL("insert into S0 select 's0' as streamone, * from " + SupportBean.class.getName());
        epService.getEPAdministrator().createEPL("insert into S1 select 's1' as streamtwo, * from " + SupportBean.class.getName());
        String joinStatement = "select * from S0#keepall() as a, S1#keepall() as b where a.intBoxed = b.intBoxed";

        EPStatement stmt = epService.getEPAdministrator().createEPL(joinStatement);
        stmt.addListener(listener);

        for (int i = 0; i < 100; i++)
        {
            epService.getEPRuntime().sendEvent(new SupportBean());
        }
    }

    private void sendMapEvent(String name, String id, int p00)
    {
        Map<String, Object> theEvent = new HashMap<String, Object>();
        theEvent.put("id", id);
        theEvent.put("p00", p00);
        epService.getEPRuntime().sendEvent(theEvent, name);
    }
}

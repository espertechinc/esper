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
package com.espertech.esper.regression.resultset;

import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPServiceProviderManager;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.client.time.CurrentTimeEvent;
import com.espertech.esper.client.util.DateTime;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportBean_S0;
import com.espertech.esper.supportregression.client.SupportConfigFactory;
import junit.framework.TestCase;

public class TestOutputLimitParameterizedByContext extends TestCase
 {
     private EPServiceProvider epService;
     private SupportUpdateListener listener;

     public void setUp()
     {
         epService = EPServiceProviderManager.getDefaultProvider(SupportConfigFactory.getConfiguration());
         epService.initialize();
         if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}
         listener = new SupportUpdateListener();
     }

     protected void tearDown() throws Exception {
         if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
         listener = null;
     }

     public void testCrontabFromContext() {
         epService.getEPAdministrator().getConfiguration().addEventType(MySimpleScheduleEvent.class);
         epService.getEPAdministrator().getConfiguration().addEventType(SupportBean_S0.class);

         epService.getEPRuntime().sendEvent(new CurrentTimeEvent(DateTime.parseDefaultMSec("2002-05-01T09:00:00.000")));
         epService.getEPAdministrator().createEPL("create context MyCtx start MySimpleScheduleEvent as sse");
         EPStatement stmt = epService.getEPAdministrator().createEPL("context MyCtx\n" +
                 "select count(*) as c \n" +
                 "from SupportBean_S0\n" +
                 "output last at(context.sse.atminute, context.sse.athour, *, *, *, *) and when terminated\n");
         stmt.addListener(listener);

         epService.getEPRuntime().sendEvent(new MySimpleScheduleEvent(10, 15));
         epService.getEPRuntime().sendEvent(new SupportBean_S0(0));

         epService.getEPRuntime().sendEvent(new CurrentTimeEvent(DateTime.parseDefaultMSec("2002-05-01T10:14:59.000")));
         assertFalse(listener.getAndClearIsInvoked());

         epService.getEPRuntime().sendEvent(new CurrentTimeEvent(DateTime.parseDefaultMSec("2002-05-01T10:15:00.000")));
         assertTrue(listener.getAndClearIsInvoked());
     }

     public static class MySimpleScheduleEvent
     {
         private int athour;
         private int atminute;

         public MySimpleScheduleEvent(int athour, int atminute) {
             this.athour = athour;
             this.atminute = atminute;
         }

         public int getAthour() {
             return athour;
         }

         public int getAtminute() {
             return atminute;
         }
     }
 }

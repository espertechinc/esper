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

package com.espertech.esper.regression.datetime;

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPServiceProviderManager;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.client.time.CurrentTimeEvent;
import com.espertech.esper.client.util.DateTime;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.supportregression.bean.SupportDateTime;
import com.espertech.esper.supportregression.bean.lambda.LambdaAssertionUtil;
import com.espertech.esper.supportregression.client.SupportConfigFactory;
import junit.framework.TestCase;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Date;

public class TestDTWithTime extends TestCase {

    private EPServiceProvider epService;
    private SupportUpdateListener listener;

    public void setUp() {

        Configuration config = SupportConfigFactory.getConfiguration();
        config.addEventType("SupportDateTime", SupportDateTime.class);
        epService = EPServiceProviderManager.getDefaultProvider(config);
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}
        listener = new SupportUpdateListener();
    }

    public void tearDown() {
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
        listener = null;
    }

    public void testWithTime() {

        epService.getEPAdministrator().createEPL("create variable int varhour");
        epService.getEPAdministrator().createEPL("create variable int varmin");
        epService.getEPAdministrator().createEPL("create variable int varsec");
        epService.getEPAdministrator().createEPL("create variable int varmsec");
        String startTime = "2002-05-30T09:00:00.000";
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(DateTime.parseDefaultMSec(startTime)));

        String[] fields = "val0,val1,val2,val3,val4,val5".split(",");
        String eplFragment = "select " +
                "current_timestamp.withTime(varhour, varmin, varsec, varmsec) as val0," +
                "utildate.withTime(varhour, varmin, varsec, varmsec) as val1," +
                "msecdate.withTime(varhour, varmin, varsec, varmsec) as val2," +
                "caldate.withTime(varhour, varmin, varsec, varmsec) as val3," +
                "localdate.withTime(varhour, varmin, varsec, varmsec) as val4," +
                "zoneddate.withTime(varhour, varmin, varsec, varmsec) as val5" +
                " from SupportDateTime";
        EPStatement stmtFragment = epService.getEPAdministrator().createEPL(eplFragment);
        stmtFragment.addListener(listener);
        LambdaAssertionUtil.assertTypes(stmtFragment.getEventType(), fields, new Class[]{Long.class, Date.class, Long.class, Calendar.class, LocalDateTime.class, ZonedDateTime.class});

        epService.getEPRuntime().sendEvent(SupportDateTime.make(null));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{SupportDateTime.getValueCoerced(startTime, "msec"), null, null, null, null, null});

        String expectedTime = "2002-05-30T09:00:00.000";
        epService.getEPRuntime().setVariableValue("varhour", null); // variable is null
        epService.getEPRuntime().sendEvent(SupportDateTime.make(startTime));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, SupportDateTime.getArrayCoerced(expectedTime, "msec", "util", "msec", "cal", "ldt", "zdt"));

        expectedTime = "2002-05-30T01:02:03.004";
        epService.getEPRuntime().setVariableValue("varhour", 1);
        epService.getEPRuntime().setVariableValue("varmin", 2);
        epService.getEPRuntime().setVariableValue("varsec", 3);
        epService.getEPRuntime().setVariableValue("varmsec", 4);
        epService.getEPRuntime().sendEvent(SupportDateTime.make(startTime));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, SupportDateTime.getArrayCoerced(expectedTime, "msec", "util", "msec", "cal", "ldt", "zdt"));

        expectedTime = "2002-05-30T00:00:00.006";
        epService.getEPRuntime().setVariableValue("varhour", 0);
        epService.getEPRuntime().setVariableValue("varmin", null);
        epService.getEPRuntime().setVariableValue("varsec", null);
        epService.getEPRuntime().setVariableValue("varmsec", 6);
        epService.getEPRuntime().sendEvent(SupportDateTime.make(startTime));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, SupportDateTime.getArrayCoerced(expectedTime, "msec", "util", "msec", "cal", "ldt", "zdt"));
    }
}

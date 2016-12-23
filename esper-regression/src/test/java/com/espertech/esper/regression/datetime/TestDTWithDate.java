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

public class TestDTWithDate extends TestCase {

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

    public void testWithDate() {

        epService.getEPAdministrator().createEPL("create variable int varyear");
        epService.getEPAdministrator().createEPL("create variable int varmonth");
        epService.getEPAdministrator().createEPL("create variable int varday");

        String startTime = "2002-05-30T09:00:00.000";
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(DateTime.parseDefaultMSec(startTime)));

        String[] fields = "val0,val1,val2,val3,val4,val5".split(",");
        String eplFragment = "select " +
                "current_timestamp.withDate(varyear, varmonth, varday) as val0," +
                "utildate.withDate(varyear, varmonth, varday) as val1," +
                "msecdate.withDate(varyear, varmonth, varday) as val2," +
                "caldate.withDate(varyear, varmonth, varday) as val3," +
                "localdate.withDate(varyear, varmonth+1, varday) as val4," +
                "zoneddate.withDate(varyear, varmonth+1, varday) as val5" +
                " from SupportDateTime";
        EPStatement stmtFragment = epService.getEPAdministrator().createEPL(eplFragment);
        stmtFragment.addListener(listener);
        LambdaAssertionUtil.assertTypes(stmtFragment.getEventType(), fields, new Class[]{Long.class, Date.class, Long.class, Calendar.class, LocalDateTime.class, ZonedDateTime.class});

        epService.getEPRuntime().sendEvent(SupportDateTime.make(null));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{SupportDateTime.getValueCoerced(startTime, "msec"), null, null, null, null, null});

        String expectedTime = "2004-09-03T09:00:00.000";
        epService.getEPRuntime().setVariableValue("varyear", 2004);
        epService.getEPRuntime().setVariableValue("varmonth", 8);
        epService.getEPRuntime().setVariableValue("varday", 3);
        epService.getEPRuntime().sendEvent(SupportDateTime.make(startTime));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, SupportDateTime.getArrayCoerced(expectedTime, "msec", "util", "msec", "cal", "ldt", "zdt"));

        expectedTime = "2002-09-30T09:00:00.000";
        epService.getEPRuntime().setVariableValue("varyear", null);
        epService.getEPRuntime().setVariableValue("varmonth", 8);
        epService.getEPRuntime().setVariableValue("varday", null);
        epService.getEPRuntime().sendEvent(SupportDateTime.make(startTime));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, SupportDateTime.getArrayCoerced(expectedTime, "msec", "util", "msec", "cal", "ldt", "zdt"));
    }
}

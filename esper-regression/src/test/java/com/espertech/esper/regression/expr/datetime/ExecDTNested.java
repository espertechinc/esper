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
package com.espertech.esper.regression.expr.datetime;

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.supportregression.bean.SupportDateTime;
import com.espertech.esper.supportregression.bean.lambda.LambdaAssertionUtil;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Date;

public class ExecDTNested implements RegressionExecution {

    public void configure(Configuration configuration) throws Exception {
        configuration.addEventType("SupportDateTime", SupportDateTime.class);
    }

    public void run(EPServiceProvider epService) throws Exception {
        String[] fields = "val0,val1,val2,val3,val4".split(",");
        String eplFragment = "select " +
                "utildate.set('hour', 1).set('minute', 2).set('second', 3) as val0," +
                "longdate.set('hour', 1).set('minute', 2).set('second', 3) as val1," +
                "caldate.set('hour', 1).set('minute', 2).set('second', 3) as val2," +
                "localdate.set('hour', 1).set('minute', 2).set('second', 3) as val3," +
                "zoneddate.set('hour', 1).set('minute', 2).set('second', 3) as val4" +
                " from SupportDateTime";
        EPStatement stmtFragment = epService.getEPAdministrator().createEPL(eplFragment);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmtFragment.addListener(listener);
        LambdaAssertionUtil.assertTypes(stmtFragment.getEventType(), fields, new Class[]{Date.class, Long.class, Calendar.class, LocalDateTime.class, ZonedDateTime.class});

        String startTime = "2002-05-30T09:00:00.000";
        String expectedTime = "2002-05-30T01:02:03.000";
        epService.getEPRuntime().sendEvent(SupportDateTime.make(startTime));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, SupportDateTime.getArrayCoerced(expectedTime, "util", "long", "cal", "ldt", "zdt"));

        stmtFragment.destroy();
        eplFragment = "select " +
                "utildate.set('hour', 1).set('minute', 2).set('second', 3).toCalendar() as val0," +
                "longdate.set('hour', 1).set('minute', 2).set('second', 3).toCalendar() as val1," +
                "caldate.set('hour', 1).set('minute', 2).set('second', 3).toCalendar() as val2," +
                "localdate.set('hour', 1).set('minute', 2).set('second', 3).toCalendar() as val3," +
                "zoneddate.set('hour', 1).set('minute', 2).set('second', 3).toCalendar() as val4" +
                " from SupportDateTime";
        stmtFragment = epService.getEPAdministrator().createEPL(eplFragment);
        stmtFragment.addListener(listener);
        LambdaAssertionUtil.assertTypes(stmtFragment.getEventType(), fields, new Class[]{Calendar.class, Calendar.class, Calendar.class, Calendar.class, Calendar.class});

        epService.getEPRuntime().sendEvent(SupportDateTime.make(startTime));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, SupportDateTime.getArrayCoerced(expectedTime, "cal", "cal", "cal", "cal", "cal"));
    }
}

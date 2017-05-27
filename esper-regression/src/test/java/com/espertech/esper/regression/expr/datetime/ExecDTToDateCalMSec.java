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
import com.espertech.esper.client.time.CurrentTimeEvent;
import com.espertech.esper.client.util.DateTime;
import com.espertech.esper.supportregression.bean.SupportDateTime;
import com.espertech.esper.supportregression.bean.lambda.LambdaAssertionUtil;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import java.util.Calendar;
import java.util.Date;

public class ExecDTToDateCalMSec implements RegressionExecution {

    public void configure(Configuration configuration) throws Exception {
        configuration.addEventType("SupportDateTime", SupportDateTime.class);
    }

    public void run(EPServiceProvider epService) throws Exception {

        String startTime = "2002-05-30T09:00:00.000";
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(DateTime.parseDefaultMSec(startTime)));

        String[] fields = "val0,val1,val2,val3,val4,val5,val6,val7,val8,val9,val10,val11,val12,val13,val14,val15,val16,val17".split(",");
        String eplFragment = "select " +
                "current_timestamp.toDate() as val0," +
                "utildate.toDate() as val1," +
                "longdate.toDate() as val2," +
                "caldate.toDate() as val3," +
                "localdate.toDate() as val4," +
                "zoneddate.toDate() as val5," +
                "current_timestamp.toCalendar() as val6," +
                "utildate.toCalendar() as val7," +
                "longdate.toCalendar() as val8," +
                "caldate.toCalendar() as val9," +
                "localdate.toCalendar() as val10," +
                "zoneddate.toCalendar() as val11," +
                "current_timestamp.toMillisec() as val12," +
                "utildate.toMillisec() as val13," +
                "longdate.toMillisec() as val14," +
                "caldate.toMillisec() as val15," +
                "localdate.toMillisec() as val16," +
                "zoneddate.toMillisec() as val17" +
                " from SupportDateTime";
        EPStatement stmtFragment = epService.getEPAdministrator().createEPL(eplFragment);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmtFragment.addListener(listener);
        LambdaAssertionUtil.assertTypes(stmtFragment.getEventType(), fields, new Class[]{Date.class, Date.class, Date.class, Date.class, Date.class, Date.class,
            Calendar.class, Calendar.class, Calendar.class, Calendar.class, Calendar.class, Calendar.class,
            Long.class, Long.class, Long.class, Long.class, Long.class, Long.class});

        epService.getEPRuntime().sendEvent(SupportDateTime.make(startTime));
        Object[] expectedUtil = SupportDateTime.getArrayCoerced(startTime, "util", "util", "util", "util", "util", "util");
        Object[] expectedCal = SupportDateTime.getArrayCoerced(startTime, "cal", "cal", "cal", "cal", "cal", "cal");
        Object[] expectedMsec = SupportDateTime.getArrayCoerced(startTime, "long", "long", "long", "long", "long", "long");
        Object[] expected = EPAssertionUtil.concatenateArray(expectedUtil, expectedCal, expectedMsec);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, expected);

        epService.getEPRuntime().sendEvent(SupportDateTime.make(null));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{
                SupportDateTime.getValueCoerced(startTime, "util"), null, null, null, null, null,
                SupportDateTime.getValueCoerced(startTime, "cal"), null, null, null, null, null,
                SupportDateTime.getValueCoerced(startTime, "long"), null, null, null, null, null});
    }
}

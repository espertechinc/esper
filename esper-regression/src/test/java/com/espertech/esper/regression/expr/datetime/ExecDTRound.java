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

public class ExecDTRound implements RegressionExecution {

    public void run(EPServiceProvider epService) throws Exception {
        epService.getEPAdministrator().getConfiguration().addEventType("SupportDateTime", SupportDateTime.class);

        runAssertionInput(epService);
        runAssertionRoundCeil(epService);
        runAssertionRoundFloor(epService);
        runAssertionRoundHalf(epService);
    }

    private void runAssertionInput(EPServiceProvider epService) {

        String[] fields = "val0,val1,val2,val3,val4".split(",");
        String eplFragment = "select " +
                "utildate.roundCeiling('hour') as val0," +
                "longdate.roundCeiling('hour') as val1," +
                "caldate.roundCeiling('hour') as val2," +
                "localdate.roundCeiling('hour') as val3," +
                "zoneddate.roundCeiling('hour') as val4" +
                " from SupportDateTime";
        EPStatement stmtFragment = epService.getEPAdministrator().createEPL(eplFragment);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmtFragment.addListener(listener);
        LambdaAssertionUtil.assertTypes(stmtFragment.getEventType(), fields, new Class[]{Date.class, Long.class, Calendar.class, LocalDateTime.class, ZonedDateTime.class});

        String startTime = "2002-05-30T09:01:02.003";
        String expectedTime = "2002-5-30T10:00:00.000";
        epService.getEPRuntime().sendEvent(SupportDateTime.make(startTime));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, SupportDateTime.getArrayCoerced(expectedTime, "util", "long", "cal", "ldt", "zdt"));

        stmtFragment.destroy();
    }

    private void runAssertionRoundCeil(EPServiceProvider epService) {

        String[] fields = "val0,val1,val2,val3,val4,val5,val6".split(",");
        String eplFragment = "select " +
                "utildate.roundCeiling('msec') as val0," +
                "utildate.roundCeiling('sec') as val1," +
                "utildate.roundCeiling('minutes') as val2," +
                "utildate.roundCeiling('hour') as val3," +
                "utildate.roundCeiling('day') as val4," +
                "utildate.roundCeiling('month') as val5," +
                "utildate.roundCeiling('year') as val6" +
                " from SupportDateTime";
        EPStatement stmtFragment = epService.getEPAdministrator().createEPL(eplFragment);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmtFragment.addListener(listener);
        LambdaAssertionUtil.assertTypes(stmtFragment.getEventType(), fields, new Class[]{Date.class, Date.class, Date.class, Date.class, Date.class, Date.class, Date.class});

        String[] expected = {
            "2002-05-30T09:01:02.003",
            "2002-05-30T09:01:03.000",
            "2002-05-30T09:02:00.000",
            "2002-05-30T10:00:00.000",
            "2002-05-31T00:00:00.000",
            "2002-06-1T00:00:00.000",
            "2003-01-1T00:00:00.000",
        };
        String startTime = "2002-05-30T09:01:02.003";
        epService.getEPRuntime().sendEvent(SupportDateTime.make(startTime));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, SupportDateTime.getArrayCoerced(expected, "util"));

        stmtFragment.destroy();
    }

    private void runAssertionRoundFloor(EPServiceProvider epService) {

        String[] fields = "val0,val1,val2,val3,val4,val5,val6".split(",");
        String eplFragment = "select " +
                "utildate.roundFloor('msec') as val0," +
                "utildate.roundFloor('sec') as val1," +
                "utildate.roundFloor('minutes') as val2," +
                "utildate.roundFloor('hour') as val3," +
                "utildate.roundFloor('day') as val4," +
                "utildate.roundFloor('month') as val5," +
                "utildate.roundFloor('year') as val6" +
                " from SupportDateTime";
        EPStatement stmtFragment = epService.getEPAdministrator().createEPL(eplFragment);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmtFragment.addListener(listener);
        LambdaAssertionUtil.assertTypes(stmtFragment.getEventType(), fields, new Class[]{Date.class, Date.class, Date.class, Date.class, Date.class, Date.class, Date.class});

        String[] expected = {
            "2002-05-30T09:01:02.003",
            "2002-05-30T09:01:02.000",
            "2002-05-30T09:01:00.000",
            "2002-05-30T09:00:00.000",
            "2002-05-30T00:00:00.000",
            "2002-05-1T00:00:00.000",
            "2002-01-1T00:00:00.000",
        };
        String startTime = "2002-05-30T09:01:02.003";
        epService.getEPRuntime().sendEvent(SupportDateTime.make(startTime));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, SupportDateTime.getArrayCoerced(expected, "util"));

        stmtFragment.destroy();
    }

    private void runAssertionRoundHalf(EPServiceProvider epService) {

        String[] fields = "val0,val1,val2,val3,val4,val5,val6".split(",");
        String eplFragment = "select " +
                "utildate.roundHalf('msec') as val0," +
                "utildate.roundHalf('sec') as val1," +
                "utildate.roundHalf('minutes') as val2," +
                "utildate.roundHalf('hour') as val3," +
                "utildate.roundHalf('day') as val4," +
                "utildate.roundHalf('month') as val5," +
                "utildate.roundHalf('year') as val6" +
                " from SupportDateTime";
        EPStatement stmtFragment = epService.getEPAdministrator().createEPL(eplFragment);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmtFragment.addListener(listener);
        LambdaAssertionUtil.assertTypes(stmtFragment.getEventType(), fields, new Class[]{Date.class, Date.class, Date.class, Date.class, Date.class, Date.class, Date.class});

        String[] expected = {
            "2002-05-30T15:30:02.550",
            "2002-05-30T15:30:03.000",
            "2002-05-30T15:30:00.000",
            "2002-05-30T16:00:00.00",
            "2002-05-31T00:00:00.000",
            "2002-06-01T00:00:00.000",
            "2002-01-01T00:00:00.000",
        };
        String startTime = "2002-05-30T15:30:02.550";
        epService.getEPRuntime().sendEvent(SupportDateTime.make(startTime));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, SupportDateTime.getArrayCoerced(expected, "util"));

        // test rounding up/down
        stmtFragment.destroy();
        fields = "val0".split(",");
        eplFragment = "select utildate.roundHalf('min') as val0 from SupportDateTime";
        stmtFragment = epService.getEPAdministrator().createEPL(eplFragment);
        stmtFragment.addListener(listener);

        epService.getEPRuntime().sendEvent(SupportDateTime.make("2002-05-30T15:30:29.999"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{SupportDateTime.getValueCoerced("2002-05-30T15:30:00.000", "util")});

        epService.getEPRuntime().sendEvent(SupportDateTime.make("2002-05-30T15:30:30.000"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{SupportDateTime.getValueCoerced("2002-05-30T15:31:00.000", "util")});

        epService.getEPRuntime().sendEvent(SupportDateTime.make("2002-05-30T15:30:30.001"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{SupportDateTime.getValueCoerced("2002-05-30T15:31:00.000", "util")});

        stmtFragment.destroy();
    }
}

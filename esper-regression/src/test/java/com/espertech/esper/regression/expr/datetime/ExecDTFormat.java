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

import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;

public class ExecDTFormat implements RegressionExecution {

    public void configure(Configuration configuration) throws Exception {
        configuration.addEventType("SupportDateTime", SupportDateTime.class);
    }

    public void run(EPServiceProvider epService) throws Exception {
        runAssertionFormat(epService);
        runAssertionFormatWString(epService);
    }

    private void runAssertionFormat(EPServiceProvider epService) {

        String startTime = "2002-05-30T09:00:00.000";
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(DateTime.parseDefaultMSec(startTime)));

        String[] fields = "val0,val1,val2,val3,val4,val5".split(",");
        String eplFragment = "select " +
                "current_timestamp.format() as val0," +
                "utildate.format() as val1," +
                "longdate.format() as val2," +
                "caldate.format() as val3," +
                "localdate.format() as val4," +
                "zoneddate.format() as val5" +
                " from SupportDateTime";
        EPStatement stmtFragment = epService.getEPAdministrator().createEPL(eplFragment);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmtFragment.addListener(listener);
        LambdaAssertionUtil.assertTypes(stmtFragment.getEventType(), fields, new Class[]{String.class, String.class, String.class, String.class, String.class, String.class});

        epService.getEPRuntime().sendEvent(SupportDateTime.make(startTime));
        Object[] expected = SupportDateTime.getArrayCoerced(startTime, "sdf", "sdf", "sdf", "sdf", "dtf_isodt", "dtf_isozdt");
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, expected);

        epService.getEPRuntime().sendEvent(SupportDateTime.make(null));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{SupportDateTime.getValueCoerced(startTime, "sdf"), null, null, null, null, null});

        stmtFragment.destroy();
    }

    private void runAssertionFormatWString(EPServiceProvider epService) {

        String startTime = "2002-05-30T09:00:00.000";
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(DateTime.parseDefaultMSec(startTime)));
        String sdfPattern = "yyyy.MM.dd G 'at' HH:mm:ss";
        SimpleDateFormat sdf = new SimpleDateFormat(sdfPattern);
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern(sdfPattern);

        String[] fields = "val0,val1,val2,val3,val4,val5,val6".split(",");
        String eplFragment = "select " +
                "longdate.format(\"" + sdfPattern + "\") as val0," +
                "utildate.format(\"" + sdfPattern + "\") as val1," +
                "caldate.format(\"" + sdfPattern + "\") as val2," +
                "localdate.format(\"" + sdfPattern + "\") as val3," +
                "zoneddate.format(\"" + sdfPattern + "\") as val4," +
                "utildate.format(SimpleDateFormat.getDateInstance()) as val5," +
                "localdate.format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE) as val6" +
                " from SupportDateTime";
        EPStatement stmtFragment = epService.getEPAdministrator().createEPL(eplFragment);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmtFragment.addListener(listener);
        LambdaAssertionUtil.assertTypesAllSame(stmtFragment.getEventType(), fields, String.class);

        SupportDateTime sdt = SupportDateTime.make(startTime);
        epService.getEPRuntime().sendEvent(SupportDateTime.make(startTime));
        Object[] expected = new Object[]{
                sdf.format(sdt.getLongdate()), sdf.format(sdt.getUtildate()), sdf.format(sdt.getCaldate().getTime()),
                sdt.getLocaldate().format(dtf), sdt.getZoneddate().format(dtf),
                SimpleDateFormat.getDateInstance().format(sdt.getUtildate()), sdt.getLocaldate().format(DateTimeFormatter.BASIC_ISO_DATE)
        };
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, expected);

        epService.getEPRuntime().sendEvent(SupportDateTime.make(null));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{null, null, null, null, null, null, null});

        stmtFragment.destroy();
    }
}

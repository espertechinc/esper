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
package com.espertech.esper.regressionlib.suite.expr.datetime;

import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.client.util.DateTime;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.support.bean.SupportDateTime;
import com.espertech.esper.regressionlib.support.util.LambdaAssertionUtil;

import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;

public class ExprDTFormat {

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> executions = new ArrayList<>();
        executions.add(new ExprDTFormatSimple());
        executions.add(new ExprDTFormatWString());
        return executions;
    }

    private static class ExprDTFormatSimple implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            String startTime = "2002-05-30T09:00:00.000";
            env.advanceTime(DateTime.parseDefaultMSec(startTime));

            String[] fields = "val0,val1,val2,val3,val4,val5".split(",");
            String eplFragment = "@name('s0') select " +
                "current_timestamp.format() as val0," +
                "utildate.format() as val1," +
                "longdate.format() as val2," +
                "caldate.format() as val3," +
                "localdate.format() as val4," +
                "zoneddate.format() as val5" +
                " from SupportDateTime";
            env.compileDeploy(eplFragment).addListener("s0");
            LambdaAssertionUtil.assertTypes(env.statement("s0").getEventType(), fields, new Class[]{String.class, String.class, String.class, String.class, String.class, String.class});

            env.sendEventBean(SupportDateTime.make(startTime));
            Object[] expected = SupportDateTime.getArrayCoerced(startTime, "sdf", "sdf", "sdf", "sdf", "dtf_isodt", "dtf_isozdt");
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, expected);

            env.sendEventBean(SupportDateTime.make(null));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{SupportDateTime.getValueCoerced(startTime, "sdf"), null, null, null, null, null});

            env.undeployAll();
        }
    }

    private static class ExprDTFormatWString implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            String startTime = "2002-05-30T09:00:00.000";
            env.advanceTime(DateTime.parseDefaultMSec(startTime));
            String sdfPattern = "yyyy.MM.dd G 'at' HH:mm:ss";
            SimpleDateFormat sdf = new SimpleDateFormat(sdfPattern);
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern(sdfPattern);

            String[] fields = "val0,val1,val2,val3,val4,val5,val6".split(",");
            String eplFragment = "@name('s0') select " +
                "longdate.format(\"" + sdfPattern + "\") as val0," +
                "utildate.format(\"" + sdfPattern + "\") as val1," +
                "caldate.format(\"" + sdfPattern + "\") as val2," +
                "localdate.format(\"" + sdfPattern + "\") as val3," +
                "zoneddate.format(\"" + sdfPattern + "\") as val4," +
                "utildate.format(SimpleDateFormat.getDateInstance()) as val5," +
                "localdate.format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE) as val6" +
                " from SupportDateTime";
            env.compileDeploy(eplFragment).addListener("s0");
            LambdaAssertionUtil.assertTypesAllSame(env.statement("s0").getEventType(), fields, String.class);

            SupportDateTime sdt = SupportDateTime.make(startTime);
            env.sendEventBean(SupportDateTime.make(startTime));
            Object[] expected = new Object[]{
                sdf.format(sdt.getLongdate()), sdf.format(sdt.getUtildate()), sdf.format(sdt.getCaldate().getTime()),
                sdt.getLocaldate().format(dtf), sdt.getZoneddate().format(dtf),
                SimpleDateFormat.getDateInstance().format(sdt.getUtildate()), sdt.getLocaldate().format(DateTimeFormatter.BASIC_ISO_DATE)
            };
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, expected);

            env.sendEventBean(SupportDateTime.make(null));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{null, null, null, null, null, null, null});

            env.undeployAll();
        }
    }
}

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
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionlib.support.bean.SupportDateTime;
import com.espertech.esper.regressionlib.support.bean.SupportTimeStartEndA;
import com.espertech.esper.regressionlib.support.util.LambdaAssertionUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;

public class ExprDTBetween {

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> executions = new ArrayList<>();
        executions.add(new ExprDTBetweenIncludeEndpoints());
        executions.add(new ExprDTBetweenExcludeEndpoints());
        executions.add(new ExprDTBetweenTypes());
        return executions;
    }

    private static class ExprDTBetweenTypes implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "c0,c1,c2,c3,c4".split(",");
            String eplCurrentTS = "@name('s0') select " +
                "longdate.between(longPrimitive, longBoxed) as c0, " +
                "utildate.between(longPrimitive, longBoxed) as c1, " +
                "caldate.between(longPrimitive, longBoxed) as c2," +
                "localdate.between(longPrimitive, longBoxed) as c3," +
                "zoneddate.between(longPrimitive, longBoxed) as c4 " +
                " from SupportDateTime unidirectional, SupportBean#lastevent";
            env.compileDeploy(eplCurrentTS).addListener("s0");

            SupportBean bean = new SupportBean();
            bean.setLongPrimitive(10);
            bean.setLongBoxed(20L);
            env.sendEventBean(bean);

            env.sendEventBean(SupportDateTime.make("2002-05-30T09:01:02.003"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{false, false, false, false, false});

            bean = new SupportBean();
            bean.setLongPrimitive(0);
            bean.setLongBoxed(Long.MAX_VALUE);
            env.sendEventBean(bean);

            env.sendEventBean(SupportDateTime.make("2002-05-30T09:01:02.003"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{true, true, true, true, true});

            env.undeployAll();
        }
    }

    private static class ExprDTBetweenIncludeEndpoints implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            String startTime = "2002-05-30T09:00:00.000";
            env.advanceTime(DateTime.parseDefaultMSec(startTime));

            String[] fieldsCurrentTs = "val0,val1,val2,val3,val4,val5,val6,val7,val8".split(",");
            String eplCurrentTS = "@name('s0') select " +
                "current_timestamp.after(longdateStart) as val0, " +
                "current_timestamp.between(longdateStart, longdateEnd) as val1, " +
                "current_timestamp.between(utildateStart, caldateEnd) as val2, " +
                "current_timestamp.between(caldateStart, utildateEnd) as val3, " +
                "current_timestamp.between(utildateStart, utildateEnd) as val4, " +
                "current_timestamp.between(caldateStart, caldateEnd) as val5, " +
                "current_timestamp.between(caldateEnd, caldateStart) as val6, " +
                "current_timestamp.between(ldtStart, ldtEnd) as val7, " +
                "current_timestamp.between(zdtStart, zdtEnd) as val8 " +
                "from SupportTimeStartEndA";
            env.compileDeploy(eplCurrentTS).addListener("s0");
            LambdaAssertionUtil.assertTypesAllSame(env.statement("s0").getEventType(), fieldsCurrentTs, Boolean.class);

            env.sendEventBean(SupportTimeStartEndA.make("E1", "2002-05-30T08:59:59.999", 0));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fieldsCurrentTs, new Object[]{true, false, false, false, false, false, false, false, false});

            env.sendEventBean(SupportTimeStartEndA.make("E1", "2002-05-30T08:59:59.999", 1));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fieldsCurrentTs, new Object[]{true, true, true, true, true, true, true, true, true});

            env.sendEventBean(SupportTimeStartEndA.make("E1", "2002-05-30T08:59:59.999", 100));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fieldsCurrentTs, new Object[]{true, true, true, true, true, true, true, true, true});

            env.sendEventBean(SupportTimeStartEndA.make("E1", "2002-05-30T09:00:00.000", 0));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fieldsCurrentTs, new Object[]{false, true, true, true, true, true, true, true, true});

            env.sendEventBean(SupportTimeStartEndA.make("E1", "2002-05-30T09:00:00.000", 100));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fieldsCurrentTs, new Object[]{false, true, true, true, true, true, true, true, true});

            env.sendEventBean(SupportTimeStartEndA.make("E1", "2002-05-30T09:00:00.001", 100));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fieldsCurrentTs, new Object[]{false, false, false, false, false, false, false, false, false});
            env.undeployAll();

            // test calendar field and constants
            String[] fieldsConstants = "val0,val1,val2,val3,val4,val5".split(",");
            String eplConstants = "@name('s0') select " +
                "longdateStart.between(DateTime.toCalendar('2002-05-30T09:00:00.000', \"yyyy-MM-dd'T'HH:mm:ss.SSS\"), DateTime.toCalendar('2002-05-30T09:01:00.000', \"yyyy-MM-dd'T'HH:mm:ss.SSS\")) as val0, " +
                "utildateStart.between(DateTime.toCalendar('2002-05-30T09:00:00.000', \"yyyy-MM-dd'T'HH:mm:ss.SSS\"), DateTime.toCalendar('2002-05-30T09:01:00.000', \"yyyy-MM-dd'T'HH:mm:ss.SSS\")) as val1, " +
                "caldateStart.between(DateTime.toCalendar('2002-05-30T09:00:00.000', \"yyyy-MM-dd'T'HH:mm:ss.SSS\"), DateTime.toCalendar('2002-05-30T09:01:00.000', \"yyyy-MM-dd'T'HH:mm:ss.SSS\")) as val2, " +
                "ldtStart.between(DateTime.toCalendar('2002-05-30T09:00:00.000', \"yyyy-MM-dd'T'HH:mm:ss.SSS\"), DateTime.toCalendar('2002-05-30T09:01:00.000', \"yyyy-MM-dd'T'HH:mm:ss.SSS\")) as val3, " +
                "zdtStart.between(DateTime.toCalendar('2002-05-30T09:00:00.000', \"yyyy-MM-dd'T'HH:mm:ss.SSS\"), DateTime.toCalendar('2002-05-30T09:01:00.000', \"yyyy-MM-dd'T'HH:mm:ss.SSS\")) as val4, " +
                "longdateStart.between(DateTime.toCalendar('2002-05-30T09:01:00.000', \"yyyy-MM-dd'T'HH:mm:ss.SSS\"), DateTime.toCalendar('2002-05-30T09:00:00.000', \"yyyy-MM-dd'T'HH:mm:ss.SSS\")) as val5 " +
                "from SupportTimeStartEndA";
            env.compileDeployAddListenerMile(eplConstants, "s0", 1);
            LambdaAssertionUtil.assertTypesAllSame(env.statement("s0").getEventType(), fieldsConstants, Boolean.class);

            env.sendEventBean(SupportTimeStartEndA.make("E1", "2002-05-30T08:59:59.999", 0));
            EPAssertionUtil.assertPropsAllValuesSame(env.listener("s0").assertOneGetNewAndReset(), fieldsConstants, false);

            env.sendEventBean(SupportTimeStartEndA.make("E2", "2002-05-30T09:00:00.000", 0));
            EPAssertionUtil.assertPropsAllValuesSame(env.listener("s0").assertOneGetNewAndReset(), fieldsConstants, true);

            env.sendEventBean(SupportTimeStartEndA.make("E2", "2002-05-30T09:00:05.000", 0));
            EPAssertionUtil.assertPropsAllValuesSame(env.listener("s0").assertOneGetNewAndReset(), fieldsConstants, true);

            env.sendEventBean(SupportTimeStartEndA.make("E2", "2002-05-30T09:00:59.999", 0));
            EPAssertionUtil.assertPropsAllValuesSame(env.listener("s0").assertOneGetNewAndReset(), fieldsConstants, true);

            env.sendEventBean(SupportTimeStartEndA.make("E2", "2002-05-30T09:01:00.000", 0));
            EPAssertionUtil.assertPropsAllValuesSame(env.listener("s0").assertOneGetNewAndReset(), fieldsConstants, true);

            env.sendEventBean(SupportTimeStartEndA.make("E2", "2002-05-30T09:01:00.001", 0));
            EPAssertionUtil.assertPropsAllValuesSame(env.listener("s0").assertOneGetNewAndReset(), fieldsConstants, false);

            env.undeployAll();
        }
    }

    private static class ExprDTBetweenExcludeEndpoints implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            AtomicInteger milestone = new AtomicInteger();
            String startTime = "2002-05-30T09:00:00.000";
            env.advanceTime(DateTime.parseDefaultMSec(startTime));

            RegressionPath path = new RegressionPath();
            env.compileDeploy("create variable boolean VAR_TRUE = true", path);
            env.compileDeploy("create variable boolean VAR_FALSE = false", path);

            tryAssertionExcludeEndpoints(env, path, "longdateStart, longdateEnd", milestone);
            tryAssertionExcludeEndpoints(env, path, "utildateStart, utildateEnd", milestone);
            tryAssertionExcludeEndpoints(env, path, "caldateStart, caldateEnd", milestone);
            tryAssertionExcludeEndpoints(env, path, "ldtStart, ldtEnd", milestone);
            tryAssertionExcludeEndpoints(env, path, "zdtStart, zdtEnd", milestone);

            env.undeployAll();
        }
    }

    private static void tryAssertionExcludeEndpoints(RegressionEnvironment env, RegressionPath path, String fields, AtomicInteger milestone) {

        String[] fieldsCurrentTs = "val0,val1,val2,val3,val4,val5,val6,val7".split(",");
        String eplCurrentTS = "@name('s0') select " +
            "current_timestamp.between(" + fields + ", true, true) as val0, " +
            "current_timestamp.between(" + fields + ", true, false) as val1, " +
            "current_timestamp.between(" + fields + ", false, true) as val2, " +
            "current_timestamp.between(" + fields + ", false, false) as val3, " +
            "current_timestamp.between(" + fields + ", VAR_TRUE, VAR_TRUE) as val4, " +
            "current_timestamp.between(" + fields + ", VAR_TRUE, VAR_FALSE) as val5, " +
            "current_timestamp.between(" + fields + ", VAR_FALSE, VAR_TRUE) as val6, " +
            "current_timestamp.between(" + fields + ", VAR_FALSE, VAR_FALSE) as val7 " +
            "from SupportTimeStartEndA";
        env.compileDeploy(eplCurrentTS, path).addListener("s0");
        LambdaAssertionUtil.assertTypesAllSame(env.statement("s0").getEventType(), fieldsCurrentTs, Boolean.class);

        env.sendEventBean(SupportTimeStartEndA.make("E1", "2002-05-30T08:59:59.999", 0));
        EPAssertionUtil.assertPropsAllValuesSame(env.listener("s0").assertOneGetNewAndReset(), fieldsCurrentTs, false);

        env.sendEventBean(SupportTimeStartEndA.make("E1", "2002-05-30T08:59:59.999", 1));
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fieldsCurrentTs, new Object[]{true, false, true, false, true, false, true, false});

        env.milestoneInc(milestone);

        env.sendEventBean(SupportTimeStartEndA.make("E1", "2002-05-30T08:59:59.999", 2));
        EPAssertionUtil.assertPropsAllValuesSame(env.listener("s0").assertOneGetNewAndReset(), fieldsCurrentTs, true);

        env.sendEventBean(SupportTimeStartEndA.make("E1", "2002-05-30T09:00:00.000", 1));
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fieldsCurrentTs, new Object[]{true, true, false, false, true, true, false, false});

        env.undeployModuleContaining("s0");

        // test calendar field and constants
        String[] fieldsConstants = "val0,val1,val2,val3".split(",");
        String eplConstants = "@name('s0') select " +
            "longdateStart.between(DateTime.toCalendar('2002-05-30T09:00:00.000', \"yyyy-MM-dd'T'HH:mm:ss.SSS\"), DateTime.toCalendar('2002-05-30T09:01:00.000', \"yyyy-MM-dd'T'HH:mm:ss.SSS\"), true, true) as val0, " +
            "longdateStart.between(DateTime.toCalendar('2002-05-30T09:00:00.000', \"yyyy-MM-dd'T'HH:mm:ss.SSS\"), DateTime.toCalendar('2002-05-30T09:01:00.000', \"yyyy-MM-dd'T'HH:mm:ss.SSS\"), true, false) as val1, " +
            "longdateStart.between(DateTime.toCalendar('2002-05-30T09:00:00.000', \"yyyy-MM-dd'T'HH:mm:ss.SSS\"), DateTime.toCalendar('2002-05-30T09:01:00.000', \"yyyy-MM-dd'T'HH:mm:ss.SSS\"), false, true) as val2, " +
            "longdateStart.between(DateTime.toCalendar('2002-05-30T09:00:00.000', \"yyyy-MM-dd'T'HH:mm:ss.SSS\"), DateTime.toCalendar('2002-05-30T09:01:00.000', \"yyyy-MM-dd'T'HH:mm:ss.SSS\"), false, false) as val3 " +
            "from SupportTimeStartEndA";
        env.compileDeploy(eplConstants).addListener("s0");
        LambdaAssertionUtil.assertTypesAllSame(env.statement("s0").getEventType(), fieldsConstants, Boolean.class);

        env.sendEventBean(SupportTimeStartEndA.make("E1", "2002-05-30T08:59:59.999", 0));
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fieldsConstants, new Object[]{false, false, false, false});

        env.sendEventBean(SupportTimeStartEndA.make("E2", "2002-05-30T09:00:00.000", 0));
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fieldsConstants, new Object[]{true, true, false, false});

        env.sendEventBean(SupportTimeStartEndA.make("E2", "2002-05-30T09:00:05.000", 0));
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fieldsConstants, new Object[]{true, true, true, true});

        env.milestoneInc(milestone);

        env.sendEventBean(SupportTimeStartEndA.make("E2", "2002-05-30T09:00:59.999", 0));
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fieldsConstants, new Object[]{true, true, true, true});

        env.sendEventBean(SupportTimeStartEndA.make("E2", "2002-05-30T09:01:00.000", 0));
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fieldsConstants, new Object[]{true, false, true, false});

        env.sendEventBean(SupportTimeStartEndA.make("E2", "2002-05-30T09:01:00.001", 0));
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fieldsConstants, new Object[]{false, false, false, false});

        env.undeployModuleContaining("s0");
    }
}

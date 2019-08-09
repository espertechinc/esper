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
package com.espertech.esper.regressionlib.suite.client.extension;

import com.espertech.esper.common.client.EPException;
import com.espertech.esper.common.client.hook.datetimemethod.*;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.internal.epl.methodbase.DotMethodFP;
import com.espertech.esper.common.internal.epl.methodbase.DotMethodFPInputEnum;
import com.espertech.esper.common.internal.epl.methodbase.DotMethodFPParam;
import com.espertech.esper.common.internal.epl.util.EPLExpressionParamType;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.support.bean.SupportDateTime;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;

import static com.espertech.esper.regressionlib.framework.SupportMessageAssertUtil.tryInvalidCompile;

public class ClientExtendDateTimeMethod {
    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new ClientExtendDateTimeMethodTransform());
        execs.add(new ClientExtendDateTimeMethodReformat());
        execs.add(new ClientExtendDateTimeMethodInvalid());
        return execs;
    }

    private static class ClientExtendDateTimeMethodInvalid implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            // validate factory returns no forge
            tryInvalidCompile(env, "select caldate.someDTMInvalidNoOp() from SupportDateTime",
                "Failed to validate select-clause expression 'caldate.someDTMInvalidNoOp()': Plug-in datetime method provider class");

            // validate pre-made argument test
            tryInvalidCompile(env, "select caldate.dtmInvalidMethodNotExists('x') from SupportDateTime",
                "Failed to validate select-clause expression 'caldate.dtmInvalidMethodNotExists('x')': Failed to resolve enumeration method, date-time method or mapped property 'caldate.dtmInvalidMethodNotExists('x')': Error validating date-time method 'dtmInvalidMethodNotExists', expected a Integer-type result for expression parameter 0 but received java.lang.String");

            // validate static method not matching
            tryInvalidCompile(env, "select localdate.dtmInvalidMethodNotExists(1) from SupportDateTime",
                "Failed to validate select-clause expression 'localdate.dtmInvalidMethodNotExists(1)': Failed to find static method for date-time method extension: Unknown method ArrayList.dtmInvalidMethod");

            // validate not provided
            tryInvalidCompile(env, "select caldate.dtmInvalidNotProvided() from SupportDateTime",
                "Failed to validate select-clause expression 'caldate.dtmInvalidNotProvided()': Plugin datetime method does not provide a forge for input type java.util.Calendar");
        }
    }

    private static class ClientExtendDateTimeMethodTransform implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select " +
                "caldate.roll('date', true) as c0, " +
                "longdate.roll('date', true) as c1, " +
                "utildate.roll('date', true) as c2, " +
                "localdate.roll('date', true) as c3, " +
                "zoneddate.roll('date', true) as c4 " +
                " from SupportDateTime";
            env.compileDeploy(epl).addListener("s0");

            SupportDateTime event = SupportDateTime.make("2002-05-30T09:01:02.000");
            Calendar calExpected = (Calendar) event.getCaldate().clone();
            calExpected.roll(Calendar.DATE, true);

            env.sendEventBean(event);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "c0,c1,c2,c3,c4".split(","),
                new Object[]{calExpected, calExpected.getTimeInMillis(), calExpected.getTime(), event.getLocaldate().plusDays(1), event.getZoneddate().plusDays(1)});

            env.undeployAll();
        }
    }

    private static class ClientExtendDateTimeMethodReformat implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select " +
                "caldate.asArrayOfString() as c0, " +
                "longdate.asArrayOfString() as c1, " +
                "utildate.asArrayOfString() as c2, " +
                "localdate.asArrayOfString() as c3, " +
                "zoneddate.asArrayOfString() as c4 " +
                " from SupportDateTime";
            env.compileDeploy(epl).addListener("s0");

            SupportDateTime event = SupportDateTime.make("2002-05-30T09:01:02.000");

            env.sendEventBean(event);
            String[] expected = "30,5,2002".split(",");
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "c0,c1,c2,c3,c4".split(","),
                new Object[]{expected, expected, expected, expected, expected});

            env.undeployAll();
        }
    }

    public static class MyLocalDTMForgeFactoryRoll implements DateTimeMethodForgeFactory {
        private final static DotMethodFP[] FOOTPRINTS = new DotMethodFP[]{
            new DotMethodFP(DotMethodFPInputEnum.SCALAR_ANY,
                new DotMethodFPParam("an string-type calendar field name", EPLExpressionParamType.SPECIFIC, String.class),
                new DotMethodFPParam("a boolean-type roll indicator", EPLExpressionParamType.SPECIFIC, boolean.class))
        };

        public DateTimeMethodDescriptor initialize(DateTimeMethodInitializeContext context) {
            return new DateTimeMethodDescriptor(FOOTPRINTS);
        }

        public DateTimeMethodOps validate(DateTimeMethodValidateContext context) {
            DateTimeMethodOpsModify roll = new DateTimeMethodOpsModify();
            roll.setCalendarOp(new DateTimeMethodModeStaticMethod(MyLocalDTMRollUtility.class, "rollOne"));
            roll.setLdtOp(new DateTimeMethodModeStaticMethod(MyLocalDTMRollUtility.class, "rollTwo"));
            roll.setZdtOp(new DateTimeMethodModeStaticMethod(MyLocalDTMRollUtility.class, "rollThree"));
            return roll;
        }
    }

    public static class MyLocalDTMRollUtility {
        public static void rollOne(Calendar calendar, String fieldName, boolean flagValue) {
            switch (fieldName) {
                case "date":
                    calendar.roll(Calendar.DATE, flagValue);
                    break;
                default:
                    throw new EPException("Invalid field name '" + fieldName + "'");
            }
        }

        public static LocalDateTime rollTwo(LocalDateTime ldt, String fieldName, boolean flagValue) {
            switch (fieldName) {
                case "date":
                    return ldt.plusDays(1);
                default:
                    throw new EPException("Invalid field name '" + fieldName + "'");
            }
        }

        public static ZonedDateTime rollThree(ZonedDateTime zdt, String fieldName, boolean flagValue) {
            switch (fieldName) {
                case "date":
                    return zdt.plusDays(1);
                default:
                    throw new EPException("Invalid field name '" + fieldName + "'");
            }
        }
    }

    public static class MyLocalDTMForgeFactoryArrayOfString implements DateTimeMethodForgeFactory {
        private final static DotMethodFP[] FOOTPRINTS = new DotMethodFP[]{
            new DotMethodFP(DotMethodFPInputEnum.SCALAR_ANY)
        };

        public DateTimeMethodDescriptor initialize(DateTimeMethodInitializeContext context) {
            return new DateTimeMethodDescriptor(FOOTPRINTS);
        }

        public DateTimeMethodOps validate(DateTimeMethodValidateContext context) {
            DateTimeMethodOpsReformat asArrayOfString = new DateTimeMethodOpsReformat();
            asArrayOfString.setReturnType(String[].class);
            asArrayOfString.setLongOp(new DateTimeMethodModeStaticMethod(MyLocalDTMArrayOfStringUtility.class, "asArrayOfStringOne"));
            asArrayOfString.setDateOp(new DateTimeMethodModeStaticMethod(MyLocalDTMArrayOfStringUtility.class, "asArrayOfStringTwo"));
            asArrayOfString.setCalendarOp(new DateTimeMethodModeStaticMethod(MyLocalDTMArrayOfStringUtility.class, "asArrayOfStringThree"));
            asArrayOfString.setLdtOp(new DateTimeMethodModeStaticMethod(MyLocalDTMArrayOfStringUtility.class, "asArrayOfStringFour"));
            asArrayOfString.setZdtOp(new DateTimeMethodModeStaticMethod(MyLocalDTMArrayOfStringUtility.class, "asArrayOfStringFive"));
            return asArrayOfString;
        }
    }

    public static class MyLocalDTMArrayOfStringUtility {
        public static String[] asArrayOfStringOne(long date) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(date);
            return asArrayOfStringThree(calendar);
        }

        public static String[] asArrayOfStringTwo(Date date) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(date);
            return asArrayOfStringThree(calendar);
        }

        public static String[] asArrayOfStringThree(Calendar calendar) {
            return new String[]{Integer.toString(calendar.get(Calendar.DAY_OF_MONTH)),
                Integer.toString(calendar.get(Calendar.MONTH) + 1),
                Integer.toString(calendar.get(Calendar.YEAR))};
        }

        public static String[] asArrayOfStringFour(LocalDateTime ldt) {
            return new String[]{Integer.toString(ldt.getDayOfMonth()),
                Integer.toString(ldt.getMonthValue()),
                Integer.toString(ldt.getYear())};
        }

        public static String[] asArrayOfStringFive(ZonedDateTime zdt) {
            return new String[]{Integer.toString(zdt.getDayOfMonth()),
                Integer.toString(zdt.getMonthValue()),
                Integer.toString(zdt.getYear())};
        }
    }

    public static class MyLocalDTMForgeFactoryInvalidMethodNotExists implements DateTimeMethodForgeFactory {
        private final static DotMethodFP[] FOOTPRINTS = new DotMethodFP[]{
            new DotMethodFP(DotMethodFPInputEnum.SCALAR_ANY,
                new DotMethodFPParam("an int-type dummy", EPLExpressionParamType.SPECIFIC, Integer.class))
        };

        public DateTimeMethodDescriptor initialize(DateTimeMethodInitializeContext context) {
            return new DateTimeMethodDescriptor(FOOTPRINTS);
        }

        public DateTimeMethodOps validate(DateTimeMethodValidateContext context) {
            DateTimeMethodOpsModify valueChange = new DateTimeMethodOpsModify();
            valueChange.setLdtOp(new DateTimeMethodModeStaticMethod(ArrayList.class, "dtmInvalidMethod"));
            return valueChange;
        }
    }

    public static class MyLocalDTMForgeFactoryInvalidNotProvided implements DateTimeMethodForgeFactory {
        private final static DotMethodFP[] FOOTPRINTS = new DotMethodFP[]{
            new DotMethodFP(DotMethodFPInputEnum.SCALAR_ANY)
        };

        public DateTimeMethodDescriptor initialize(DateTimeMethodInitializeContext context) {
            return new DateTimeMethodDescriptor(FOOTPRINTS);
        }

        public DateTimeMethodOps validate(DateTimeMethodValidateContext context) {
            return new DateTimeMethodOpsModify();
        }
    }

    public static class MyLocalDTMForgeFactoryInvalidReformat implements DateTimeMethodForgeFactory {
        private final static DotMethodFP[] FOOTPRINTS = new DotMethodFP[]{
            new DotMethodFP(DotMethodFPInputEnum.SCALAR_ANY)
        };

        public DateTimeMethodDescriptor initialize(DateTimeMethodInitializeContext context) {
            return new DateTimeMethodDescriptor(FOOTPRINTS);
        }

        public DateTimeMethodOps validate(DateTimeMethodValidateContext context) {
            return null;
        }
    }

    public static class MyLocalDTMForgeFactoryInvalidNoOp implements DateTimeMethodForgeFactory {
        private final static DotMethodFP[] FOOTPRINTS = new DotMethodFP[]{
            new DotMethodFP(DotMethodFPInputEnum.SCALAR_ANY)
        };

        public DateTimeMethodDescriptor initialize(DateTimeMethodInitializeContext context) {
            return new DateTimeMethodDescriptor(FOOTPRINTS);
        }

        public DateTimeMethodOps validate(DateTimeMethodValidateContext context) {
            return null;
        }
    }
}

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

import com.espertech.esper.common.client.EPCompiled;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.client.util.DateTime;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.regressionlib.support.bean.SupportDateTime;
import com.espertech.esper.regressionlib.support.bean.SupportStartTSEndTSImpl;
import com.espertech.esper.regressionlib.support.bean.SupportStartTSEndTSInterface;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;

import static com.espertech.esper.regressionlib.framework.SupportMessageAssertUtil.tryInvalidCompile;
import static java.time.DayOfWeek.THURSDAY;
import static org.junit.Assert.assertEquals;

public class ExprDTDataSources {

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> executions = new ArrayList<>();
        executions.add(new ExprDTDataSourcesStartEndTS());
        executions.add(new ExprDTDataSourcesFieldWValue());
        executions.add(new ExprDTDataSourcesAllCombinations());
        executions.add(new ExprDTDataSourcesMinMax());
        return executions;
    }

    private static class ExprDTDataSourcesMinMax implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select " +
                "min(longPrimitive).before(max(longBoxed), 1 second) as c0," +
                "min(longPrimitive, longBoxed).before(20000L, 1 second) as c1" +
                " from SupportBean#length(2)";
            env.compileDeploy(epl).addListener("s0");
            String[] fields = "c0,c1".split(",");

            sendBean(env, 20000, 20000);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{false, false});

            sendBean(env, 19000, 20000);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{true, true});

            env.undeployAll();
        }
    }

    private static class ExprDTDataSourcesAllCombinations implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "utildate,longdate,caldate,zoneddate,localdate".split(",");
            AtomicInteger milestone = new AtomicInteger();

            for (String field : fields) {
                runAssertionAllCombinations(env, field, milestone);
            }
        }
    }

    private static class ExprDTDataSourcesFieldWValue implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String startTime = "2002-05-30T09:01:02.003";   // use 2-digit hour, see https://bugs.openjdk.java.net/browse/JDK-8066806
            env.advanceTime(DateTime.parseDefaultMSec(startTime));

            String[] fields = "valmoh,valmoy,valdom,valdow,valdoy,valera,valhod,valmos,valsom,valwye,valyea,val1,val2,val3,val4,val5".split(",");
            String eplFragment = "@name('s0') select " +
                "current_timestamp.getMinuteOfHour() as valmoh," +
                "current_timestamp.getMonthOfYear() as valmoy," +
                "current_timestamp.getDayOfMonth() as valdom," +
                "current_timestamp.getDayOfWeek() as valdow," +
                "current_timestamp.getDayOfYear() as valdoy," +
                "current_timestamp.getEra() as valera," +
                "current_timestamp.gethourOfDay() as valhod," +
                "current_timestamp.getmillisOfSecond()  as valmos," +
                "current_timestamp.getsecondOfMinute() as valsom," +
                "current_timestamp.getweekyear() as valwye," +
                "current_timestamp.getyear() as valyea," +
                "utildate.gethourOfDay() as val1," +
                "longdate.gethourOfDay() as val2," +
                "caldate.gethourOfDay() as val3," +
                "zoneddate.gethourOfDay() as val4," +
                "localdate.gethourOfDay() as val5" +
                " from SupportDateTime";
            env.compileDeploy(eplFragment).addListener("s0");
            for (String field : fields) {
                assertEquals(Integer.class, env.statement("s0").getEventType().getPropertyType(field));
            }

            env.sendEventBean(SupportDateTime.make(startTime));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{
                1, 4, 30, 5, 150, 1, 9, 3, 2, 22, 2002, 9, 9, 9, 9, 9
            });

            env.undeployAll();
        }
    }

    private static class ExprDTDataSourcesStartEndTS implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();

            // test Map inheritance via create-schema
            String eplMap = "create schema ParentType as (startTS long, endTS long) starttimestamp startTS endtimestamp endTS;\n" +
                "create schema ChildType as (foo string) inherits ParentType;\n";
            env.compileDeployWBusPublicType(eplMap, path);

            env.compileDeploy("@name('s0') select * from ChildType dt where dt.before(current_timestamp())", path);
            assertEquals("startTS", env.statement("s0").getEventType().getStartTimestampPropertyName());
            assertEquals("endTS", env.statement("s0").getEventType().getEndTimestampPropertyName());

            env.undeployAll();

            // test Object-array inheritance via create-schema
            path.clear();
            String eplObjectArray = "create objectarray schema ParentType as (startTS long, endTS long) starttimestamp startTS endtimestamp endTS;\n" +
                "create objectarray schema ChildType as (foo string) inherits ParentType;\n";
            env.compileDeployWBusPublicType(eplObjectArray, path);

            env.compileDeploy("@name('s0') select * from ChildType dt where dt.before(current_timestamp())", path);
            assertEquals("startTS", env.statement("s0").getEventType().getStartTimestampPropertyName());
            assertEquals("endTS", env.statement("s0").getEventType().getEndTimestampPropertyName());

            env.undeployAll();

            // test POJO inheritance via create-schema
            path.clear();
            String eplPOJO = "create schema InterfaceType as " + SupportStartTSEndTSInterface.class.getName() + " starttimestamp startTS endtimestamp endTS;\n" +
                "create schema DerivedType as " + SupportStartTSEndTSImpl.class.getName() + " inherits InterfaceType";
            env.compileDeployWBusPublicType(eplPOJO, path);

            EPCompiled compiled = env.compile("@name('s2') select * from DerivedType dt where dt.before(current_timestamp())", path);
            env.deploy(compiled);
            assertEquals("startTS", env.statement("s2").getEventType().getStartTimestampPropertyName());
            assertEquals("endTS", env.statement("s2").getEventType().getEndTimestampPropertyName());

            env.undeployAll();

            // test POJO inheritance via create-schema
            path.clear();
            String eplXML = "@XMLSchema(rootElementName='root', schemaText='') " +
                "@XMLSchemaField(name='startTS', xpath='/abc', type='string', castToType='long')" +
                "@XMLSchemaField(name='endTS', xpath='/def', type='string', castToType='long')" +
                "create xml schema MyXMLEvent() starttimestamp startTS endtimestamp endTS;\n";
            env.compileDeployWBusPublicType(eplXML, path);

            compiled = env.compile("@name('s2') select * from MyXMLEvent dt where dt.before(current_timestamp())", path);
            env.deploy(compiled);
            assertEquals("startTS", env.statement("s2").getEventType().getStartTimestampPropertyName());
            assertEquals("endTS", env.statement("s2").getEventType().getEndTimestampPropertyName());

            env.undeployAll();

            // test incompatible
            path.clear();
            String eplT1T2 = "create schema T1 as (startTS long, endTS long) starttimestamp startTS endtimestamp endTS;\n" +
                "create schema T2 as (startTSOne long, endTSOne long) starttimestamp startTSOne endtimestamp endTSOne;\n";
            env.compileDeployWBusPublicType(eplT1T2, path);

            tryInvalidCompile(env, path, "create schema T12 as () inherits T1,T2",
                "Event type declares start timestamp as property 'startTS' however inherited event type 'T2' declares start timestamp as property 'startTSOne'");
            tryInvalidCompile(env, path, "create schema T12 as (startTSOne long, endTSXXX long) inherits T2 starttimestamp startTSOne endtimestamp endTSXXX",
                "Event type declares end timestamp as property 'endTSXXX' however inherited event type 'T2' declares end timestamp as property 'endTSOne'");

            env.undeployAll();
        }
    }

    private static void runAssertionAllCombinations(RegressionEnvironment env, String field, AtomicInteger milestone) {
        String[] methods = "getMinuteOfHour,getMonthOfYear,getDayOfMonth,getDayOfWeek,getDayOfYear,getEra,gethourOfDay,getmillisOfSecond,getsecondOfMinute,getweekyear,getyear".split(",");
        StringWriter epl = new StringWriter();
        epl.append("@name('s0') select ");
        int count = 0;
        String delimiter = "";
        for (String method : methods) {
            epl.append(delimiter).append(field).append(".").append(method).append("() ").append("c").append(Integer.toString(count++));
            delimiter = ",";
        }
        epl.append(" from SupportDateTime");

        env.compileDeployAddListenerMile(epl.toString(), "s0", milestone.getAndIncrement());

        SupportDateTime sdt = SupportDateTime.make("2002-05-30T09:01:02.003");
        sdt.getCaldate().set(Calendar.MILLISECOND, 3);
        env.sendEventBean(sdt);

        boolean java8date = field.equals("zoneddate") || field.equals("localdate");
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "c0,c1,c2,c3,c4,c5,c6,c7,c8,c9,c10".split(","), new Object[]{
            1, java8date ? 5 : 4, 30, java8date ? THURSDAY : 5, 150, 1, 9, 3, 2, 22, 2002
        });

        env.undeployAll();
    }

    private static void sendBean(RegressionEnvironment env, long longPrimitive, long longBoxed) {
        SupportBean bean = new SupportBean();
        bean.setLongPrimitive(longPrimitive);
        bean.setLongBoxed(longBoxed);
        env.sendEventBean(bean);
    }

}


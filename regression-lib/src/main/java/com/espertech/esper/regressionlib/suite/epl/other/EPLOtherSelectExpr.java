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
package com.espertech.esper.regressionlib.suite.epl.other;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.annotation.Description;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionlib.support.bean.SupportBeanComplexProps;
import com.espertech.esper.regressionlib.support.bean.SupportBeanKeywords;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class EPLOtherSelectExpr {
    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new EPLOtherPrecedenceNoColumnName());
        execs.add(new EPLOtherGraphSelect());
        execs.add(new EPLOtherKeywordsAllowed());
        execs.add(new EPLOtherEscapeString());
        execs.add(new EPLOtherGetEventType());
        execs.add(new EPLOtherWindowStats());
        return execs;
    }

    private static class EPLOtherPrecedenceNoColumnName implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            tryPrecedenceNoColumnName(env, "3*2+1", "3*2+1", 7);
            tryPrecedenceNoColumnName(env, "(3*2)+1", "3*2+1", 7);
            tryPrecedenceNoColumnName(env, "3*(2+1)", "3*(2+1)", 9);
        }

        private static void tryPrecedenceNoColumnName(RegressionEnvironment env, String selectColumn, String expectedColumn, Object value) {
            String epl = "@name('s0') select " + selectColumn + " from SupportBean";
            env.compileDeploy(epl).addListener("s0");
            if (!env.statement("s0").getEventType().getPropertyNames()[0].equals(expectedColumn)) {
                fail("Expected '" + expectedColumn + "' but was " + env.statement("s0").getEventType().getPropertyNames()[0]);
            }

            env.sendEventBean(new SupportBean("E1", 1));
            EventBean event = env.listener("s0").assertOneGetNewAndReset();
            Assert.assertEquals(value, event.get(expectedColumn));
            env.undeployAll();
        }
    }

    private static class EPLOtherGraphSelect implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("insert into MyStream select nested from SupportBeanComplexProps", path);
            String epl = "@name('s0') select nested.nestedValue, nested.nestedNested.nestedNestedValue from MyStream";
            env.compileDeploy(epl, path).addListener("s0");

            env.sendEventBean(SupportBeanComplexProps.makeDefaultBean());
            assertNotNull(env.listener("s0").assertOneGetNewAndReset());

            env.undeployAll();
        }
    }

    private static class EPLOtherKeywordsAllowed implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String fields = "count,escape,every,sum,avg,max,min,coalesce,median,stddev,avedev,events,first,last,unidirectional,pattern,sql,metadatasql,prev,prior,weekday,lastweekday,cast,snapshot,variable,window,left,right,full,outer,join";
            env.compileDeploy("@name('s0') select " + fields + " from SupportBeanKeywords").addListener("s0");

            env.sendEventBean(new SupportBeanKeywords());
            EPAssertionUtil.assertEqualsExactOrder(env.statement("s0").getEventType().getPropertyNames(), fields.split(","));

            EventBean theEvent = env.listener("s0").assertOneGetNewAndReset();

            String[] fieldsArr = fields.split(",");
            for (String aFieldsArr : fieldsArr) {
                Assert.assertEquals(1, theEvent.get(aFieldsArr));
            }
            env.undeployAll();

            env.compileDeploy("@name('s0') select escape as stddev, count(*) as count, last from SupportBeanKeywords");
            env.addListener("s0");
            env.sendEventBean(new SupportBeanKeywords());

            theEvent = env.listener("s0").assertOneGetNewAndReset();
            Assert.assertEquals(1, theEvent.get("stddev"));
            Assert.assertEquals(1L, theEvent.get("count"));
            Assert.assertEquals(1, theEvent.get("last"));

            env.undeployAll();
        }
    }

    private static class EPLOtherEscapeString implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            // The following EPL syntax compiles but fails to match a string "A'B", we are looking into:
            // env.compileDeploy("@name('s0') select * from SupportBean(string='A\\\'B')");

            tryEscapeMatch(env, "A'B", "\"A'B\"");       // opposite quotes
            tryEscapeMatch(env, "A'B", "'A\\'B'");      // escape '
            tryEscapeMatch(env, "A'B", "'A\\u0027B'");   // unicode

            tryEscapeMatch(env, "A\"B", "'A\"B'");       // opposite quotes
            tryEscapeMatch(env, "A\"B", "'A\\\"B'");      // escape "
            tryEscapeMatch(env, "A\"B", "'A\\u0022B'");   // unicode

            env.compileDeploy("@Name('A\\\'B') @Description(\"A\\\"B\") select * from SupportBean");
            Assert.assertEquals("A\'B", env.statement("A\'B").getName());
            Description desc = (Description) env.statement("A\'B").getAnnotations()[1];
            Assert.assertEquals("A\"B", desc.value());
            env.undeployAll();

            env.compileDeploy("@name('s0') select 'volume' as field1, \"sleep\" as field2, \"\\u0041\" as unicodeA from SupportBean");
            env.addListener("s0");

            env.sendEventBean(new SupportBean());
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), new String[]{"field1", "field2", "unicodeA"}, new Object[]{"volume", "sleep", "A"});
            env.undeployAll();

            tryStatementMatch(env, "John's", "select * from SupportBean(theString='John\\'s')");
            tryStatementMatch(env, "John's", "select * from SupportBean(theString='John\\u0027s')");
            tryStatementMatch(env, "Quote \"Hello\"", "select * from SupportBean(theString like \"Quote \\\"Hello\\\"\")");
            tryStatementMatch(env, "Quote \"Hello\"", "select * from SupportBean(theString like \"Quote \\u0022Hello\\u0022\")");

            env.undeployAll();
        }

        private static void tryEscapeMatch(RegressionEnvironment env, String property, String escaped) {
            String epl = "@name('s0') select * from SupportBean(theString=" + escaped + ")";
            String text = "trying >" + escaped + "< (" + escaped.length() + " chars) EPL " + epl;
            log.info("tryEscapeMatch for " + text);
            env.compileDeploy(epl).addListener("s0");
            env.sendEventBean(new SupportBean(property, 1));
            Assert.assertEquals(env.listener("s0").assertOneGetNewAndReset().get("intPrimitive"), 1);
            env.undeployAll();
        }

        private static void tryStatementMatch(RegressionEnvironment env, String property, String epl) {
            String text = "trying EPL " + epl;
            log.info("tryEscapeMatch for " + text);
            env.compileDeploy("@name('s0') " + epl).addListener("s0");
            env.sendEventBean(new SupportBean(property, 1));
            Assert.assertEquals(env.listener("s0").assertOneGetNewAndReset().get("intPrimitive"), 1);
            env.undeployAll();
        }
    }

    private static class EPLOtherGetEventType implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select theString, boolBoxed aBool, 3*intPrimitive, floatBoxed+floatPrimitive result" +
                " from SupportBean#length(3) " +
                " where boolBoxed = true";
            env.compileDeploy(epl).addListener("s0");

            EventType type = env.statement("s0").getEventType();
            log.debug(".testGetEventType properties=" + Arrays.toString(type.getPropertyNames()));
            EPAssertionUtil.assertEqualsAnyOrder(type.getPropertyNames(), new String[]{"3*intPrimitive", "theString", "result", "aBool"});
            Assert.assertEquals(String.class, type.getPropertyType("theString"));
            Assert.assertEquals(Boolean.class, type.getPropertyType("aBool"));
            Assert.assertEquals(Float.class, type.getPropertyType("result"));
            Assert.assertEquals(Integer.class, type.getPropertyType("3*intPrimitive"));

            env.undeployAll();
        }
    }

    private static class EPLOtherWindowStats implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select theString, boolBoxed as aBool, 3*intPrimitive, floatBoxed+floatPrimitive as result" +
                " from SupportBean#length(3) " +
                " where boolBoxed = true";
            env.compileDeploy(epl).addListener("s0");

            sendEvent(env, "a", false, 0, 0, 0);
            sendEvent(env, "b", false, 0, 0, 0);
            assertTrue(env.listener("s0").getLastNewData() == null);
            sendEvent(env, "c", true, 3, 10, 20);

            EventBean received = env.listener("s0").getAndResetLastNewData()[0];
            Assert.assertEquals("c", received.get("theString"));
            Assert.assertEquals(true, received.get("aBool"));
            Assert.assertEquals(30f, received.get("result"));

            env.undeployAll();
        }
    }

    private static void sendEvent(RegressionEnvironment env, String s, boolean b, int i, float f1, float f2) {
        SupportBean bean = new SupportBean();
        bean.setTheString(s);
        bean.setBoolBoxed(b);
        bean.setIntPrimitive(i);
        bean.setFloatPrimitive(f1);
        bean.setFloatBoxed(f2);
        env.sendEventBean(bean);
    }

    private static final Logger log = LoggerFactory.getLogger(EPLOtherSelectExpr.class);
}

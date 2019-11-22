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

import com.espertech.esper.common.client.soda.*;
import com.espertech.esper.common.internal.util.SerializableObjectCopier;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.runtime.client.scopetest.SupportListener;
import junit.framework.TestCase;
import org.junit.Assert;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class EPLOtherIStreamRStreamKeywords {
    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new EPLOtherRStreamOnlyOM());
        execs.add(new EPLOtherRStreamOnlyCompile());
        execs.add(new EPLOtherRStreamOnly());
        execs.add(new EPLOtherRStreamInsertInto());
        execs.add(new EPLOtherRStreamInsertIntoRStream());
        execs.add(new EPLOtherRStreamJoin());
        execs.add(new EPLOtherIStreamOnly());
        execs.add(new EPLOtherIStreamInsertIntoRStream());
        execs.add(new EPLOtherIStreamJoin());
        execs.add(new EPLOtherRStreamOutputSnapshot());
        return execs;
    }

    private static class EPLOtherRStreamOutputSnapshot implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "select rstream * from SupportBean#time(30 minutes) output snapshot";
            env.compileDeploy(epl).undeployAll();
        }
    }

    private static class EPLOtherRStreamOnlyOM implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String stmtText = "select rstream * from SupportBean#length(3)";
            EPStatementObjectModel model = new EPStatementObjectModel();
            model.setSelectClause(SelectClause.createWildcard(StreamSelector.RSTREAM_ONLY));
            FromClause fromClause = FromClause.create(FilterStream.create("SupportBean").addView(View.create("length", Expressions.constant(3))));
            model.setFromClause(fromClause);
            model = SerializableObjectCopier.copyMayFail(model);

            Assert.assertEquals(stmtText, model.toEPL());
            model.setAnnotations(Collections.singletonList(AnnotationPart.nameAnnotation("s0")));
            env.compileDeploy(model).addListener("s0");

            Object theEvent = sendEvent(env, "a", 2);
            TestCase.assertFalse(env.listener("s0").isInvoked());

            sendEvents(env, new String[]{"a", "b"});
            TestCase.assertFalse(env.listener("s0").isInvoked());

            sendEvent(env, "d", 2);
            TestCase.assertSame(theEvent, env.listener("s0").getLastNewData()[0].getUnderlying());    // receive 'a' as new data
            TestCase.assertNull(env.listener("s0").getLastOldData());  // receive no more old data

            env.undeployAll();
        }
    }

    private static class EPLOtherRStreamOnlyCompile implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String stmtText = "select rstream * from SupportBean#length(3)";
            EPStatementObjectModel model = env.eplToModel(stmtText);
            model = SerializableObjectCopier.copyMayFail(model);

            Assert.assertEquals(stmtText, model.toEPL());
            model.setAnnotations(Collections.singletonList(AnnotationPart.nameAnnotation("s0")));
            env.compileDeploy(model).addListener("s0");

            Object theEvent = sendEvent(env, "a", 2);
            TestCase.assertFalse(env.listener("s0").isInvoked());

            sendEvents(env, new String[]{"a", "b"});
            TestCase.assertFalse(env.listener("s0").isInvoked());

            sendEvent(env, "d", 2);
            TestCase.assertSame(theEvent, env.listener("s0").getLastNewData()[0].getUnderlying());    // receive 'a' as new data
            TestCase.assertNull(env.listener("s0").getLastOldData());  // receive no more old data

            env.undeployAll();
        }
    }

    private static class EPLOtherRStreamOnly implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            env.compileDeploy("@name('s0') select rstream * from SupportBean#length(3)").addListener("s0");

            Object theEvent = sendEvent(env, "a", 2);
            TestCase.assertFalse(env.listener("s0").isInvoked());

            sendEvents(env, new String[]{"a", "b"});
            TestCase.assertFalse(env.listener("s0").isInvoked());

            sendEvent(env, "d", 2);
            TestCase.assertSame(theEvent, env.listener("s0").getLastNewData()[0].getUnderlying());    // receive 'a' as new data
            TestCase.assertNull(env.listener("s0").getLastOldData());  // receive no more old data

            env.undeployAll();
        }
    }

    private static class EPLOtherRStreamInsertInto implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("@name('s0') insert into NextStream " +
                "select rstream s0.theString as theString from SupportBean#length(3) as s0", path);
            env.addListener("s0");
            env.compileDeploy("@name('ii') select * from NextStream", path).addListener("ii");

            sendEvent(env, "a", 2);
            TestCase.assertFalse(env.listener("s0").isInvoked());
            Assert.assertEquals("a", env.listener("ii").assertOneGetNewAndReset().get("theString"));    // insert into unchanged

            sendEvents(env, new String[]{"b", "c"});
            TestCase.assertFalse(env.listener("s0").isInvoked());
            Assert.assertEquals(2, env.listener("ii").getNewDataList().size());    // insert into unchanged
            env.listener("ii").reset();

            sendEvent(env, "d", 2);
            TestCase.assertSame("a", env.listener("s0").getLastNewData()[0].get("theString"));    // receive 'a' as new data
            TestCase.assertNull(env.listener("s0").getLastOldData());  // receive no more old data
            Assert.assertEquals("d", env.listener("ii").getLastNewData()[0].get("theString"));    // insert into unchanged
            TestCase.assertNull(env.listener("ii").getLastOldData());  // receive no old data in insert into

            env.undeployAll();
        }
    }

    private static class EPLOtherRStreamInsertIntoRStream implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("@name('s0') insert rstream into NextStream " +
                "select rstream s0.theString as theString from SupportBean#length(3) as s0", path);
            env.addListener("s0");

            env.compileDeploy("@name('ii') select * from NextStream", path).addListener("ii");

            sendEvent(env, "a", 2);
            TestCase.assertFalse(env.listener("s0").isInvoked());
            TestCase.assertFalse(env.listener("ii").isInvoked());

            sendEvents(env, new String[]{"b", "c"});
            TestCase.assertFalse(env.listener("s0").isInvoked());
            TestCase.assertFalse(env.listener("ii").isInvoked());

            sendEvent(env, "d", 2);
            TestCase.assertSame("a", env.listener("s0").getLastNewData()[0].get("theString"));    // receive 'a' as new data
            TestCase.assertNull(env.listener("s0").getLastOldData());  // receive no more old data
            Assert.assertEquals("a", env.listener("ii").getLastNewData()[0].get("theString"));    // insert into unchanged
            TestCase.assertNull(env.listener("s0").getLastOldData());  // receive no old data in insert into

            env.undeployAll();
        }
    }

    private static class EPLOtherRStreamJoin implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            env.compileDeploy("@name('s0') select rstream s1.intPrimitive as aID, s2.intPrimitive as bID " +
                "from SupportBean(theString='a')#length(2) as s1, "
                + "SupportBean(theString='b')#keepall as s2" +
                " where s1.intPrimitive = s2.intPrimitive").addListener("s0");

            sendEvent(env, "a", 1);
            sendEvent(env, "b", 1);
            TestCase.assertFalse(env.listener("s0").isInvoked());

            sendEvent(env, "a", 2);
            TestCase.assertFalse(env.listener("s0").isInvoked());

            sendEvent(env, "a", 3);
            Assert.assertEquals(1, env.listener("s0").getLastNewData()[0].get("aID"));    // receive 'a' as new data
            Assert.assertEquals(1, env.listener("s0").getLastNewData()[0].get("bID"));
            TestCase.assertNull(env.listener("s0").getLastOldData());  // receive no more old data

            env.undeployAll();
        }
    }

    private static class EPLOtherIStreamOnly implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            env.compileDeploy("@name('s0') select istream * from SupportBean#length(1)").addListener("s0");

            Object theEvent = sendEvent(env, "a", 2);
            TestCase.assertSame(theEvent, env.listener("s0").assertOneGetNewAndReset().getUnderlying());

            theEvent = sendEvent(env, "b", 2);
            TestCase.assertSame(theEvent, env.listener("s0").getLastNewData()[0].getUnderlying());
            TestCase.assertNull(env.listener("s0").getLastOldData()); // receive no old data, just istream events

            env.undeployAll();
        }
    }

    private static class EPLOtherIStreamInsertIntoRStream implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("@name('s0') insert rstream into NextStream " +
                "select istream a.theString as theString from SupportBean#length(1) as a", path);
            env.addListener("s0");

            env.compileDeploy("@name('ii') select * from NextStream", path).addListener("ii");

            sendEvent(env, "a", 2);
            Assert.assertEquals("a", env.listener("s0").assertOneGetNewAndReset().get("theString"));
            TestCase.assertFalse(env.listener("ii").isInvoked());

            sendEvent(env, "b", 2);
            SupportListener listener = env.listener("s0");
            Assert.assertEquals("b", env.listener("s0").getLastNewData()[0].get("theString"));
            TestCase.assertNull(env.listener("s0").getLastOldData());
            Assert.assertEquals("a", env.listener("ii").getLastNewData()[0].get("theString"));
            TestCase.assertNull(env.listener("ii").getLastOldData());

            env.undeployAll();
        }
    }

    private static class EPLOtherIStreamJoin implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            env.compileDeploy("@name('s0') " +
                "select istream s1.intPrimitive as aID, s2.intPrimitive as bID " +
                "from SupportBean(theString='a')#length(2) as s1, "
                + "SupportBean(theString='b')#keepall as s2" +
                " where s1.intPrimitive = s2.intPrimitive").addListener("s0");

            sendEvent(env, "a", 1);
            sendEvent(env, "b", 1);
            Assert.assertEquals(1, env.listener("s0").getLastNewData()[0].get("aID"));    // receive 'a' as new data
            Assert.assertEquals(1, env.listener("s0").getLastNewData()[0].get("bID"));
            TestCase.assertNull(env.listener("s0").getLastOldData());  // receive no more old data
            env.listener("s0").reset();

            sendEvent(env, "a", 2);
            TestCase.assertFalse(env.listener("s0").isInvoked());

            sendEvent(env, "a", 3);
            TestCase.assertFalse(env.listener("s0").isInvoked());

            env.undeployAll();
        }
    }

    private static void sendEvents(RegressionEnvironment env, String[] stringValue) {
        for (int i = 0; i < stringValue.length; i++) {
            sendEvent(env, stringValue[i], 2);
        }
    }

    private static Object sendEvent(RegressionEnvironment env, String stringValue, int intPrimitive) {
        SupportBean theEvent = new SupportBean();
        theEvent.setTheString(stringValue);
        theEvent.setIntPrimitive(intPrimitive);
        env.sendEventBean(theEvent);
        return theEvent;
    }
}

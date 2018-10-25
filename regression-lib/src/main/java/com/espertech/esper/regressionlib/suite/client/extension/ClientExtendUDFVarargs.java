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

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.internal.collection.UniformPair;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.SupportMessageAssertUtil;
import com.espertech.esper.regressionlib.support.bean.ISupportBImpl;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionlib.support.client.SupportSingleRowFunction;

import java.io.StringWriter;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;

public class ClientExtendUDFVarargs implements RegressionExecution {
    public void run(RegressionEnvironment env) {
        AtomicInteger milestone = new AtomicInteger();

        runVarargAssertion(env,
            milestone, makePair("varargsOnlyInt(1, 2, 3, 4)", "1,2,3,4"),
            makePair("varargsOnlyInt(1, 2, 3)", "1,2,3"),
            makePair("varargsOnlyInt(1, 2)", "1,2"),
            makePair("varargsOnlyInt(1)", "1"),
            makePair("varargsOnlyInt()", ""));

        runVarargAssertion(
            env, milestone,
            makePair("varargsW1Param('abc', 1.0, 2.0)", "abc,1.0,2.0"),
            makePair("varargsW1Param('abc', 1, 2)", "abc,1.0,2.0"),
            makePair("varargsW1Param('abc', 1)", "abc,1.0"),
            makePair("varargsW1Param('abc')", "abc")
        );

        runVarargAssertion(
            env, milestone, makePair("varargsW2Param(1, 2.0, 3L, 4L)", "1,2.0,3,4"),
            makePair("varargsW2Param(1, 2.0, 3L)", "1,2.0,3"),
            makePair("varargsW2Param(1, 2.0)", "1,2.0"),
            makePair("varargsW2Param(1, 2.0, 3, 4L)", "1,2.0,3,4"),
            makePair("varargsW2Param(1, 2.0, 3L, 4L)", "1,2.0,3,4"),
            makePair("varargsW2Param(1, 2.0, 3, 4)", "1,2.0,3,4"),
            makePair("varargsW2Param(1, 2.0, 3L, 4)", "1,2.0,3,4"));

        runVarargAssertion(
            env, milestone, makePair("varargsOnlyWCtx(1, 2, 3)", "CTX+1,2,3"),
            makePair("varargsOnlyWCtx(1, 2)", "CTX+1,2"),
            makePair("varargsOnlyWCtx(1)", "CTX+1"),
            makePair("varargsOnlyWCtx()", "CTX+"));

        runVarargAssertion(
            env, milestone, makePair("varargsW1ParamWCtx('a', 1, 2, 3)", "CTX+a,1,2,3"),
            makePair("varargsW1ParamWCtx('a', 1, 2)", "CTX+a,1,2"),
            makePair("varargsW1ParamWCtx('a', 1)", "CTX+a,1"),
            makePair("varargsW1ParamWCtx('a')", "CTX+a,"));

        runVarargAssertion(
            env, milestone, makePair("varargsW2ParamWCtx('a', 'b', 1, 2, 3)", "CTX+a,b,1,2,3"),
            makePair("varargsW2ParamWCtx('a', 'b', 1, 2)", "CTX+a,b,1,2"),
            makePair("varargsW2ParamWCtx('a', 'b', 1)", "CTX+a,b,1"),
            makePair("varargsW2ParamWCtx('a', 'b')", "CTX+a,b,"),
            makePair(SupportSingleRowFunction.class.getName() + ".varargsW2ParamWCtx('a', 'b')", "CTX+a,b,"));

        runVarargAssertion(
            env, milestone, makePair("varargsOnlyObject('a', 1, new BigInteger('2'))", "a,1,2"),
            makePair("varargsOnlyNumber(1f, 2L, 3, new BigInteger('4'))", "1.0,2,3,4"));

        runVarargAssertion(
            env, milestone, makePair("varargsOnlyNumber(1f, 2L, 3, new BigInteger('4'))", "1.0,2,3,4"));

        runVarargAssertion(
            env, milestone, makePair("varargsOnlyISupportBaseAB(new " + ISupportBImpl.class.getName() + "('a', 'b'))", "ISupportBImpl{valueB='a', valueBaseAB='b'}"));

        // tests for array-passthru
        runVarargAssertion(
            env, milestone, makePair("varargsOnlyString({'a'})", "a"),
            makePair("varargsOnlyString({'a', 'b'})", "a,b"),
            makePair("varargsOnlyObject({'a', 'b'})", "a,b"),
            makePair("varargsOnlyObject({})", ""),
            makePair("varargsObjectsWCtx({1, 'a'})", "CTX+1,a"),
            makePair("varargsW1ParamObjectsWCtx(1, {'a', 1})", "CTX+,1,a,1")
        );

        // try Arrays.asList
        tryAssertionArraysAsList(env, milestone);

        SupportMessageAssertUtil.tryInvalidCompile(env, "select varargsOnlyInt(1, null) from SupportBean", "Failed to validate select-clause expression 'varargsOnlyInt(1,null)': Could not find static method");

        runVarargAssertion(env, milestone, makePair("varargsOnlyBoxedFloat(cast(1, byte), cast(2, short), null, 3)", "1.0,2.0,null,3.0"));
        runVarargAssertion(env, milestone, makePair("varargsOnlyBoxedShort(null, cast(1, byte))", "null,1"));
        runVarargAssertion(env, milestone, makePair("varargsOnlyBoxedByte(null, cast(1, byte))", "null,1"));

        // test excact match takes priority over varargs
        runVarargAssertion(env, milestone,
            makePair("varargOverload()", "many"),
            makePair("varargOverload(1)", "p1"),
            makePair("varargOverload(1, 2)", "p2"),
            makePair("varargOverload(1, 2, 3)", "p3"),
            makePair("varargOverload(1, 2, 3, 4)", "many")
        );
    }

    private void runVarargAssertion(RegressionEnvironment env, AtomicInteger milestone, UniformPair<String>... pairs) {
        StringWriter buf = new StringWriter();
        buf.append("@name('test') select ");
        int count = 0;
        for (UniformPair<String> pair : pairs) {
            buf.append(pair.getFirst());
            buf.append(" as c");
            buf.append(Integer.toString(count));
            count++;
            buf.append(",");
        }
        buf.append("intPrimitive from SupportBean");

        env.compileDeployAddListenerMile(buf.toString(), "test", milestone.getAndIncrement());

        env.sendEventBean(new SupportBean());
        EventBean out = env.listener("test").assertOneGetNewAndReset();

        count = 0;
        for (UniformPair<String> pair : pairs) {
            assertEquals("failed for '" + pair.getFirst() + "'", pair.getSecond(), out.get("c" + count));
            count++;
        }

        env.undeployAll();
    }

    private UniformPair<String> makePair(String expression, String expected) {
        return new UniformPair<>(expression, expected);
    }

    private void tryAssertionArraysAsList(RegressionEnvironment env, AtomicInteger milestone) {
        String epl = "@name('s0') select " +
            "java.util.Arrays.asList('a') as c0, " +
            "java.util.Arrays.asList({'a'}) as c1, " +
            "java.util.Arrays.asList('a', 'b') as c2, " +
            "java.util.Arrays.asList({'a', 'b'}) as c3 " +
            "from SupportBean";
        env.compileDeployAddListenerMile(epl, "s0", milestone.getAndIncrement());

        env.sendEventBean(new SupportBean());
        EventBean event = env.listener("s0").assertOneGetNewAndReset();
        assertEqualsColl(event, "c0", "a");
        assertEqualsColl(event, "c1", "a");
        assertEqualsColl(event, "c2", "a", "b");
        assertEqualsColl(event, "c3", "a", "b");

        env.undeployAll();
    }

    private void assertEqualsColl(EventBean event, String property, String... values) {
        Collection data = (Collection) event.get(property);
        EPAssertionUtil.assertEqualsExactOrder(values, data.toArray());
    }
}

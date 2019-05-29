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
package com.espertech.esper.regressionlib.suite.event.infra;

import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.runtime.client.EPStatement;
import com.espertech.esper.runtime.client.scopetest.SupportUpdateListener;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;

import java.util.HashMap;

import static com.espertech.esper.regressionlib.support.events.SupportEventInfra.*;
import static com.espertech.esper.runtime.client.scopetest.SupportUpdateListener.getInvokedFlagsAndReset;
import static org.apache.avro.SchemaBuilder.record;

public class EventInfraSuperType implements RegressionExecution {

    public void run(RegressionEnvironment env) {
        RegressionPath path = new RegressionPath();

        // Bean
        runAssertion(env, path, "Bean", FBEANWTYPE, new Bean_Type_Root(), new Bean_Type_1(), new Bean_Type_2(), new Bean_Type_2_1());

        // Map
        runAssertion(env, path, "Map", FMAPWTYPE, new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>());

        // OA
        runAssertion(env, path, "OA", FOAWTYPE, new Object[0], new Object[0], new Object[0], new Object[0]);

        // Avro
        Schema fake = record("fake").fields().endRecord();
        runAssertion(env, path, "Avro", FAVROWTYPE, new GenericData.Record(fake), new GenericData.Record(fake), new GenericData.Record(fake), new GenericData.Record(fake));

        // Json
        String schemas = "@public @buseventtype @name('schema') create json schema Json_Type_Root();\n" +
            "@public @buseventtype create json schema Json_Type_1() inherits Json_Type_Root;\n" +
            "@public @buseventtype create json schema Json_Type_2() inherits Json_Type_Root;\n" +
            "@public @buseventtype create json schema Json_Type_2_1() inherits Json_Type_2;\n";
        env.compileDeploy(schemas, path);
        runAssertion(env, path, "Json", FJSONWTYPE, "{}", "{}", "{}", "{}");
    }

    private void runAssertion(RegressionEnvironment env,
                              RegressionPath path,
                              String typePrefix,
                              FunctionSendEventWType sender,
                              Object root, Object type1, Object type2, Object type21) {

        String[] typeNames = "Type_Root,Type_1,Type_2,Type_2_1".split(",");
        EPStatement[] statements = new EPStatement[4];
        SupportUpdateListener[] listeners = new SupportUpdateListener[4];
        for (int i = 0; i < typeNames.length; i++) {
            env.compileDeploy("@name('s" + i + "') select * from " + typePrefix + "_" + typeNames[i], path);
            statements[i] = env.statement("s" + i);
            listeners[i] = new SupportUpdateListener();
            statements[i].addListener(listeners[i]);
        }

        sender.apply(env, root, typePrefix + "_" + typeNames[0]);
        EPAssertionUtil.assertEqualsExactOrder(new boolean[]{true, false, false, false}, getInvokedFlagsAndReset(listeners));

        sender.apply(env, type1, typePrefix + "_" + typeNames[1]);
        EPAssertionUtil.assertEqualsExactOrder(new boolean[]{true, true, false, false}, getInvokedFlagsAndReset(listeners));

        sender.apply(env, type2, typePrefix + "_" + typeNames[2]);
        EPAssertionUtil.assertEqualsExactOrder(new boolean[]{true, false, true, false}, getInvokedFlagsAndReset(listeners));

        sender.apply(env, type21, typePrefix + "_" + typeNames[3]);
        EPAssertionUtil.assertEqualsExactOrder(new boolean[]{true, false, true, true}, getInvokedFlagsAndReset(listeners));

        env.undeployAll();
    }

    public static class Bean_Type_Root {
    }

    public static class Bean_Type_1 extends Bean_Type_Root {
    }

    public static class Bean_Type_2 extends Bean_Type_Root {
    }

    public static class Bean_Type_2_1 extends Bean_Type_2 {
    }
}

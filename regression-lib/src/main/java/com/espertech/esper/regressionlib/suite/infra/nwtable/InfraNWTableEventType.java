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
package com.espertech.esper.regressionlib.suite.infra.nwtable;

import com.espertech.esper.common.internal.support.SupportEventTypeAssertionEnum;
import com.espertech.esper.common.internal.support.SupportEventTypeAssertionUtil;
import com.espertech.esper.common.internal.util.CollectionUtil;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;

import java.util.ArrayList;
import java.util.Collection;

public class InfraNWTableEventType {

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new InfraNWTableEventTypeInvalid());
        execs.add(new InfraNWTableEventTypeDefineFields());
        execs.add(new InfraNWTableEventTypeInsertIntoProtected());
        return execs;
    }

    private static class InfraNWTableEventTypeInsertIntoProtected implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "module test;\n" +
                "@name('event') @public @buseventtype @public create map schema Fubar as (foo string, bar double);\n" +
                "@name('window') @protected create window Snafu#keepall as Fubar;\n" +
                "@name('insert') @private insert into Snafu select * from Fubar;\n";
            env.compileDeploy(epl);

            env.sendEventMap(CollectionUtil.buildMap("foo", "a", "bar", 1d), "Fubar");
            env.sendEventMap(CollectionUtil.buildMap("foo", "b", "bar", 2d), "Fubar");

            env.assertPropsPerRowIterator("window", "foo,bar".split(","), new Object[][] {{"a", 1d}, {"b", 2d}});

            env.undeployAll();
        }
    }

    private static class InfraNWTableEventTypeDefineFields implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            runAssertionType(env, true);
            runAssertionType(env, false);
        }
    }

    private static class InfraNWTableEventTypeInvalid implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl;

            // name cannot be the same as an existing event type
            epl = "create schema SchemaOne as (p0 string);\n" +
                "create window SchemaOne#keepall as SchemaOne;\n";
            env.tryInvalidCompile(epl,
                "Error starting statement: An event type or schema by name 'SchemaOne' already exists");

            epl = "create schema SchemaTwo as (p0 string);\n" +
                "create table SchemaTwo(c0 int);\n";
            env.tryInvalidCompile(epl,
                "An event type by name 'SchemaTwo' has already been declared");
        }
    }

    private static void runAssertionType(RegressionEnvironment env, boolean namedWindow) {
        String eplCreate = namedWindow ?
            "@name('s0') @public create window MyInfra#keepall as (c0 int[], c1 int[primitive])" :
            "@name('s0') @public create table MyInfra (c0 int[], c1 int[primitive])";
        env.compileDeploy(eplCreate);

        Object[][] expectedType = new Object[][]{{"c0", Integer[].class}, {"c1", int[].class}};
        env.assertStatement("s0", statement -> SupportEventTypeAssertionUtil.assertEventTypeProperties(expectedType, statement.getEventType(), SupportEventTypeAssertionEnum.NAME, SupportEventTypeAssertionEnum.TYPE));

        env.undeployAll();
    }
}

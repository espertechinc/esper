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
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.SupportMessageAssertUtil;

public class InfraNWTableEventType implements RegressionExecution {

    public void run(RegressionEnvironment env) {
        runAssertionType(env, true);
        runAssertionType(env, false);

        String epl;

        // name cannot be the same as an existing event type
        epl = "create schema SchemaOne as (p0 string);\n" +
            "create window SchemaOne#keepall as SchemaOne;\n";
        SupportMessageAssertUtil.tryInvalidCompile(env, epl,
            "Error starting statement: An event type or schema by name 'SchemaOne' already exists");

        epl = "create schema SchemaTwo as (p0 string);\n" +
            "create table SchemaTwo(c0 int);\n";
        SupportMessageAssertUtil.tryInvalidCompile(env, epl,
            "An event type by name 'SchemaTwo' has already been declared");
    }

    private static void runAssertionType(RegressionEnvironment env, boolean namedWindow) {
        String eplCreate = namedWindow ?
            "@name('s0') create window MyInfra#keepall as (c0 int[], c1 int[primitive])" :
            "@name('s0') create table MyInfra (c0 int[], c1 int[primitive])";
        env.compileDeploy(eplCreate);

        Object[][] expectedType = new Object[][]{{"c0", Integer[].class}, {"c1", int[].class}};
        SupportEventTypeAssertionUtil.assertEventTypeProperties(expectedType, env.statement("s0").getEventType(), SupportEventTypeAssertionEnum.NAME, SupportEventTypeAssertionEnum.TYPE);

        env.undeployAll();
    }
}

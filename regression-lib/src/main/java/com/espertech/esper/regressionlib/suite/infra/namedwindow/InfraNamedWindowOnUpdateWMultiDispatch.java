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
package com.espertech.esper.regressionlib.suite.infra.namedwindow;

import com.espertech.esper.common.internal.support.EventRepresentationChoice;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionFlag;
import com.espertech.esper.regressionlib.framework.RegressionPath;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;

import static org.junit.Assert.assertEquals;

/**
 * NOTE: More namedwindow-related tests in "nwtable"
 */
public class InfraNamedWindowOnUpdateWMultiDispatch implements RegressionExecution {
    @Override
    public EnumSet<RegressionFlag> flags() {
        return EnumSet.of(RegressionFlag.EXCLUDEWHENINSTRUMENTED);
    }

    public void run(RegressionEnvironment env) {
        String[] fields = "company,value,total".split(",");

        String eplSchema = "@buseventtype create schema S2 ( company string, value double, total double)";
        RegressionPath path = new RegressionPath();
        env.compileDeploy(eplSchema, path);

        // ESPER-568
        env.compileDeploy("@name('create') create window S2Win#time(25 hour)#firstunique(company) as S2", path);
        env.compileDeploy("insert into S2Win select * from S2#firstunique(company)", path);
        env.compileDeploy("on S2 as a update S2Win as b set total = b.value + a.value", path);
        env.compileDeploy("@name('s0') select count(*) as cnt from S2Win", path).addListener("s0");

        createSendEvent(env, "S2", "AComp", 3.0, 0.0);
        assertCount(env, 1L);
        env.assertPropsPerRowIterator("create", fields, new Object[][]{{"AComp", 3.0, 0.0}});

        createSendEvent(env, "S2", "AComp", 6.0, 0.0);
        assertCount(env, 1L);
        env.assertPropsPerRowIterator("create", fields, new Object[][]{{"AComp", 3.0, 9.0}});

        createSendEvent(env, "S2", "AComp", 5.0, 0.0);
        assertCount(env, 1L);
        env.assertPropsPerRowIterator("create", fields, new Object[][]{{"AComp", 3.0, 8.0}});

        createSendEvent(env, "S2", "BComp", 4.0, 0.0);
        // this example does not have @priority thereby it is undefined whether there are two counts delivered or one
        env.assertListener("s0", listener -> {
            if (listener.getLastNewData().length == 2) {
                assertEquals(1L, listener.getLastNewData()[0].get("cnt"));
                assertEquals(2L, listener.getLastNewData()[1].get("cnt"));
            } else {
                assertEquals(2L, listener.assertOneGetNewAndReset().get("cnt"));
            }
        });
        env.assertPropsPerRowIterator("create", fields, new Object[][]{{"AComp", 3.0, 7.0}, {"BComp", 4.0, 0.0}});

        env.undeployAll();
    }

    private static void assertCount(RegressionEnvironment env, long expected) {
        env.assertEqualsNew("s0", "cnt", expected);
    }

    private static void createSendEvent(RegressionEnvironment env, String typeName, String company, double value, double total) {
        HashMap<String, Object> map = new LinkedHashMap<String, Object>();
        map.put("company", company);
        map.put("value", value);
        map.put("total", total);
        if (EventRepresentationChoice.getEngineDefault(env.getConfiguration()).isObjectArrayEvent()) {
            env.sendEventObjectArray(map.values().toArray(), typeName);
        } else {
            env.sendEventMap(map, typeName);
        }
    }
}
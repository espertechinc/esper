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
package com.espertech.esper.regressionlib.suite.event.map;

import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.internal.event.core.MappedEventBean;
import com.espertech.esper.common.internal.event.core.ObjectArrayBackedEventBean;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class EventMapObjectArrayInterUse implements RegressionExecution {
    public void run(RegressionEnvironment env) {
        runAssertionObjectArrayWithMap(env);
        runAssertionMapWithObjectArray(env);
    }

    // test ObjectArray event with Map, Map[], MapType and MapType[] properties
    private void runAssertionObjectArrayWithMap(RegressionEnvironment env) {

        env.compileDeploy("@name('s0') select p0 as c0, p1.im as c1, p2[0].im as c2, p3.om as c3 from OAType");
        env.addListener("s0");

        env.sendEventObjectArray(new Object[]{"E1", Collections.singletonMap("im", "IM1"), new Map[]{Collections.singletonMap("im", "IM2")}, Collections.singletonMap("om", "OM1")}, "OAType");
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "c0,c1,c2,c3".split(","), new Object[]{"E1", "IM1", "IM2", "OM1"});

        env.undeployAll();

        // test inserting from array to map
        env.compileDeploy("@name('s0') insert into MapType(im) select p0 from OAType").addListener("s0");
        env.sendEventObjectArray(new Object[]{"E1", null, null, null}, "OAType");
        assertTrue(env.listener("s0").assertOneGetNew() instanceof MappedEventBean);
        assertEquals("E1", env.listener("s0").assertOneGetNewAndReset().get("im"));

        env.undeployAll();
    }

    // test Map event with ObjectArrayType and ObjectArrayType[] properties
    private void runAssertionMapWithObjectArray(RegressionEnvironment env) {

        RegressionPath path = new RegressionPath();
        String schema = "create objectarray schema OATypeInMap(p0 string, p1 int);\n" +
            "create map schema MapTypeWOA(oa1 OATypeInMap, oa2 OATypeInMap[]);\n";
        env.compileDeployWBusPublicType(schema, path);

        env.compileDeploy("@name('s0') select oa1.p0 as c0, oa1.p1 as c1, oa2[0].p0 as c2, oa2[1].p1 as c3 from MapTypeWOA", path);
        env.addListener("s0");

        Map<String, Object> data = new HashMap<String, Object>();
        data.put("oa1", new Object[]{"A", 100});
        data.put("oa2", new Object[][]{{"B", 200}, {"C", 300}});
        env.sendEventMap(data, "MapTypeWOA");
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "c0,c1,c2,c3".split(","), new Object[]{"A", 100, "B", 300});
        env.undeployModuleContaining("s0");

        // test inserting from map to array
        env.compileDeploy("@name('s0') insert into OATypeInMap select 'a' as p0, 1 as p1 from MapTypeWOA", path).addListener("s0");
        env.sendEventMap(data, "MapTypeWOA");
        assertTrue(env.listener("s0").assertOneGetNew() instanceof ObjectArrayBackedEventBean);
        assertEquals("a", env.listener("s0").assertOneGetNew().get("p0"));
        assertEquals(1, env.listener("s0").assertOneGetNewAndReset().get("p1"));

        env.undeployAll();
    }
}

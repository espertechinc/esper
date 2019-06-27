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

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;

import java.io.Serializable;
import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class EventInfraPropertyIndexedKeyExpr implements RegressionExecution {
    public void run(RegressionEnvironment env) {
        runAssertionOA(env);
        runAssertionMap(env);
        runAssertionWrapper(env);
        runAssertionBean(env);
        runAssertionJson(env);
        runAssertionJsonClassProvided(env);
    }

    private void runAssertionJsonClassProvided(RegressionEnvironment env) {
        env.compileDeploy("@JsonSchema(className='" + MyLocalJsonProvided.class.getName() + "') @public @buseventtype create json schema JsonSchema();\n" +
            "@name('s0') select * from JsonSchema;\n").addListener("s0");
        env.sendEventJson("{ \"indexed\": [1, 2], \"mapped\" : { \"keyOne\": 20 }}", "JsonSchema");
        EventBean event = env.listener("s0").assertOneGetNewAndReset();

        assertEquals(2, event.getEventType().getGetterIndexed("indexed").get(event, 1));
        assertEquals(20, event.getEventType().getGetterMapped("mapped").get(event, "keyOne"));

        env.undeployAll();
    }

    private void runAssertionJson(RegressionEnvironment env) {
        env.compileDeploy("@public @buseventtype create json schema JsonSchema(indexed int[], mapped java.util.Map);\n" +
            "@name('s0') select * from JsonSchema;\n").addListener("s0");
        env.sendEventJson("{ \"indexed\": [1, 2], \"mapped\" : { \"keyOne\": 20 }}", "JsonSchema");
        EventBean event = env.listener("s0").assertOneGetNewAndReset();

        assertEquals(2, event.getEventType().getGetterIndexed("indexed").get(event, 1));
        assertEquals(20, event.getEventType().getGetterMapped("mapped").get(event, "keyOne"));

        env.undeployAll();
    }

    private void runAssertionBean(RegressionEnvironment env) {
        RegressionPath path = new RegressionPath();
        env.compileDeployWBusPublicType("create schema MyIndexMappedSamplerBean as " + MyIndexMappedSamplerBean.class.getName(), path);

        env.compileDeploy("@name('s0') select * from MyIndexMappedSamplerBean", path).addListener("s0");

        env.sendEventBean(new MyIndexMappedSamplerBean());

        EventBean event = env.listener("s0").assertOneGetNewAndReset();
        EventType type = event.getEventType();
        assertEquals(2, type.getGetterIndexed("listOfInt").get(event, 1));
        assertEquals(2, type.getGetterIndexed("iterableOfInt").get(event, 1));

        env.undeployAll();
    }

    private void runAssertionWrapper(RegressionEnvironment env) {
        env.compileDeploy("@name('s0') select {1, 2} as arr, *, Collections.singletonMap('A', 2) as mapped from SupportBean");
        env.addListener("s0");

        env.sendEventBean(new SupportBean());
        EventBean event = env.listener("s0").assertOneGetNewAndReset();
        EventType type = event.getEventType();
        assertEquals(2, type.getGetterIndexed("arr").get(event, 1));
        assertEquals(2, type.getGetterMapped("mapped").get(event, "A"));

        env.undeployAll();
    }

    private void runAssertionMap(RegressionEnvironment env) {
        String epl = "create schema MapEventInner(p0 string);\n" +
            "create schema MapEvent(intarray int[], mapinner MapEventInner[]);\n" +
            "@name('s0') select * from MapEvent;\n";
        env.compileDeployWBusPublicType(epl, new RegressionPath()).addListener("s0");

        Map[] mapinner = new Map[]{Collections.singletonMap("p0", "A"), Collections.singletonMap("p0", "B")};
        Map map = new HashMap();
        map.put("intarray", new int[]{1, 2});
        map.put("mapinner", mapinner);
        env.sendEventMap(map, "MapEvent");
        EventBean event = env.listener("s0").assertOneGetNewAndReset();
        EventType type = event.getEventType();
        assertEquals(2, type.getGetterIndexed("intarray").get(event, 1));
        assertNull(type.getGetterIndexed("dummy"));
        assertEquals(mapinner[1], type.getGetterIndexed("mapinner").get(event, 1));

        env.undeployAll();
    }

    private void runAssertionOA(RegressionEnvironment env) {
        String epl = "create objectarray schema OAEventInner(p0 string);\n" +
            "create objectarray schema OAEvent(intarray int[], oainner OAEventInner[]);\n" +
            "@name('s0') select * from OAEvent;\n";
        env.compileDeployWBusPublicType(epl, new RegressionPath()).addListener("s0");

        Object[] oainner = new Object[]{new Object[]{"A"}, new Object[]{"B"}};
        env.sendEventObjectArray(new Object[]{new int[]{1, 2}, oainner}, "OAEvent");
        EventBean event = env.listener("s0").assertOneGetNewAndReset();
        EventType type = event.getEventType();
        assertEquals(2, type.getGetterIndexed("intarray").get(event, 1));
        assertNull(type.getGetterIndexed("dummy"));
        assertEquals(oainner[1], type.getGetterIndexed("oainner").get(event, 1));

        env.undeployAll();
    }

    public final static class MyIndexMappedSamplerBean {
        private final List<Integer> intlist = Arrays.asList(1, 2);

        public List<Integer> getListOfInt() {
            return intlist;
        }

        public Iterable<Integer> getIterableOfInt() {
            return intlist;
        }
    }

    public static class MyLocalJsonProvided implements Serializable {
        public int[] indexed;
        public Map<String, Object> mapped;
    }
}

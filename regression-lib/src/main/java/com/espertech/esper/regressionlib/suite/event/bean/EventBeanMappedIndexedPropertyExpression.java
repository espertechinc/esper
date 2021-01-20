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
package com.espertech.esper.regressionlib.suite.event.bean;

import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.regressionlib.support.bean.SupportBeanComplexProps;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class EventBeanMappedIndexedPropertyExpression implements RegressionExecution {

    public void run(RegressionEnvironment env) {

        // test bean-type
        RegressionPath path = new RegressionPath();
        String eplBeans = "select " +
            "mapped(theString) as val0, " +
            "indexed(intPrimitive) as val1 " +
            "from SupportBeanComplexProps#lastevent, SupportBean sb unidirectional";
        runAssertionBean(env, path, eplBeans);

        // test bean-type prefixed
        String eplBeansPrefixed = "select " +
            "sbcp.mapped(theString) as val0, " +
            "sbcp.indexed(intPrimitive) as val1 " +
            "from SupportBeanComplexProps#lastevent sbcp, SupportBean sb unidirectional";
        runAssertionBean(env, path, eplBeansPrefixed);

        // test wrap
        env.compileDeploy("@public insert into SecondStream select 'a' as val0, * from SupportBeanComplexProps", path);

        String eplWrap = "select " +
            "mapped(theString) as val0," +
            "indexed(intPrimitive) as val1 " +
            "from SecondStream #lastevent, SupportBean unidirectional";
        runAssertionBean(env, path, eplWrap);

        String eplWrapPrefixed = "select " +
            "sbcp.mapped(theString) as val0," +
            "sbcp.indexed(intPrimitive) as val1 " +
            "from SecondStream #lastevent sbcp, SupportBean unidirectional";
        runAssertionBean(env, path, eplWrapPrefixed);

        // test Map-type
        String eplMap = "select " +
            "mapped(theString) as val0," +
            "indexed(intPrimitive) as val1 " +
            "from MapEvent#lastevent, SupportBean unidirectional";
        runAssertionMap(env, eplMap);

        String eplMapPrefixed = "select " +
            "sbcp.mapped(theString) as val0," +
            "sbcp.indexed(intPrimitive) as val1 " +
            "from MapEvent#lastevent sbcp, SupportBean unidirectional";
        runAssertionMap(env, eplMapPrefixed);

        // test insert-int
        env.compileDeploy("@name('s0') select name,value,properties(name) = value as ok from InputEvent").addListener("s0");

        env.sendEventMap(makeMapEvent("name", "value1", Collections.singletonMap("name", "xxxx")), "InputEvent");
        env.assertEqualsNew("s0", "ok", false);

        env.sendEventMap(makeMapEvent("name", "value1", Collections.singletonMap("name", "value1")), "InputEvent");
        env.assertEqualsNew("s0", "ok", true);

        env.undeployAll();

        // test Object-array-type
        String eplObjectArray = "select " +
            "mapped(theString) as val0," +
            "indexed(intPrimitive) as val1 " +
            "from ObjectArrayEvent#lastevent, SupportBean unidirectional";
        runAssertionObjectArray(env, eplObjectArray);

        String eplObjectArrayPrefixed = "select " +
            "sbcp.mapped(theString) as val0," +
            "sbcp.indexed(intPrimitive) as val1 " +
            "from ObjectArrayEvent#lastevent sbcp, SupportBean unidirectional";
        runAssertionObjectArray(env, eplObjectArrayPrefixed);
    }

    private void runAssertionMap(RegressionEnvironment env, String epl) {
        env.compileDeploy("@name('s0') " + epl).addListener("s0");

        env.sendEventMap(makeMapEvent(), "MapEvent");
        env.sendEventBean(new SupportBean("keyOne", 1));
        env.assertPropsNew("s0", "val0,val1".split(","), new Object[]{"valueOne", 2});
        env.undeployModuleContaining("s0");
    }

    private void runAssertionObjectArray(RegressionEnvironment env, String epl) {
        env.compileDeploy("@name('s0') " + epl).addListener("s0");

        env.sendEventObjectArray(new Object[]{Collections.singletonMap("keyOne", "valueOne"), new int[]{1, 2}}, "ObjectArrayEvent");
        env.sendEventBean(new SupportBean("keyOne", 1));
        env.assertPropsNew("s0", "val0,val1".split(","), new Object[]{"valueOne", 2});
        env.undeployModuleContaining("s0");
    }

    private void runAssertionBean(RegressionEnvironment env, RegressionPath path, String epl) {
        env.compileDeploy("@name('s0') " + epl, path).addListener("s0");

        env.sendEventBean(SupportBeanComplexProps.makeDefaultBean());
        env.sendEventBean(new SupportBean("keyOne", 1));
        env.assertPropsNew("s0", "val0,val1".split(","), new Object[]{"valueOne", 2});
        env.undeployModuleContaining("s0");
    }

    private Map<String, Object> makeMapEvent() {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("mapped", Collections.singletonMap("keyOne", "valueOne"));
        map.put("indexed", new int[]{1, 2});
        return map;
    }

    private Map<String, Object> makeMapEvent(String name, String value, Map properties) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("name", name);
        map.put("value", value);
        map.put("properties", properties);
        return map;
    }
}

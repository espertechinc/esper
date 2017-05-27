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
package com.espertech.esper.regression.event.bean;

import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportBeanComplexProps;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ExecEventBeanMappedIndexedPropertyExpression implements RegressionExecution {

    public void run(EPServiceProvider epService) throws Exception {
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean.class);
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBeanComplexProps.class);
        SupportUpdateListener listener = new SupportUpdateListener();

        // test bean-type
        String eplBeans = "select " +
                "mapped(theString) as val0, " +
                "indexed(intPrimitive) as val1 " +
                "from SupportBeanComplexProps#lastevent, SupportBean sb unidirectional";
        runAssertionBean(epService, listener, eplBeans);

        // test bean-type prefixed
        String eplBeansPrefixed = "select " +
                "sbcp.mapped(theString) as val0, " +
                "sbcp.indexed(intPrimitive) as val1 " +
                "from SupportBeanComplexProps#lastevent sbcp, SupportBean sb unidirectional";
        runAssertionBean(epService, listener, eplBeansPrefixed);

        // test wrap
        epService.getEPAdministrator().createEPL("insert into SecondStream select 'a' as val0, * from SupportBeanComplexProps");

        String eplWrap = "select " +
                "mapped(theString) as val0," +
                "indexed(intPrimitive) as val1 " +
                "from SecondStream #lastevent, SupportBean unidirectional";
        runAssertionBean(epService, listener, eplWrap);

        String eplWrapPrefixed = "select " +
                "sbcp.mapped(theString) as val0," +
                "sbcp.indexed(intPrimitive) as val1 " +
                "from SecondStream #lastevent sbcp, SupportBean unidirectional";
        runAssertionBean(epService, listener, eplWrapPrefixed);

        // test Map-type
        Map<String, Object> def = new HashMap<String, Object>();
        def.put("mapped", new HashMap());
        def.put("indexed", int[].class);
        epService.getEPAdministrator().getConfiguration().addEventType("MapEvent", def);

        String eplMap = "select " +
                "mapped(theString) as val0," +
                "indexed(intPrimitive) as val1 " +
                "from MapEvent#lastevent, SupportBean unidirectional";
        runAssertionMap(epService, listener, eplMap);

        String eplMapPrefixed = "select " +
                "sbcp.mapped(theString) as val0," +
                "sbcp.indexed(intPrimitive) as val1 " +
                "from MapEvent#lastevent sbcp, SupportBean unidirectional";
        runAssertionMap(epService, listener, eplMapPrefixed);

        // test insert-int
        Map<String, Object> defType = new HashMap<String, Object>();
        defType.put("name", String.class);
        defType.put("value", String.class);
        defType.put("properties", Map.class);
        epService.getEPAdministrator().getConfiguration().addEventType("InputEvent", defType);
        epService.getEPAdministrator().createEPL("select name,value,properties(name) = value as ok from InputEvent").addListener(listener);

        listener.reset();
        epService.getEPRuntime().sendEvent(makeMapEvent("name", "value1", Collections.singletonMap("name", "xxxx")), "InputEvent");
        assertFalse((Boolean) listener.assertOneGetNewAndReset().get("ok"));

        epService.getEPRuntime().sendEvent(makeMapEvent("name", "value1", Collections.singletonMap("name", "value1")), "InputEvent");
        assertTrue((Boolean) listener.assertOneGetNewAndReset().get("ok"));

        // test Object-array-type
        epService.getEPAdministrator().getConfiguration().addEventType("ObjectArrayEvent", new String[]{"mapped", "indexed"}, new Object[]{new HashMap(), int[].class});
        String eplObjectArray = "select " +
                "mapped(theString) as val0," +
                "indexed(intPrimitive) as val1 " +
                "from ObjectArrayEvent#lastevent, SupportBean unidirectional";
        runAssertionObjectArray(epService, listener, eplObjectArray);

        String eplObjectArrayPrefixed = "select " +
                "sbcp.mapped(theString) as val0," +
                "sbcp.indexed(intPrimitive) as val1 " +
                "from ObjectArrayEvent#lastevent sbcp, SupportBean unidirectional";
        runAssertionObjectArray(epService, listener, eplObjectArrayPrefixed);
    }

    private void runAssertionMap(EPServiceProvider epService, SupportUpdateListener listener, String epl) {
        EPStatement stmtMap = epService.getEPAdministrator().createEPL(epl);
        stmtMap.addListener(listener);

        epService.getEPRuntime().sendEvent(makeMapEvent(), "MapEvent");
        epService.getEPRuntime().sendEvent(new SupportBean("keyOne", 1));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "val0,val1".split(","), new Object[]{"valueOne", 2});
        stmtMap.destroy();
    }

    private void runAssertionObjectArray(EPServiceProvider epService, SupportUpdateListener listener, String epl) {
        EPStatement stmtObjectArray = epService.getEPAdministrator().createEPL(epl);
        stmtObjectArray.addListener(listener);

        epService.getEPRuntime().sendEvent(new Object[]{Collections.singletonMap("keyOne", "valueOne"), new int[]{1, 2}}, "ObjectArrayEvent");
        epService.getEPRuntime().sendEvent(new SupportBean("keyOne", 1));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "val0,val1".split(","), new Object[]{"valueOne", 2});
        stmtObjectArray.destroy();
    }

    private void runAssertionBean(EPServiceProvider epService, SupportUpdateListener listener, String epl) {
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(SupportBeanComplexProps.makeDefaultBean());
        epService.getEPRuntime().sendEvent(new SupportBean("keyOne", 1));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "val0,val1".split(","), new Object[]{"valueOne", 2});
        stmt.destroy();
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

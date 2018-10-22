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

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.meta.EventTypeApplicationType;
import com.espertech.esper.common.client.meta.EventTypeTypeClass;
import com.espertech.esper.common.client.util.EventTypeBusModifier;
import com.espertech.esper.common.internal.event.core.EventTypeSPI;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.support.bean.SupportLegacyBean;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class EventBeanPublicAccessors implements RegressionExecution {
    protected static SupportLegacyBean makeSampleEvent() {
        Map<String, String> mappedProperty = new HashMap<String, String>();
        mappedProperty.put("key1", "value1");
        mappedProperty.put("key2", "value2");
        return new SupportLegacyBean("leg", new String[]{"a", "b"}, mappedProperty, "nest");
    }

    public void run(RegressionEnvironment env) {

        // assert type metadata
        EventType type = env.runtime().getEventTypeService().getEventTypePreconfigured("AnotherLegacyEvent");
        assertEquals(EventTypeApplicationType.CLASS, type.getMetadata().getApplicationType());
        assertEquals("AnotherLegacyEvent", type.getMetadata().getName());

        String statementText = "@name('s0') select " +
            "fieldLegacyVal as fieldSimple," +
            "fieldStringArray as fieldArr," +
            "fieldStringArray[1] as fieldArrIndexed," +
            "fieldMapped as fieldMap," +
            "fieldNested as fieldNested," +
            "fieldNested.readNestedValue as fieldNestedVal," +
            "readLegacyBeanVal as simple," +
            "readLegacyNested as nestedObject," +
            "readLegacyNested.readNestedValue as nested," +
            "readStringArray[0] as array," +
            "readStringIndexed[1] as indexed," +
            "readMapByKey('key1') as mapped," +
            "readMap as mapItself," +
            "explicitFSimple, " +
            "explicitFIndexed[0], " +
            "explicitFNested, " +
            "explicitMSimple, " +
            "explicitMArray[0], " +
            "explicitMIndexed[1], " +
            "explicitMMapped('key2')" +
            " from AnotherLegacyEvent#length(5)";
        env.compileDeploy(statementText).addListener("s0");

        EventType eventType = env.statement("s0").getEventType();
        assertEquals(String.class, eventType.getPropertyType("fieldSimple"));
        assertEquals(String[].class, eventType.getPropertyType("fieldArr"));
        assertEquals(String.class, eventType.getPropertyType("fieldArrIndexed"));
        assertEquals(Map.class, eventType.getPropertyType("fieldMap"));
        assertEquals(SupportLegacyBean.LegacyNested.class, eventType.getPropertyType("fieldNested"));
        assertEquals(String.class, eventType.getPropertyType("fieldNestedVal"));
        assertEquals(String.class, eventType.getPropertyType("simple"));
        assertEquals(SupportLegacyBean.LegacyNested.class, eventType.getPropertyType("nestedObject"));
        assertEquals(String.class, eventType.getPropertyType("nested"));
        assertEquals(String.class, eventType.getPropertyType("array"));
        assertEquals(String.class, eventType.getPropertyType("indexed"));
        assertEquals(String.class, eventType.getPropertyType("mapped"));
        assertEquals(String.class, eventType.getPropertyType("explicitFSimple"));
        assertEquals(String.class, eventType.getPropertyType("explicitFIndexed[0]"));
        assertEquals(SupportLegacyBean.LegacyNested.class, eventType.getPropertyType("explicitFNested"));
        assertEquals(String.class, eventType.getPropertyType("explicitMSimple"));
        assertEquals(String.class, eventType.getPropertyType("explicitMArray[0]"));
        assertEquals(String.class, eventType.getPropertyType("explicitMIndexed[1]"));
        assertEquals(String.class, eventType.getPropertyType("explicitMMapped('key2')"));

        SupportLegacyBean legacyBean = makeSampleEvent();
        env.sendEventBean(legacyBean, "AnotherLegacyEvent");

        assertEquals(legacyBean.fieldLegacyVal, env.listener("s0").getLastNewData()[0].get("fieldSimple"));
        assertEquals(legacyBean.fieldStringArray, env.listener("s0").getLastNewData()[0].get("fieldArr"));
        assertEquals(legacyBean.fieldStringArray[1], env.listener("s0").getLastNewData()[0].get("fieldArrIndexed"));
        assertEquals(legacyBean.fieldMapped, env.listener("s0").getLastNewData()[0].get("fieldMap"));
        assertEquals(legacyBean.fieldNested, env.listener("s0").getLastNewData()[0].get("fieldNested"));
        assertEquals(legacyBean.fieldNested.readNestedValue(), env.listener("s0").getLastNewData()[0].get("fieldNestedVal"));

        assertEquals(legacyBean.readLegacyBeanVal(), env.listener("s0").getLastNewData()[0].get("simple"));
        assertEquals(legacyBean.readLegacyNested(), env.listener("s0").getLastNewData()[0].get("nestedObject"));
        assertEquals(legacyBean.readLegacyNested().readNestedValue(), env.listener("s0").getLastNewData()[0].get("nested"));
        assertEquals(legacyBean.readStringIndexed(0), env.listener("s0").getLastNewData()[0].get("array"));
        assertEquals(legacyBean.readStringIndexed(1), env.listener("s0").getLastNewData()[0].get("indexed"));
        assertEquals(legacyBean.readMapByKey("key1"), env.listener("s0").getLastNewData()[0].get("mapped"));
        assertEquals(legacyBean.readMap(), env.listener("s0").getLastNewData()[0].get("mapItself"));

        assertEquals(legacyBean.readLegacyBeanVal(), env.listener("s0").getLastNewData()[0].get("explicitFSimple"));
        assertEquals(legacyBean.readLegacyBeanVal(), env.listener("s0").getLastNewData()[0].get("explicitMSimple"));
        assertEquals(legacyBean.readLegacyNested(), env.listener("s0").getLastNewData()[0].get("explicitFNested"));
        assertEquals(legacyBean.readStringIndexed(0), env.listener("s0").getLastNewData()[0].get("explicitFIndexed[0]"));
        assertEquals(legacyBean.readStringIndexed(0), env.listener("s0").getLastNewData()[0].get("explicitMArray[0]"));
        assertEquals(legacyBean.readStringIndexed(1), env.listener("s0").getLastNewData()[0].get("explicitMIndexed[1]"));
        assertEquals(legacyBean.readMapByKey("key2"), env.listener("s0").getLastNewData()[0].get("explicitMMapped('key2')"));

        EventTypeSPI stmtType = (EventTypeSPI) env.statement("s0").getEventType();
        assertEquals(EventTypeBusModifier.NONBUS, stmtType.getMetadata().getBusModifier());
        assertEquals(EventTypeApplicationType.MAP, stmtType.getMetadata().getApplicationType());
        assertEquals(EventTypeTypeClass.STATEMENTOUT, stmtType.getMetadata().getTypeClass());

        env.undeployAll();
    }
}

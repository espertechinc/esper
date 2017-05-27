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

import com.espertech.esper.client.ConfigurationEventTypeLegacy;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.EventType;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.core.service.EPServiceProviderSPI;
import com.espertech.esper.event.EventTypeMetadata;
import com.espertech.esper.event.EventTypeSPI;
import com.espertech.esper.supportregression.bean.SupportLegacyBean;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class ExecEventBeanPublicAccessors implements RegressionExecution {
    private final boolean codegen;

    public ExecEventBeanPublicAccessors(boolean codegen) {
        this.codegen = codegen;
    }

    protected static SupportLegacyBean makeSampleEvent() {
        Map<String, String> mappedProperty = new HashMap<String, String>();
        mappedProperty.put("key1", "value1");
        mappedProperty.put("key2", "value2");
        return new SupportLegacyBean("leg", new String[]{"a", "b"}, mappedProperty, "nest");
    }

    public void run(EPServiceProvider epService) throws Exception {
        ConfigurationEventTypeLegacy legacyDef = new ConfigurationEventTypeLegacy();
        legacyDef.setAccessorStyle(ConfigurationEventTypeLegacy.AccessorStyle.PUBLIC);
        legacyDef.setCodeGeneration(codegen ? ConfigurationEventTypeLegacy.CodeGeneration.ENABLED : ConfigurationEventTypeLegacy.CodeGeneration.DISABLED);
        legacyDef.addFieldProperty("explicitFSimple", "fieldLegacyVal");
        legacyDef.addFieldProperty("explicitFIndexed", "fieldStringArray");
        legacyDef.addFieldProperty("explicitFNested", "fieldNested");
        legacyDef.addMethodProperty("explicitMSimple", "readLegacyBeanVal");
        legacyDef.addMethodProperty("explicitMArray", "readStringArray");
        legacyDef.addMethodProperty("explicitMIndexed", "readStringIndexed");
        legacyDef.addMethodProperty("explicitMMapped", "readMapByKey");
        epService.getEPAdministrator().getConfiguration().addEventType("MyLegacyEvent", SupportLegacyBean.class.getName(), legacyDef);

        legacyDef = new ConfigurationEventTypeLegacy();
        legacyDef.setAccessorStyle(ConfigurationEventTypeLegacy.AccessorStyle.PUBLIC);
        legacyDef.setCodeGeneration(ConfigurationEventTypeLegacy.CodeGeneration.DISABLED);
        epService.getEPAdministrator().getConfiguration().addEventType("MyLegacyNestedEvent", SupportLegacyBean.LegacyNested.class.getName(), legacyDef);

        // assert type metadata
        EventTypeSPI type = (EventTypeSPI) ((EPServiceProviderSPI) epService).getEventAdapterService().getExistsTypeByName("MyLegacyEvent");
        assertEquals(EventTypeMetadata.ApplicationType.CLASS, type.getMetadata().getOptionalApplicationType());
        assertEquals(1, type.getMetadata().getOptionalSecondaryNames().size());
        assertEquals(SupportLegacyBean.class.getName(), type.getMetadata().getOptionalSecondaryNames().iterator().next());
        assertEquals("MyLegacyEvent", type.getMetadata().getPrimaryName());
        assertEquals("MyLegacyEvent", type.getMetadata().getPublicName());
        assertEquals(EventTypeMetadata.TypeClass.APPLICATION, type.getMetadata().getTypeClass());
        assertEquals(true, type.getMetadata().isApplicationConfigured());
        assertEquals(false, type.getMetadata().isApplicationPreConfigured());
        assertEquals(false, type.getMetadata().isApplicationPreConfiguredStatic());

        String statementText = "select " +
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
                " from MyLegacyEvent#length(5)";

        EPStatement statement = epService.getEPAdministrator().createEPL(statementText);
        SupportUpdateListener listener = new SupportUpdateListener();
        statement.addListener(listener);

        EventType eventType = statement.getEventType();
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
        epService.getEPRuntime().sendEvent(legacyBean);

        assertEquals(legacyBean.fieldLegacyVal, listener.getLastNewData()[0].get("fieldSimple"));
        assertEquals(legacyBean.fieldStringArray, listener.getLastNewData()[0].get("fieldArr"));
        assertEquals(legacyBean.fieldStringArray[1], listener.getLastNewData()[0].get("fieldArrIndexed"));
        assertEquals(legacyBean.fieldMapped, listener.getLastNewData()[0].get("fieldMap"));
        assertEquals(legacyBean.fieldNested, listener.getLastNewData()[0].get("fieldNested"));
        assertEquals(legacyBean.fieldNested.readNestedValue(), listener.getLastNewData()[0].get("fieldNestedVal"));

        assertEquals(legacyBean.readLegacyBeanVal(), listener.getLastNewData()[0].get("simple"));
        assertEquals(legacyBean.readLegacyNested(), listener.getLastNewData()[0].get("nestedObject"));
        assertEquals(legacyBean.readLegacyNested().readNestedValue(), listener.getLastNewData()[0].get("nested"));
        assertEquals(legacyBean.readStringIndexed(0), listener.getLastNewData()[0].get("array"));
        assertEquals(legacyBean.readStringIndexed(1), listener.getLastNewData()[0].get("indexed"));
        assertEquals(legacyBean.readMapByKey("key1"), listener.getLastNewData()[0].get("mapped"));
        assertEquals(legacyBean.readMap(), listener.getLastNewData()[0].get("mapItself"));

        assertEquals(legacyBean.readLegacyBeanVal(), listener.getLastNewData()[0].get("explicitFSimple"));
        assertEquals(legacyBean.readLegacyBeanVal(), listener.getLastNewData()[0].get("explicitMSimple"));
        assertEquals(legacyBean.readLegacyNested(), listener.getLastNewData()[0].get("explicitFNested"));
        assertEquals(legacyBean.readStringIndexed(0), listener.getLastNewData()[0].get("explicitFIndexed[0]"));
        assertEquals(legacyBean.readStringIndexed(0), listener.getLastNewData()[0].get("explicitMArray[0]"));
        assertEquals(legacyBean.readStringIndexed(1), listener.getLastNewData()[0].get("explicitMIndexed[1]"));
        assertEquals(legacyBean.readMapByKey("key2"), listener.getLastNewData()[0].get("explicitMMapped('key2')"));

        EventTypeSPI stmtType = (EventTypeSPI) statement.getEventType();
        assertEquals(EventTypeMetadata.ApplicationType.MAP, stmtType.getMetadata().getOptionalApplicationType());
        assertEquals(null, stmtType.getMetadata().getOptionalSecondaryNames());
        assertNotNull(stmtType.getMetadata().getPrimaryName());
        assertNotNull(stmtType.getMetadata().getPublicName());
        assertNotNull(stmtType.getName());
        assertEquals(EventTypeMetadata.TypeClass.ANONYMOUS, stmtType.getMetadata().getTypeClass());
        assertEquals(false, stmtType.getMetadata().isApplicationConfigured());
        assertEquals(false, stmtType.getMetadata().isApplicationPreConfigured());
        assertEquals(false, stmtType.getMetadata().isApplicationPreConfiguredStatic());
    }
}

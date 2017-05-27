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

import com.espertech.esper.client.*;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportLegacyBean;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import static org.junit.Assert.assertEquals;

public class ExecEventBeanExplicitOnly implements RegressionExecution {
    private final boolean codegen;

    public ExecEventBeanExplicitOnly(boolean codegen) {
        this.codegen = codegen;
    }

    public void configure(Configuration configuration) throws Exception {
        ConfigurationEventTypeLegacy.CodeGeneration codeGeneration = codegen ? ConfigurationEventTypeLegacy.CodeGeneration.ENABLED : ConfigurationEventTypeLegacy.CodeGeneration.DISABLED;
        ConfigurationEventTypeLegacy legacyDef = new ConfigurationEventTypeLegacy();
        legacyDef.setAccessorStyle(ConfigurationEventTypeLegacy.AccessorStyle.EXPLICIT);
        legacyDef.setCodeGeneration(codeGeneration);
        legacyDef.addFieldProperty("explicitFNested", "fieldNested");
        legacyDef.addMethodProperty("explicitMNested", "readLegacyNested");
        configuration.addEventType("MyLegacyEvent", SupportLegacyBean.class.getName(), legacyDef);

        legacyDef = new ConfigurationEventTypeLegacy();
        legacyDef.setAccessorStyle(ConfigurationEventTypeLegacy.AccessorStyle.EXPLICIT);
        legacyDef.setCodeGeneration(codeGeneration);
        legacyDef.addFieldProperty("fieldNestedClassValue", "fieldNestedValue");
        legacyDef.addMethodProperty("readNestedClassValue", "readNestedValue");
        configuration.addEventType("MyLegacyNestedEvent", SupportLegacyBean.LegacyNested.class.getName(), legacyDef);

        legacyDef = new ConfigurationEventTypeLegacy();
        legacyDef.setAccessorStyle(ConfigurationEventTypeLegacy.AccessorStyle.EXPLICIT);
        legacyDef.setCodeGeneration(codeGeneration);
        configuration.addEventType("MySupportBean", SupportBean.class.getName(), legacyDef);
    }

    public void run(EPServiceProvider epService) throws Exception {
        String statementText = "select " +
                "explicitFNested.fieldNestedClassValue as fnested, " +
                "explicitMNested.readNestedClassValue as mnested" +
                " from MyLegacyEvent#length(5)";

        EPStatement statement = epService.getEPAdministrator().createEPL(statementText);
        SupportUpdateListener listener = new SupportUpdateListener();
        statement.addListener(listener);

        EventType eventType = statement.getEventType();
        assertEquals(String.class, eventType.getPropertyType("fnested"));
        assertEquals(String.class, eventType.getPropertyType("mnested"));

        SupportLegacyBean legacyBean = ExecEventBeanPublicAccessors.makeSampleEvent();
        epService.getEPRuntime().sendEvent(legacyBean);

        assertEquals(legacyBean.fieldNested.readNestedValue(), listener.getLastNewData()[0].get("fnested"));
        assertEquals(legacyBean.fieldNested.readNestedValue(), listener.getLastNewData()[0].get("mnested"));

        try {
            // invalid statement, JavaBean-style getters not exposed
            statementText = "select intPrimitive from MySupportBean#length(5)";
            epService.getEPAdministrator().createEPL(statementText);
        } catch (EPStatementException ex) {
            // expected
        }
    }
}

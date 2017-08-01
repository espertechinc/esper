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
import com.espertech.esper.supportregression.bean.SupportLegacyBeanInt;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import static org.junit.Assert.assertEquals;

public class ExecEventBeanJavaBeanAccessor implements RegressionExecution {
    private final boolean codegen;

    public ExecEventBeanJavaBeanAccessor(boolean codegen) {
        this.codegen = codegen;
    }

    public void configure(Configuration configuration) throws Exception {
        ConfigurationEventTypeLegacy legacyDef = new ConfigurationEventTypeLegacy();
        legacyDef.setAccessorStyle(ConfigurationEventTypeLegacy.AccessorStyle.JAVABEAN);
        legacyDef.setCodeGeneration(codegen ? ConfigurationEventTypeLegacy.CodeGeneration.ENABLED : ConfigurationEventTypeLegacy.CodeGeneration.DISABLED);
        legacyDef.addFieldProperty("explicitFInt", "fieldIntPrimitive");
        legacyDef.addMethodProperty("explicitMGetInt", "getIntPrimitive");
        legacyDef.addMethodProperty("explicitMReadInt", "readIntPrimitive");
        configuration.addEventType("MyLegacyEvent", SupportLegacyBeanInt.class.getName(), legacyDef);
    }

    public void run(EPServiceProvider epService) throws Exception {
        String statementText = "select intPrimitive, explicitFInt, explicitMGetInt, explicitMReadInt " +
                " from MyLegacyEvent#length(5)";

        EPStatement statement = epService.getEPAdministrator().createEPL(statementText);
        SupportUpdateListener listener = new SupportUpdateListener();
        statement.addListener(listener);
        EventType eventType = statement.getEventType();

        SupportLegacyBeanInt theEvent = new SupportLegacyBeanInt(10);
        epService.getEPRuntime().sendEvent(theEvent);

        for (String name : new String[]{"intPrimitive", "explicitFInt", "explicitMGetInt", "explicitMReadInt"}) {
            assertEquals(Integer.class, eventType.getPropertyType(name));
            assertEquals(10, listener.getLastNewData()[0].get(name));
        }
    }
}

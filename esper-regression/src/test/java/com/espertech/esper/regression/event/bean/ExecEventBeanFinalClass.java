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

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.ConfigurationEventTypeLegacy;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.supportregression.bean.SupportBeanFinal;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import static org.junit.Assert.assertEquals;

public class ExecEventBeanFinalClass implements RegressionExecution {
    private final boolean codegen;

    public ExecEventBeanFinalClass(boolean codegen) {
        this.codegen = codegen;
    }

    public void configure(Configuration configuration) throws Exception {
        ConfigurationEventTypeLegacy legacyDef = new ConfigurationEventTypeLegacy();
        legacyDef.setAccessorStyle(ConfigurationEventTypeLegacy.AccessorStyle.JAVABEAN);
        legacyDef.setCodeGeneration(codegen ? ConfigurationEventTypeLegacy.CodeGeneration.ENABLED : ConfigurationEventTypeLegacy.CodeGeneration.DISABLED);
        configuration.addEventType("MyFinalEvent", SupportBeanFinal.class.getName(), legacyDef);
    }

    public void run(EPServiceProvider epService) throws Exception {
        String statementText = "select intPrimitive " +
                "from " + SupportBeanFinal.class.getName() + "#length(5)";

        EPStatement statement = epService.getEPAdministrator().createEPL(statementText);
        SupportUpdateListener listener = new SupportUpdateListener();
        statement.addListener(listener);

        SupportBeanFinal theEvent = new SupportBeanFinal(10);
        epService.getEPRuntime().sendEvent(theEvent);
        assertEquals(10, listener.getLastNewData()[0].get("intPrimitive"));
    }
}

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
package com.espertech.esper.regression.expr.datetime;

import com.espertech.esper.client.ConfigurationEventTypeLegacy;
import com.espertech.esper.client.ConfigurationException;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportDateTime;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class ExecDTIntervalOpsInvalidConfig implements RegressionExecution {

    public void run(EPServiceProvider epService) throws Exception {
        ConfigurationEventTypeLegacy configBean = new ConfigurationEventTypeLegacy();

        configBean.setStartTimestampPropertyName(null);
        configBean.setEndTimestampPropertyName("caldate");
        tryInvalidConfig(epService, SupportDateTime.class, configBean, "Declared end timestamp property requires that a start timestamp property is also declared");

        configBean.setStartTimestampPropertyName("xyz");
        configBean.setEndTimestampPropertyName(null);
        tryInvalidConfig(epService, SupportBean.class, configBean, "Declared start timestamp property name 'xyz' was not found");

        configBean.setStartTimestampPropertyName("longPrimitive");
        configBean.setEndTimestampPropertyName("xyz");
        tryInvalidConfig(epService, SupportBean.class, configBean, "Declared end timestamp property name 'xyz' was not found");

        configBean.setEndTimestampPropertyName(null);
        configBean.setStartTimestampPropertyName("theString");
        tryInvalidConfig(epService, SupportBean.class, configBean, "Declared start timestamp property 'theString' is expected to return a Date, Calendar or long-typed value but returns 'java.lang.String'");

        configBean.setStartTimestampPropertyName("longPrimitive");
        configBean.setEndTimestampPropertyName("theString");
        tryInvalidConfig(epService, SupportBean.class, configBean, "Declared end timestamp property 'theString' is expected to return a Date, Calendar or long-typed value but returns 'java.lang.String'");

        configBean.setStartTimestampPropertyName("longdate");
        configBean.setEndTimestampPropertyName("caldate");
        tryInvalidConfig(epService, SupportDateTime.class, configBean, "Declared end timestamp property 'caldate' is expected to have the same property type as the start-timestamp property 'longdate'");
    }

    private void tryInvalidConfig(EPServiceProvider epService, Class clazz, ConfigurationEventTypeLegacy config, String message) {
        try {
            epService.getEPAdministrator().getConfiguration().addEventType(clazz.getName(), clazz.getName(), config);
            fail();
        } catch (ConfigurationException ex) {
            assertEquals(message, ex.getMessage());
        }
    }
}

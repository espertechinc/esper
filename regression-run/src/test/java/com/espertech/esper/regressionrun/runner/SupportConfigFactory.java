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
package com.espertech.esper.regressionrun.runner;

import com.espertech.esper.common.client.configuration.Configuration;
import com.espertech.esper.common.client.util.UndeployRethrowPolicy;
import com.espertech.esper.regressionlib.support.util.SupportExceptionHandlerFactoryRethrow;

import java.lang.reflect.Method;

public class SupportConfigFactory {
    private static final String TEST_CONFIG_FACTORY_CLASS = "CONFIGFACTORY_CLASS";
    private static final String SYSTEM_PROPERTY_LOG_CODE = "esper_logcode";

    public static Configuration getConfiguration() {
        Configuration config;
        String configFactoryClass = System.getProperty(TEST_CONFIG_FACTORY_CLASS);
        if (configFactoryClass != null) {
            try {
                Class clazz = Class.forName(configFactoryClass);
                Object instance = clazz.newInstance();
                Method m = clazz.getMethod("getConfigurationEsperRegression");
                Object result = m.invoke(instance);
                config = (Configuration) result;
            } catch (Exception e) {
                throw new RuntimeException("Error using configuration factory class '" + configFactoryClass + "'", e);
            }
        } else {
            config = new Configuration();
            config.getRuntime().getThreading().setInternalTimerEnabled(false);
            config.getRuntime().getExceptionHandling().addClass(SupportExceptionHandlerFactoryRethrow.class);
            config.getRuntime().getExceptionHandling().setUndeployRethrowPolicy(UndeployRethrowPolicy.RETHROW_FIRST);
            config.getCompiler().getByteCode().setAttachEPL(true);

            if (System.getProperty(SYSTEM_PROPERTY_LOG_CODE) != null) {
                config.getCompiler().getLogging().setEnableCode(true);
            }
        }
        return config;
    }
}

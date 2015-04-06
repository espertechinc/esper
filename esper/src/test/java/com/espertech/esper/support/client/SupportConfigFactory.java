/*
 * *************************************************************************************
 *  Copyright (C) 2006-2015 EsperTech, Inc. All rights reserved.                       *
 *  http://www.espertech.com/esper                                                     *
 *  http://www.espertech.com                                                           *
 *  ---------------------------------------------------------------------------------- *
 *  The software in this package is published under the terms of the GPL license       *
 *  a copy of which has been included with this distribution in the license.txt file.  *
 * *************************************************************************************
 */

package com.espertech.esper.support.client;

import com.espertech.esper.client.Configuration;

import java.lang.reflect.Method;

public class SupportConfigFactory
{
    private static final String TEST_CONFIG_FACTORY_CLASS = "CONFIGFACTORY_CLASS";

    public static Configuration getConfiguration()
    {
        Configuration config;
        String configFactoryClass = System.getProperty(TEST_CONFIG_FACTORY_CLASS);
        if (configFactoryClass != null)
        {
            try
            {
                Class clazz = Class.forName(configFactoryClass);
                Object instance = clazz.newInstance();
                Method m = clazz.getMethod("getConfigurationEsperRegression");
                Object result = m.invoke(instance);
                config = (Configuration) result;
            }
            catch (Exception e)
            {
                throw new RuntimeException("Error using configuration factory class '" + configFactoryClass + "'", e);
            }
        }
        else
        {
            config = new Configuration();
            config.getEngineDefaults().getThreading().setInternalTimerEnabled(false);
            config.getEngineDefaults().getExceptionHandling().addClass(SupportExceptionHandlerFactoryRethrow.class);
        }
        return config;
    }
}

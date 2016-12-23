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

package com.espertech.esper.regression.script;

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPServiceProviderManager;
import com.espertech.esper.client.EPStatementException;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.client.SupportConfigFactory;
import junit.framework.TestCase;

public class TestScriptExpressionConfiguration extends TestCase {

    public void testConfig() throws Exception {

        Configuration config = SupportConfigFactory.getConfiguration();
        config.getEngineDefaults().getScripts().setDefaultDialect("dummy");
        config.addEventType(SupportBean.class);
        EPServiceProvider engine = EPServiceProviderManager.getDefaultProvider(config);
        engine.initialize();

        try {
            engine.getEPAdministrator().createEPL("expression abc [10] select * from SupportBean");
            fail();
        }
        catch (EPStatementException ex) {
            assertEquals("Failed to obtain script engine for dialect 'dummy' for script 'abc' [expression abc [10] select * from SupportBean]", ex.getMessage());
        }
    }
}

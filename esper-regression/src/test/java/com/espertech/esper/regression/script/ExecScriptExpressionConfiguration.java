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
package com.espertech.esper.regression.script;

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatementException;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class ExecScriptExpressionConfiguration implements RegressionExecution {

    public void configure(Configuration configuration) throws Exception {
        configuration.getEngineDefaults().getScripts().setDefaultDialect("dummy");
        configuration.addEventType(SupportBean.class);
    }

    public void run(EPServiceProvider epService) throws Exception {
        try {
            epService.getEPAdministrator().createEPL("expression abc [10] select * from SupportBean");
            fail();
        } catch (EPStatementException ex) {
            assertEquals("Failed to obtain script engine for dialect 'dummy' for script 'abc' [expression abc [10] select * from SupportBean]", ex.getMessage());
        }
    }
}

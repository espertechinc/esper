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
package com.espertech.esper.regression.client;

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPException;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import static org.junit.Assert.fail;

public class ExecClientExceptionHandler implements RegressionExecution {
    public void configure(Configuration configuration) throws Exception {
        configuration.addEventType(SupportBean.class);
        configuration.addPlugInAggregationFunctionFactory("myinvalidagg", ExecClientExceptionHandlerNoHandler.InvalidAggTestFactory.class.getName());
    }

    public void run(EPServiceProvider epService) throws Exception {
        String epl = "@Name('ABCName') select myinvalidagg() from SupportBean";
        epService.getEPAdministrator().createEPL(epl);

        try {
            epService.getEPRuntime().sendEvent(new SupportBean());
            fail();
        } catch (EPException ex) {
            /* expected */
        }
    }
}

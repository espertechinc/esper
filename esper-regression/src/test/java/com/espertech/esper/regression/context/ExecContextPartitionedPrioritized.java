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
package com.espertech.esper.regression.context;

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ExecContextPartitionedPrioritized implements RegressionExecution {

    public void configure(Configuration configuration) throws Exception {
        configuration.addEventType("SupportBean", SupportBean.class);
        configuration.getEngineDefaults().getExecution().setPrioritized(true);
    }

    public void run(EPServiceProvider epService) throws Exception {
        epService.getEPAdministrator().createEPL(
                "create context SegmentedByMessage partition by theString from SupportBean");

        EPStatement statementWithDropAnnotation = epService.getEPAdministrator().createEPL(
                "@Drop @Priority(1) context SegmentedByMessage select 'test1' from SupportBean");
        SupportUpdateListener statementWithDropAnnotationListener = new SupportUpdateListener();
        statementWithDropAnnotation.addListener(statementWithDropAnnotationListener);

        EPStatement lowPriorityStatement = epService.getEPAdministrator().createEPL(
                "@Priority(0) context SegmentedByMessage select 'test2' from SupportBean");
        SupportUpdateListener lowPriorityStatementListener = new SupportUpdateListener();
        lowPriorityStatement.addListener(lowPriorityStatementListener);

        epService.getEPRuntime().sendEvent(new SupportBean("test msg", 1));

        assertTrue(statementWithDropAnnotationListener.isInvoked());
        assertFalse(lowPriorityStatementListener.isInvoked());
    }

}

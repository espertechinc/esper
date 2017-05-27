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
import com.espertech.esper.client.EPException;
import com.espertech.esper.client.EPOnDemandPreparedQueryParameterized;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportBean_S0;
import com.espertech.esper.supportregression.bean.SupportBean_S1;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import static org.junit.Assert.assertEquals;

public class ExecContextPartitionedNamedWindow implements RegressionExecution {

    public void configure(Configuration configuration) throws Exception {
        configuration.addEventType("SupportBean", SupportBean.class);
        configuration.addEventType("SupportBean_S0", SupportBean_S0.class);
        configuration.addEventType("SupportBean_S1", SupportBean_S1.class);
        configuration.getEngineDefaults().getLogging().setEnableExecutionDebug(true);
    }

    public void run(EPServiceProvider epService) throws Exception {
        epService.getEPAdministrator().createEPL("create context SegmentedByString partition by theString from SupportBean");

        epService.getEPAdministrator().createEPL("context SegmentedByString create window MyWindow#keepall as SupportBean");
        epService.getEPAdministrator().createEPL("context SegmentedByString insert into MyWindow select * from SupportBean");

        epService.getEPRuntime().sendEvent(new SupportBean("G1", 0));

        String expected = "Error executing statement: Named window 'MyWindow' is associated to context 'SegmentedByString' that is not available for querying without context partition selector, use the executeQuery(epl, selector) method instead [select * from MyWindow]";
        try {
            epService.getEPRuntime().executeQuery("select * from MyWindow");
        } catch (EPException ex) {
            assertEquals(expected, ex.getMessage());
        }

        EPOnDemandPreparedQueryParameterized prepared = epService.getEPRuntime().prepareQueryWithParameters("select * from MyWindow");
        try {
            epService.getEPRuntime().executeQuery(prepared);
        } catch (EPException ex) {
            assertEquals(expected, ex.getMessage());
        }
    }
}

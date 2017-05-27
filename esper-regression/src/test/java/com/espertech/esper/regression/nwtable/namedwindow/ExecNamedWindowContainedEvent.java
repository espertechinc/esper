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
package com.espertech.esper.regression.nwtable.namedwindow;

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatementException;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportBean_A;
import com.espertech.esper.supportregression.bean.bookexample.OrderBean;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class ExecNamedWindowContainedEvent implements RegressionExecution {
    public void configure(Configuration configuration) throws Exception {
        configuration.getEngineDefaults().getLogging().setEnableQueryPlan(true);
    }

    public void run(EPServiceProvider epService) throws Exception {
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean.class);
        epService.getEPAdministrator().getConfiguration().addEventType(OrderBean.class);

        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean_A", SupportBean_A.class);
        epService.getEPAdministrator().createEPL("create window OrderWindow#time(30) as OrderBean");

        try {
            String epl = "select * from SupportBean unidirectional, OrderWindow[books]";
            epService.getEPAdministrator().createEPL(epl);
            fail();
        } catch (EPStatementException ex) {
            assertEquals("Error starting statement: Failed to validate named window use in join, contained-event is only allowed for named windows when marked as unidirectional [select * from SupportBean unidirectional, OrderWindow[books]]", ex.getMessage());
        }

        try {
            String epl = "select *, (select bookId from OrderWindow[books] where sb.theString = bookId) " +
                    "from SupportBean sb";
            epService.getEPAdministrator().createEPL(epl);
            fail();
        } catch (EPStatementException ex) {
            assertEquals("Error starting statement: Failed to plan subquery number 1 querying OrderWindow: Failed to validate named window use in subquery, contained-event is only allowed for named windows when not correlated [select *, (select bookId from OrderWindow[books] where sb.theString = bookId) from SupportBean sb]", ex.getMessage());
        }
    }
}

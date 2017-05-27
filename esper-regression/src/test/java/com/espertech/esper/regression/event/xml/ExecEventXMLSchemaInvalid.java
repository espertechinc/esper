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
package com.espertech.esper.regression.event.xml;

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatementException;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import static com.espertech.esper.regression.event.xml.ExecEventXMLSchemaXPathBacked.getConfigTestType;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class ExecEventXMLSchemaInvalid implements RegressionExecution {
    public void configure(Configuration configuration) throws Exception {
        configuration.addEventType("TestXMLSchemaType", getConfigTestType(null, false));
    }

    public void run(EPServiceProvider epService) throws Exception {
        try {
            epService.getEPAdministrator().createEPL("select element1 from TestXMLSchemaType#length(100)");
            fail();
        } catch (EPStatementException ex) {
            assertEquals("Error starting statement: Failed to validate select-clause expression 'element1': Property named 'element1' is not valid in any stream [select element1 from TestXMLSchemaType#length(100)]", ex.getMessage());
        }
    }
}

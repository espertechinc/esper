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
import com.espertech.esper.client.ConfigurationEventTypeXMLDOM;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import javax.xml.xpath.XPathConstants;

public class ExecEventXMLNoSchemaVariableAndDotMethodResolution implements RegressionExecution {
    public void configure(Configuration configuration) throws Exception {
        configuration.addVariable("var", int.class, 0);

        ConfigurationEventTypeXMLDOM xmlDOMEventTypeDesc = new ConfigurationEventTypeXMLDOM();
        xmlDOMEventTypeDesc.setRootElementName("myevent");
        xmlDOMEventTypeDesc.addXPathProperty("xpathAttrNum", "/myevent/@attrnum", XPathConstants.STRING, "long");
        xmlDOMEventTypeDesc.addXPathProperty("xpathAttrNumTwo", "/myevent/@attrnumtwo", XPathConstants.STRING, "long");
        configuration.addEventType("TestXMLNoSchemaType", xmlDOMEventTypeDesc);
    }

    public void run(EPServiceProvider epService) throws Exception {
        String stmtTextOne = "select var, xpathAttrNum.after(xpathAttrNumTwo) from TestXMLNoSchemaType#length(100)";
        epService.getEPAdministrator().createEPL(stmtTextOne);
    }
}

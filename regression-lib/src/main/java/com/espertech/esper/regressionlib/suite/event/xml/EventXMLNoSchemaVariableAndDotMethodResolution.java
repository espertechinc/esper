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
package com.espertech.esper.regressionlib.suite.event.xml;

import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;

public class EventXMLNoSchemaVariableAndDotMethodResolution implements RegressionExecution {

    public void run(RegressionEnvironment env) {
        String stmtTextOne = "select var, xpathAttrNum.after(xpathAttrNumTwo) from TestXMLNoSchemaTypeWNum#length(100)";
        env.compileDeploy(stmtTextOne).undeployAll();
    }
}

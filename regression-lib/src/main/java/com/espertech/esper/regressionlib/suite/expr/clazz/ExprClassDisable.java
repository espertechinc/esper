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
package com.espertech.esper.regressionlib.suite.expr.clazz;

import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;



public class ExprClassDisable implements RegressionExecution {

    public void run(RegressionEnvironment env) {
        String epl = "@name('s0') inlined_class \"\"\"\n" +
            "    public class MyClass {}\n" +
            "\"\"\" " +
            "select * from SupportBean\n";
        env.tryInvalidCompile(epl, "Inlined-class compilation has been disabled by configuration");
    }
}

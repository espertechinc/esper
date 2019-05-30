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
package com.espertech.esper.regressionlib.suite.client.compile;

import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;

import java.util.ArrayList;
import java.util.List;

public class ClientCompileLarge {

    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new ClientCompileLargeConstantPoolDueToMethods());
        return execs;
    }

    public static class ClientCompileLargeConstantPoolDueToMethods implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            StringBuilder buf = new StringBuilder();
            buf.append("select ");
            String delimiter = "";
            for (int i = 0; i < 1000; i++) {
                buf.append(delimiter);
                buf.append("((((((((((((((((((((((((1+1)+1)+1)+1)+1)+1)+1)+1)+1)+1)+1)+1)+1)+1)+1)+1)+1)+1)+1)+1)+1)+1)+1)+1)+1");
                buf.append(" as z" + i);
                delimiter = ",";
            }
            buf.append(" from SupportBean");

            env.compile(buf.toString());
        }
    }
}

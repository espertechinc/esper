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
package com.espertech.esper.regressionlib.suite.client.basic;

import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;

public class ClientBasicSelect implements RegressionExecution {
    public void run(RegressionEnvironment env) {
        String epl = "@name('s0') select * from SupportBean";
        env.compileDeployAddListenerMileZero(epl, "s0");

        env.sendEventBean(new SupportBean())
            .listener("s0").assertInvokedAndReset();
        env.milestone(1);

        env.undeployAll();
    }
}

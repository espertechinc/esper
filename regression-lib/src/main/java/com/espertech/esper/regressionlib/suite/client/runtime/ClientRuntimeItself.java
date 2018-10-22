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
package com.espertech.esper.regressionlib.suite.client.runtime;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.support.bean.SupportBean;
import com.espertech.esper.runtime.client.EPRuntime;
import com.espertech.esper.runtime.client.EPStatement;
import com.espertech.esper.runtime.client.UpdateListener;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class ClientRuntimeItself {
    public final static String TEST_SERVICE_NAME = "TEST_SERVICE_NAME";
    public final static int TEST_SECRET_VALUE = 12345;

    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new ClientRuntimeItselfTransientConfiguration());
        return execs;
    }

    private static class ClientRuntimeItselfTransientConfiguration implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            env.compileDeploy("@name('s0') select * from SupportBean");
            MyListener listener = new MyListener();
            env.statement("s0").addListener(listener);

            env.sendEventBean(new SupportBean());
            assertEquals(TEST_SECRET_VALUE, listener.getSecretValue());

            env.undeployAll();
        }
    }

    public static class MyLocalService {
        private final int secretValue;

        public MyLocalService(int secretValue) {
            this.secretValue = secretValue;
        }

        int getSecretValue() {
            return secretValue;
        }
    }

    public static class MyListener implements UpdateListener {
        private int secretValue;

        public void update(EventBean[] newEvents, EventBean[] oldEvents, EPStatement statement, EPRuntime runtime) {
            MyLocalService svc = (MyLocalService) runtime.getConfigurationTransient().get(TEST_SERVICE_NAME);
            secretValue = svc.getSecretValue();
        }

        int getSecretValue() {
            return secretValue;
        }
    }
}

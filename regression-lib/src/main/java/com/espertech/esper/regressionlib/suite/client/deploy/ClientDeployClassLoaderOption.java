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
package com.espertech.esper.regressionlib.suite.client.deploy;

import com.espertech.esper.common.client.EPCompiled;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.runtime.client.DeploymentOptions;
import com.espertech.esper.runtime.client.EPDeployException;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

public class ClientDeployClassLoaderOption {

    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new ClientDeployClassLoaderOptionSimple());
        return execs;
    }

    private static class ClientDeployClassLoaderOptionSimple implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select * from SupportBean";
            EPCompiled compiled = env.compile(epl);
            DeploymentOptions options = new DeploymentOptions();
            MySupportClassloader mySupportClassloader = new MySupportClassloader();
            options.setDeploymentClassLoaderOption(clcontext -> mySupportClassloader);

            try {
                env.deployment().deploy(compiled, options);
            } catch (EPDeployException e) {
                fail(e.getMessage());
            }

            assertFalse(mySupportClassloader.names.isEmpty());

            env.undeployAll();
        }
    }

    public static class MySupportClassloader extends ClassLoader {

        private final List<String> names = new ArrayList<>();

        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException {
            names.add(name);
            return super.findClass(name);
        }

        public List<String> getNames() {
            return names;
        }
    }
}

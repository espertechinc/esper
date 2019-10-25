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
import com.espertech.esper.common.client.EPException;
import com.espertech.esper.common.internal.util.FileUtil;
import com.espertech.esper.compiler.client.util.EPCompiledIOUtil;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.runtime.client.EPDeployDeploymentVersionException;
import com.espertech.esper.runtime.client.EPDeployException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.espertech.esper.regressionlib.framework.SupportMessageAssertUtil.assertMessage;
import static org.junit.Assert.fail;

public class ClientDeployVersion {
    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new ClientDeployVersionMinorCheck());
        return execs;
    }

    private static class ClientDeployVersionMinorCheck implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String filename = "regression/epcompiled_version_8.0.0.epl_jar_for_deployment";
            String file = FileUtil.findClasspathFile(filename);
            if (file == null) {
                throw new RuntimeException("Failed to find file " + filename);
            }

            EPCompiled compiled;
            try {
                compiled = EPCompiledIOUtil.read(new File(file));
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }

            try {
                env.runtime().getDeploymentService().deploy(compiled);
                fail();
            } catch (EPDeployDeploymentVersionException ex) {
                assertMessage(ex, "Major or minor version of compiler and runtime mismatch; The runtime version is 8.4.0 and the compiler version of the compiled unit is 8.0.0");
            } catch (EPDeployException ex) {
                throw new RuntimeException(ex);
            }

            try {
                env.runtime().getFireAndForgetService().executeQuery(compiled);
                fail();
            } catch (EPException ex) {
                assertMessage(ex, "Major or minor version of compiler and runtime mismatch; The runtime version is 8.4.0 and the compiler version of the compiled unit is 8.0.0");
            }
        }
    }
}

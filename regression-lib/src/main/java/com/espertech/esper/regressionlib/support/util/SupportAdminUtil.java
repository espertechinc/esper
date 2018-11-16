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
package com.espertech.esper.regressionlib.support.util;

import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.runtime.client.EPRuntime;
import com.espertech.esper.runtime.client.EPStatement;
import com.espertech.esper.runtime.client.UpdateListener;
import com.espertech.esper.runtime.client.scopetest.SupportListener;
import com.espertech.esper.runtime.internal.kernel.service.DeploymentInternal;
import com.espertech.esper.runtime.internal.kernel.service.EPDeploymentServiceSPI;
import com.espertech.esper.runtime.internal.kernel.statement.EPStatementSPI;

import java.util.Iterator;
import java.util.Map;

import static com.espertech.esper.common.client.scopetest.ScopeTestHelper.fail;
import static org.junit.Assert.assertEquals;

public class SupportAdminUtil {
    public static void assertStatelessStmt(RegressionEnvironment env, String stmtname, boolean flag) {
        EPStatementSPI stmt = (EPStatementSPI) getRequireStatement(stmtname, env.runtime());
        assertEquals(flag, stmt.getStatementContext().isStatelessSelect());
    }

    public static SupportListener getRequireStatementListener(String statementName, EPRuntime runtime) {
        EPStatement statement = getRequireStatement(statementName, runtime);
        if (statement == null) {
            fail("Statement by name '" + statementName + "' not found");
        }
        Iterator<UpdateListener> it = statement.getUpdateListeners();
        if (!it.hasNext()) {
            fail("Statement by name '" + statementName + "' no listener attached");
        }
        UpdateListener first = it.next();
        if (!(first instanceof SupportListener)) {
            fail("Statement by name '" + statementName + "' expected listener " + SupportListener.class.getName() + " but received " + first.getClass().getName());
        }
        if (it.hasNext()) {
            fail("Statement by name '" + statementName + "' has multiple listeners");
        }
        return (SupportListener) first;
    }

    public static EPStatement getRequireStatement(String statementName, EPRuntime runtime) {
        EPStatement found = getStatement(statementName, runtime);
        if (found == null) {
            throw new IllegalArgumentException("Failed to find statements '" + statementName + "'");
        }
        return found;
    }

    public static EPStatement getStatement(String statementName, EPRuntime runtime) {
        EPDeploymentServiceSPI spi = (EPDeploymentServiceSPI) runtime.getDeploymentService();
        EPStatement found = null;
        for (Map.Entry<String, DeploymentInternal> entry : spi.getDeploymentMap().entrySet()) {
            EPStatement[] statements = entry.getValue().getStatements();
            for (EPStatement stmt : statements) {
                if (statementName.equals(stmt.getName())) {
                    if (found != null) {
                        throw new IllegalArgumentException("Found multiple statements of name '" + statementName + "', statement name is unique within a deployment only");
                    }
                    found = stmt;
                }
            }
        }
        return found;
    }
}

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

import com.espertech.esper.common.internal.epl.namedwindow.core.NamedWindow;
import com.espertech.esper.common.internal.epl.namedwindow.core.NamedWindowInstance;
import com.espertech.esper.common.internal.epl.namedwindow.core.NamedWindowManagementService;
import com.espertech.esper.common.internal.epl.table.core.Table;
import com.espertech.esper.common.internal.epl.table.core.TableInstance;
import com.espertech.esper.common.internal.epl.table.core.TableManagementService;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.runtime.internal.kernel.service.EPRuntimeSPI;

import static org.junit.Assert.fail;

public class SupportInfraUtil {
    public static int getIndexCountNoContext(RegressionEnvironment env, boolean namedWindow, String infraStatementName, String infraName) {
        if (namedWindow) {
            NamedWindowInstance instance = getInstanceNoContextNW(env, infraStatementName, infraName);
            return instance.getIndexDescriptors().length;
        }
        TableInstance instance = getInstanceNoContextTable(env, infraStatementName, infraName);
        return instance.getIndexRepository().getIndexDescriptors().length;
    }

    public static long getDataWindowCountNoContext(RegressionEnvironment env, String statementNameNamedWindow, String windowName) {
        NamedWindowInstance instance = getInstanceNoContextNW(env, statementNameNamedWindow, windowName);
        return instance.getCountDataWindow();
    }

    public static NamedWindowInstance getInstanceNoContextNW(RegressionEnvironment env, String statementNameNamedWindow, String windowName) {
        NamedWindow namedWindow = getNamedWindow(env, statementNameNamedWindow, windowName);
        return namedWindow.getNamedWindowInstance(null);
    }

    public static TableInstance getInstanceNoContextTable(RegressionEnvironment env, String statementNameTable, String tableName) {
        Table table = getTable(env, statementNameTable, tableName);
        return table.getTableInstance(-1);
    }

    public static NamedWindow getNamedWindow(RegressionEnvironment env, String statementNameNamedWindow, String windowName) {
        EPRuntimeSPI spi = (EPRuntimeSPI) env.runtime();
        NamedWindowManagementService namedWindowManagementService = spi.getServicesContext().getNamedWindowManagementService();
        String deploymentId = env.deploymentId(statementNameNamedWindow);
        NamedWindow namedWindow = namedWindowManagementService.getNamedWindow(deploymentId, windowName);
        if (namedWindow == null) {
            fail("Failed to find statement-name '" + statementNameNamedWindow + "' named window '" + windowName + "'");
        }
        return namedWindow;
    }

    public static Table getTable(RegressionEnvironment env, String statementNameTable, String tableName) {
        EPRuntimeSPI spi = (EPRuntimeSPI) env.runtime();
        TableManagementService tables = spi.getServicesContext().getTableManagementService();
        String deploymentId = env.deploymentId(statementNameTable);
        Table table = tables.getTable(deploymentId, tableName);
        if (table == null) {
            fail("Failed to find statement-name '" + statementNameTable + "' table '" + tableName + "'");
        }
        return table;
    }
}

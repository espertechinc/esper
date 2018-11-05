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
package com.espertech.esper.common.internal.epl.table.core;

import com.espertech.esper.common.internal.context.module.EPStatementInitServices;
import com.espertech.esper.common.internal.epl.table.compiletime.TableMetaData;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

public abstract class TableManagementServiceBase implements TableManagementService {
    private final TableExprEvaluatorContext tableExprEvaluatorContext;
    private final Map<String, TableDeployment> deployments = new HashMap<>();

    public TableManagementServiceBase(TableExprEvaluatorContext tableExprEvaluatorContext) {
        this.tableExprEvaluatorContext = tableExprEvaluatorContext;
    }

    public TableExprEvaluatorContext getTableExprEvaluatorContext() {
        return tableExprEvaluatorContext;
    }

    public void addTable(String tableName, TableMetaData tableMetaData, EPStatementInitServices services) {
        TableDeployment deployment = deployments.get(services.getDeploymentId());
        if (deployment == null) {
            deployment = new TableDeployment();
            deployments.put(services.getDeploymentId(), deployment);
        }
        deployment.add(tableName, tableMetaData, services);
    }

    public Table getTable(String deploymentId, String tableName) {
        TableDeployment deployment = deployments.get(deploymentId);
        return deployment == null ? null : deployment.getTable(tableName);
    }

    public void destroyTable(String deploymentId, String tableName) {
        TableDeployment deployment = deployments.get(deploymentId);
        if (deployment == null) {
            return;
        }
        deployment.remove(tableName);
        if (deployment.isEmpty()) {
            deployments.remove(deploymentId);
        }
    }

    public int getDeploymentCount() {
        return deployments.size();
    }

    public void traverseTables(BiConsumer<String, Table> consumer) {
        for (Map.Entry<String, TableDeployment> entry : deployments.entrySet()) {
            for (Map.Entry<String, Table> table : entry.getValue().getTables().entrySet()) {
                consumer.accept(entry.getKey(), table.getValue());
            }
        }
    }
}

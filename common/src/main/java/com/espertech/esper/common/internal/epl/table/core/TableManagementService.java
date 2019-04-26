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
import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.epl.table.compiletime.TableMetaData;

import java.util.function.BiConsumer;

/**
 * Service to manage named windows on an runtime level.
 */
public interface TableManagementService {
    void addTable(String tableName, TableMetaData tableMetaData, EPStatementInitServices services);

    Table getTable(String deploymentId, String tableName);

    int getDeploymentCount();

    void destroyTable(String deploymentId, String tableName);

    Table allocateTable(TableMetaData metadata);

    TableInstance allocateTableInstance(Table table, AgentInstanceContext agentInstanceContext);

    TableExprEvaluatorContext getTableExprEvaluatorContext();

    void traverseTables(BiConsumer<String, Table> consumer);
}

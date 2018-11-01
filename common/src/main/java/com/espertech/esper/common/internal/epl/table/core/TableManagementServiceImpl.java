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

import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.context.util.StatementContext;
import com.espertech.esper.common.internal.epl.table.compiletime.TableMetaData;
import com.espertech.esper.common.client.serde.DataInputOutputSerde;

public class TableManagementServiceImpl extends TableManagementServiceBase {

    public TableManagementServiceImpl(TableExprEvaluatorContext tableExprEvaluatorContext) {
        super(tableExprEvaluatorContext);
    }

    public Table allocateTable(TableMetaData metadata) {
        return new TableImpl(metadata);
    }

    public TableSerdes getTableSerdes(Table table, DataInputOutputSerde aggregationSerde, StatementContext statementContext) {
        return null;    // this implementation does not require serdes
    }

    public TableInstance allocateTableInstance(Table table, AgentInstanceContext agentInstanceContext) {
        if (!table.getMetaData().isKeyed()) {
            return new TableInstanceUngroupedImpl(table, agentInstanceContext);
        }
        return new TableInstanceGroupedImpl(table, agentInstanceContext);
    }
}

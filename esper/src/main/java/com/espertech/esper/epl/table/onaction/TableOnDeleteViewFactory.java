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
package com.espertech.esper.epl.table.onaction;

import com.espertech.esper.core.context.util.AgentInstanceContext;
import com.espertech.esper.core.service.StatementResultService;
import com.espertech.esper.epl.core.resultset.core.ResultSetProcessor;
import com.espertech.esper.epl.lookup.SubordWMatchExprLookupStrategy;
import com.espertech.esper.epl.table.mgmt.TableMetadata;
import com.espertech.esper.epl.table.mgmt.TableStateInstance;

public class TableOnDeleteViewFactory implements TableOnViewFactory {
    private final StatementResultService statementResultService;
    private final TableMetadata tableMetadata;

    public TableOnDeleteViewFactory(StatementResultService statementResultService, TableMetadata tableMetadata) {
        this.statementResultService = statementResultService;
        this.tableMetadata = tableMetadata;
    }

    public TableOnViewBase make(SubordWMatchExprLookupStrategy lookupStrategy, TableStateInstance tableState, AgentInstanceContext agentInstanceContext, ResultSetProcessor resultSetProcessor) {
        return new TableOnDeleteView(lookupStrategy, tableState, agentInstanceContext, tableMetadata, this);
    }

    public StatementResultService getStatementResultService() {
        return statementResultService;
    }

    public TableMetadata getTableMetadata() {
        return tableMetadata;
    }
}

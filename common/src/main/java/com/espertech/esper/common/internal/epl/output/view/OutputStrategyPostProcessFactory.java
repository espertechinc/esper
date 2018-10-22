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
package com.espertech.esper.common.internal.epl.output.view;

import com.espertech.esper.common.internal.compile.stage1.spec.SelectClauseStreamSelectorEnum;
import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.epl.table.core.Table;
import com.espertech.esper.common.internal.epl.table.core.TableInstance;

/**
 * An output strategy that handles routing (insert-into) and stream selection.
 */
public class OutputStrategyPostProcessFactory {
    private final boolean isRoute;
    private final SelectClauseStreamSelectorEnum insertIntoStreamSelector;
    private final SelectClauseStreamSelectorEnum selectStreamDirEnum;
    private final boolean addToFront;
    private final Table optionalTable;

    public OutputStrategyPostProcessFactory(boolean isRoute, SelectClauseStreamSelectorEnum insertIntoStreamSelector, SelectClauseStreamSelectorEnum selectStreamDirEnum, boolean addToFront, Table optionalTable) {
        this.isRoute = isRoute;
        this.insertIntoStreamSelector = insertIntoStreamSelector;
        this.selectStreamDirEnum = selectStreamDirEnum;
        this.addToFront = addToFront;
        this.optionalTable = optionalTable;
    }

    public OutputStrategyPostProcess make(AgentInstanceContext agentInstanceContext) {
        TableInstance tableInstance = null;
        if (optionalTable != null) {
            tableInstance = optionalTable.getTableInstance(agentInstanceContext.getAgentInstanceId());
        }
        return new OutputStrategyPostProcess(this, agentInstanceContext, tableInstance);
    }

    public boolean isRoute() {
        return isRoute;
    }

    public SelectClauseStreamSelectorEnum getInsertIntoStreamSelector() {
        return insertIntoStreamSelector;
    }

    public SelectClauseStreamSelectorEnum getSelectStreamDirEnum() {
        return selectStreamDirEnum;
    }

    public boolean isAddToFront() {
        return addToFront;
    }
}

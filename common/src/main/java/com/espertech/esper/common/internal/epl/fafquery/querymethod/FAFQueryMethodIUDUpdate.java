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
package com.espertech.esper.common.internal.epl.fafquery.querymethod;

import com.espertech.esper.common.client.EPException;
import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;
import com.espertech.esper.common.internal.epl.expression.table.ExprTableAccessNode;
import com.espertech.esper.common.internal.epl.fafquery.processor.FireAndForgetInstance;
import com.espertech.esper.common.internal.epl.join.querygraph.QueryGraph;
import com.espertech.esper.common.internal.epl.table.core.Table;
import com.espertech.esper.common.internal.epl.table.update.TableUpdateStrategy;
import com.espertech.esper.common.internal.epl.table.update.TableUpdateStrategyFactory;
import com.espertech.esper.common.internal.epl.updatehelper.EventBeanUpdateHelperNoCopy;
import com.espertech.esper.common.internal.epl.updatehelper.EventBeanUpdateHelperWCopy;

public class FAFQueryMethodIUDUpdate extends FAFQueryMethodIUDBase {
    private QueryGraph queryGraph;
    private ExprEvaluator optionalWhereClause;
    private EventBeanUpdateHelperWCopy updateHelperNamedWindow;
    private EventBeanUpdateHelperNoCopy updateHelperTable;
    private TableUpdateStrategy tableUpdateStrategy;
    private ExprTableAccessNode[] optionalTableNodes;
    private Table table;

    public void setQueryGraph(QueryGraph queryGraph) {
        this.queryGraph = queryGraph;
    }

    public void setOptionalWhereClause(ExprEvaluator optionalWhereClause) {
        this.optionalWhereClause = optionalWhereClause;
    }

    public void setUpdateHelperNamedWindow(EventBeanUpdateHelperWCopy updateHelperNamedWindow) {
        this.updateHelperNamedWindow = updateHelperNamedWindow;
    }

    public void setUpdateHelperTable(EventBeanUpdateHelperNoCopy updateHelperTable) {
        this.updateHelperTable = updateHelperTable;
    }

    public void setOptionalTableNodes(ExprTableAccessNode[] optionalTableNodes) {
        this.optionalTableNodes = optionalTableNodes;
    }

    public void setTable(Table table) {
        this.table = table;
        try {
            this.tableUpdateStrategy = TableUpdateStrategyFactory.validateGetTableUpdateStrategy(table.getMetaData(), updateHelperTable, false);
        } catch (ExprValidationException e) {
            throw new EPException(e.getMessage(), e);
        }
    }

    public EventBean[] execute(FireAndForgetInstance fireAndForgetProcessorInstance) {
        return fireAndForgetProcessorInstance.processUpdate(this);
    }

    public ExprEvaluator getOptionalWhereClause() {
        return optionalWhereClause;
    }

    public EventBeanUpdateHelperWCopy getUpdateHelperNamedWindow() {
        return updateHelperNamedWindow;
    }

    public ExprTableAccessNode[] getOptionalTableNodes() {
        return optionalTableNodes;
    }

    public QueryGraph getQueryGraph() {
        return queryGraph;
    }

    public EventBeanUpdateHelperNoCopy getUpdateHelperTable() {
        return updateHelperTable;
    }

    public TableUpdateStrategy getTableUpdateStrategy() {
        return tableUpdateStrategy;
    }
}

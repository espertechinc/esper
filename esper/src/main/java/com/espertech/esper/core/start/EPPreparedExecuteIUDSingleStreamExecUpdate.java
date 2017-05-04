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
package com.espertech.esper.core.start;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.core.service.EPServicesContext;
import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.epl.expression.table.ExprTableAccessNode;
import com.espertech.esper.epl.join.plan.QueryGraph;
import com.espertech.esper.epl.table.upd.TableUpdateStrategy;
import com.espertech.esper.epl.updatehelper.EventBeanUpdateHelper;

import java.lang.annotation.Annotation;

public class EPPreparedExecuteIUDSingleStreamExecUpdate implements EPPreparedExecuteIUDSingleStreamExec {
    private final QueryGraph queryGraph;
    private final ExprNode optionalWhereClause;
    private final Annotation[] annotations;
    private final EventBeanUpdateHelper updateHelper;
    private final TableUpdateStrategy tableUpdateStrategy;
    private final ExprTableAccessNode[] optionalTableNodes;
    private final EPServicesContext services;

    public EPPreparedExecuteIUDSingleStreamExecUpdate(QueryGraph queryGraph, ExprNode optionalWhereClause, Annotation[] annotations, EventBeanUpdateHelper updateHelper, TableUpdateStrategy tableUpdateStrategy, ExprTableAccessNode[] optionalTableNodes, EPServicesContext services) {
        this.queryGraph = queryGraph;
        this.optionalWhereClause = optionalWhereClause;
        this.annotations = annotations;
        this.updateHelper = updateHelper;
        this.tableUpdateStrategy = tableUpdateStrategy;
        this.optionalTableNodes = optionalTableNodes;
        this.services = services;
    }

    public EventBean[] execute(FireAndForgetInstance fireAndForgetProcessorInstance) {
        return fireAndForgetProcessorInstance.processUpdate(this);
    }

    public ExprNode getOptionalWhereClause() {
        return optionalWhereClause;
    }

    public Annotation[] getAnnotations() {
        return annotations;
    }

    public EventBeanUpdateHelper getUpdateHelper() {
        return updateHelper;
    }

    public TableUpdateStrategy getTableUpdateStrategy() {
        return tableUpdateStrategy;
    }

    public ExprTableAccessNode[] getOptionalTableNodes() {
        return optionalTableNodes;
    }

    public EPServicesContext getServices() {
        return services;
    }

    public QueryGraph getQueryGraph() {
        return queryGraph;
    }
}

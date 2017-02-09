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
import com.espertech.esper.filter.FilterSpecCompiled;

import java.lang.annotation.Annotation;

public class EPPreparedExecuteIUDSingleStreamExecDelete implements EPPreparedExecuteIUDSingleStreamExec {
    private final FilterSpecCompiled filter;
    private final ExprNode optionalWhereClause;
    private final Annotation[] annotations;
    private final ExprTableAccessNode[] optionalTableNodes;
    private final EPServicesContext services;

    public EPPreparedExecuteIUDSingleStreamExecDelete(FilterSpecCompiled filter, ExprNode optionalWhereClause, Annotation[] annotations, ExprTableAccessNode[] optionalTableNodes, EPServicesContext services) {
        this.filter = filter;
        this.optionalWhereClause = optionalWhereClause;
        this.annotations = annotations;
        this.optionalTableNodes = optionalTableNodes;
        this.services = services;
    }

    public EventBean[] execute(FireAndForgetInstance fireAndForgetProcessorInstance) {
        return fireAndForgetProcessorInstance.processDelete(this);
    }

    public FilterSpecCompiled getFilter() {
        return filter;
    }

    public ExprNode getOptionalWhereClause() {
        return optionalWhereClause;
    }

    public Annotation[] getAnnotations() {
        return annotations;
    }

    public ExprTableAccessNode[] getOptionalTableNodes() {
        return optionalTableNodes;
    }

    public EPServicesContext getServices() {
        return services;
    }
}

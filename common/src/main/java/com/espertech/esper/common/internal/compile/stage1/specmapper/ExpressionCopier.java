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
package com.espertech.esper.common.internal.compile.stage1.specmapper;

import com.espertech.esper.common.client.soda.Expression;
import com.espertech.esper.common.internal.compile.stage1.spec.StatementSpecRaw;
import com.espertech.esper.common.internal.compile.stage3.StatementCompileTimeServices;
import com.espertech.esper.common.internal.context.compile.ContextCompileTimeDescriptor;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.epl.expression.visitor.ExprNodeSubselectDeclaredDotVisitor;

public class ExpressionCopier {
    private final StatementSpecRaw statementSpecRaw;
    private final ContextCompileTimeDescriptor contextCompileTimeDescriptor;
    private final StatementCompileTimeServices services;
    private final ExprNodeSubselectDeclaredDotVisitor visitor;

    public ExpressionCopier(StatementSpecRaw statementSpecRaw, ContextCompileTimeDescriptor contextCompileTimeDescriptor, StatementCompileTimeServices services, ExprNodeSubselectDeclaredDotVisitor visitor) {
        this.statementSpecRaw = statementSpecRaw;
        this.contextCompileTimeDescriptor = contextCompileTimeDescriptor;
        this.services = services;
        this.visitor = visitor;
    }

    public ExprNode copy(ExprNode exprNode) {
        Expression expression = StatementSpecMapper.unmap(exprNode);
        StatementSpecMapEnv mapEnv = services.getStatementSpecMapEnv();
        StatementSpecMapContext mapContext = new StatementSpecMapContext(contextCompileTimeDescriptor, mapEnv);
        ExprNode copy = StatementSpecMapper.mapExpression(expression, mapContext);

        statementSpecRaw.getTableExpressions().addAll(mapContext.getTableExpressions());
        copy.accept(visitor);

        return copy;
    }
}

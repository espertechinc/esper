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
package com.espertech.esper.common.internal.compile.stage2;

import com.espertech.esper.common.client.configuration.compiler.ConfigurationCompilerPlugInSingleRowFunction;
import com.espertech.esper.common.internal.epl.enummethod.dot.ExprLambdaGoesNode;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.epl.expression.core.ExprStreamRefNode;
import com.espertech.esper.common.internal.epl.expression.core.ExprWildcard;
import com.espertech.esper.common.internal.epl.expression.declared.compiletime.ExprDeclaredNode;
import com.espertech.esper.common.internal.epl.expression.funcs.ExprPlugInSingleRowNode;
import com.espertech.esper.common.internal.epl.expression.subquery.ExprSubselectNode;
import com.espertech.esper.common.internal.epl.expression.table.ExprTableAccessNode;
import com.espertech.esper.common.internal.epl.expression.variable.ExprVariableNode;
import com.espertech.esper.common.internal.epl.expression.visitor.ExprNodeVisitor;
import com.espertech.esper.common.internal.epl.script.core.ExprNodeScript;

public class FilterSpecExprNodeVisitorValueLimitedExpr implements ExprNodeVisitor {
    private boolean limited = true;

    public boolean isVisit(ExprNode exprNode) {
        return limited;
    }

    public void visit(ExprNode exprNode) {
        if (exprNode instanceof ExprStreamRefNode) {
            ExprStreamRefNode streamRefNode = (ExprStreamRefNode) exprNode;
            Integer stream = streamRefNode.getStreamReferencedIfAny();
            if (stream != null) {
                if (stream == 0) {
                    limited = false;
                }
            }
        }

        if (exprNode instanceof ExprVariableNode) {
            ExprVariableNode node = (ExprVariableNode) exprNode;
            if (!node.getVariableMetadata().isConstant()) {
                limited = false;
            }
        } else if (exprNode instanceof ExprTableAccessNode ||
            exprNode instanceof ExprSubselectNode ||
            exprNode instanceof ExprLambdaGoesNode ||
            exprNode instanceof ExprWildcard ||
            exprNode instanceof ExprNodeScript ||
            exprNode instanceof ExprDeclaredNode) {
            limited = false;
        } else if (exprNode instanceof ExprPlugInSingleRowNode) {
            ExprPlugInSingleRowNode plugIn = (ExprPlugInSingleRowNode) exprNode;
            if (plugIn.getConfig() != null && plugIn.getConfig().getFilterOptimizable() == ConfigurationCompilerPlugInSingleRowFunction.FilterOptimizable.DISABLED) {
                limited = false;
            }
        }
    }

    public boolean isLimited() {
        return limited;
    }
}

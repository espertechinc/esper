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
package com.espertech.esper.common.internal.epl.output.polled;

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionField;
import com.espertech.esper.common.internal.compile.stage1.spec.OnTriggerSetAssignment;
import com.espertech.esper.common.internal.compile.stage3.StatementCompileTimeServices;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;
import com.espertech.esper.common.internal.context.module.EPStatementInitServices;
import com.espertech.esper.common.internal.epl.expression.core.ExprForge;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.epl.expression.core.ExprNodeUtilityCodegen;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;
import com.espertech.esper.common.internal.epl.expression.visitor.ExprNodeIdentifierVisitor;
import com.espertech.esper.common.internal.epl.variable.core.VariableReadWritePackage;
import com.espertech.esper.common.internal.epl.variable.core.VariableReadWritePackageForge;

import java.util.List;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

/**
 * Output condition for output rate limiting that handles when-then expressions for controlling output.
 */
public class OutputConditionPolledExpressionFactoryForge implements OutputConditionPolledFactoryForge {
    private final ExprForge whenExpressionNode;
    private final VariableReadWritePackageForge variableReadWritePackage;
    private boolean isUsingBuiltinProperties;

    /**
     * Ctor.
     *
     * @param whenExpressionNode the expression to evaluate, returning true when to output
     * @param assignments        is the optional then-clause variable assignments, or null or empty if none
     * @param services           services
     * @throws ExprValidationException when validation fails
     */
    public OutputConditionPolledExpressionFactoryForge(ExprNode whenExpressionNode, List<OnTriggerSetAssignment> assignments, StatementCompileTimeServices services)
            throws ExprValidationException {
        this.whenExpressionNode = whenExpressionNode.getForge();

        // determine if using properties
        isUsingBuiltinProperties = false;
        if (containsBuiltinProperties(whenExpressionNode)) {
            isUsingBuiltinProperties = true;
        } else {
            if (assignments != null) {
                for (OnTriggerSetAssignment assignment : assignments) {
                    if (containsBuiltinProperties(assignment.getExpression())) {
                        isUsingBuiltinProperties = true;
                    }
                }
            }
        }

        if (assignments != null) {
            variableReadWritePackage = new VariableReadWritePackageForge(assignments, services);
        } else {
            variableReadWritePackage = null;
        }
    }

    public CodegenExpression make(CodegenMethodScope parent, CodegenClassScope classScope) {
        // initialize+resolve variables
        SAIFFInitializeSymbol symbols = new SAIFFInitializeSymbol();
        CodegenMethod variableInit = classScope.getPackageScope().getInitMethod().makeChildWithScope(VariableReadWritePackage.class, this.getClass(), symbols, classScope).addParam(EPStatementInitServices.class, EPStatementInitServices.REF.getRef());
        variableInit.getBlock().methodReturn(variableReadWritePackage.make(variableInit, symbols, classScope));
        CodegenExpressionField variableRW = classScope.getPackageScope().addFieldUnshared(true, VariableReadWritePackage.class, localMethod(variableInit, EPStatementInitServices.REF));

        CodegenMethod method = parent.makeChild(OutputConditionPolledExpressionFactory.class, this.getClass(), classScope);
        method.getBlock()
                .declareVar(OutputConditionPolledExpressionFactory.class, "factory", newInstance(OutputConditionPolledExpressionFactory.class))
                .exprDotMethod(ref("factory"), "setWhenExpression", ExprNodeUtilityCodegen.codegenEvaluator(whenExpressionNode, method, this.getClass(), classScope))
                .exprDotMethod(ref("factory"), "setVariableReadWritePackage", variableRW)
                .exprDotMethod(ref("factory"), "setUsingBuiltinProperties", constant(isUsingBuiltinProperties))
                .methodReturn(ref("factory"));
        return localMethod(method);
    }

    private boolean containsBuiltinProperties(ExprNode expr) {
        ExprNodeIdentifierVisitor propertyVisitor = new ExprNodeIdentifierVisitor(false);
        expr.accept(propertyVisitor);
        return !propertyVisitor.getExprProperties().isEmpty();
    }
}

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
package com.espertech.esper.common.internal.epl.output.condition;

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.compile.stage1.spec.OnTriggerSetAssignment;
import com.espertech.esper.common.internal.compile.stage3.StatementCompileTimeServices;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;
import com.espertech.esper.common.internal.context.module.EPStatementInitServices;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.epl.expression.core.ExprNodeUtilityCodegen;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;
import com.espertech.esper.common.internal.epl.expression.visitor.ExprNodeIdentifierVisitor;
import com.espertech.esper.common.internal.epl.expression.visitor.ExprNodeVariableVisitor;
import com.espertech.esper.common.internal.epl.variable.compiletime.VariableMetaData;
import com.espertech.esper.common.internal.epl.variable.core.VariableDeployTimeResolver;
import com.espertech.esper.common.internal.epl.variable.core.VariableReadWritePackageForge;
import com.espertech.esper.common.internal.schedule.ScheduleHandleCallbackProvider;

import java.util.List;
import java.util.Map;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

/**
 * Output condition for output rate limiting that handles when-then expressions for controlling output.
 */
public class OutputConditionExpressionForge implements OutputConditionFactoryForge, ScheduleHandleCallbackProvider {
    private final ExprNode whenExpressionNodeEval;
    private final ExprNode andWhenTerminatedExpressionNodeEval;
    private final VariableReadWritePackageForge variableReadWritePackage;
    private final VariableReadWritePackageForge variableReadWritePackageAfterTerminated;
    private final Map<String, VariableMetaData> variableNames;
    protected final boolean isStartConditionOnCreation;
    private final boolean isUsingBuiltinProperties;
    private int scheduleCallbackId = -1;

    public OutputConditionExpressionForge(ExprNode whenExpressionNode, List<OnTriggerSetAssignment> assignments, final ExprNode andWhenTerminatedExpr, List<OnTriggerSetAssignment> afterTerminateAssignments, boolean isStartConditionOnCreation, StatementCompileTimeServices services)
            throws ExprValidationException {
        this.whenExpressionNodeEval = whenExpressionNode;
        this.andWhenTerminatedExpressionNodeEval = andWhenTerminatedExpr;
        this.isStartConditionOnCreation = isStartConditionOnCreation;

        // determine if using variables
        ExprNodeVariableVisitor variableVisitor = new ExprNodeVariableVisitor(services.getVariableCompileTimeResolver());
        whenExpressionNode.accept(variableVisitor);
        variableNames = variableVisitor.getVariableNames();

        // determine if using properties
        boolean containsBuiltinProperties = containsBuiltinProperties(whenExpressionNode);
        if (!containsBuiltinProperties && assignments != null) {
            for (OnTriggerSetAssignment assignment : assignments) {
                if (containsBuiltinProperties(assignment.getExpression())) {
                    containsBuiltinProperties = true;
                }
            }
        }
        if (!containsBuiltinProperties && andWhenTerminatedExpressionNodeEval != null) {
            containsBuiltinProperties = containsBuiltinProperties(andWhenTerminatedExpr);
        }
        if (!containsBuiltinProperties && afterTerminateAssignments != null) {
            for (OnTriggerSetAssignment assignment : afterTerminateAssignments) {
                if (containsBuiltinProperties(assignment.getExpression())) {
                    containsBuiltinProperties = true;
                }
            }
        }
        this.isUsingBuiltinProperties = containsBuiltinProperties;

        if (assignments != null && !assignments.isEmpty()) {
            variableReadWritePackage = new VariableReadWritePackageForge(assignments, services);
        } else {
            variableReadWritePackage = null;
        }

        if (afterTerminateAssignments != null) {
            variableReadWritePackageAfterTerminated = new VariableReadWritePackageForge(afterTerminateAssignments, services);
        } else {
            variableReadWritePackageAfterTerminated = null;
        }
    }

    public CodegenExpression make(CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        if (scheduleCallbackId == -1) {
            throw new IllegalStateException("Schedule callback id not provided");
        }
        CodegenMethod method = parent.makeChild(OutputConditionFactory.class, this.getClass(), classScope);

        method.getBlock()
                .declareVar(OutputConditionExpressionFactory.class, "factory", exprDotMethodChain(symbols.getAddInitSvc(method)).add(EPStatementInitServices.GETRESULTSETPROCESSORHELPERFACTORY).add("makeOutputConditionExpression"))
                .exprDotMethod(ref("factory"), "setWhenExpressionNodeEval", ExprNodeUtilityCodegen.codegenEvaluator(whenExpressionNodeEval.getForge(), method, this.getClass(), classScope))
                .exprDotMethod(ref("factory"), "setAndWhenTerminatedExpressionNodeEval", andWhenTerminatedExpressionNodeEval == null ? constantNull() : ExprNodeUtilityCodegen.codegenEvaluator(andWhenTerminatedExpressionNodeEval.getForge(), method, this.getClass(), classScope))
                .exprDotMethod(ref("factory"), "setUsingBuiltinProperties", constant(isUsingBuiltinProperties))
                .exprDotMethod(ref("factory"), "setVariableReadWritePackage", variableReadWritePackage == null ? constantNull() : variableReadWritePackage.make(method, symbols, classScope))
                .exprDotMethod(ref("factory"), "setVariableReadWritePackageAfterTerminated", variableReadWritePackageAfterTerminated == null ? constantNull() : variableReadWritePackageAfterTerminated.make(method, symbols, classScope))
                .exprDotMethod(ref("factory"), "setVariables", variableNames == null ? constantNull() : VariableDeployTimeResolver.makeResolveVariables(variableNames.values(), symbols.getAddInitSvc(method)))
                .exprDotMethod(ref("factory"), "setScheduleCallbackId", constant(scheduleCallbackId))
                .expression(exprDotMethodChain(symbols.getAddInitSvc(method)).add("addReadyCallback", ref("factory")))
                .methodReturn(ref("factory"));
        return localMethod(method);
    }

    public void collectSchedules(List<ScheduleHandleCallbackProvider> scheduleHandleCallbackProviders) {
        scheduleHandleCallbackProviders.add(this);
    }

    public void setScheduleCallbackId(int id) {
        this.scheduleCallbackId = id;
    }

    private boolean containsBuiltinProperties(ExprNode expr) {
        ExprNodeIdentifierVisitor propertyVisitor = new ExprNodeIdentifierVisitor(false);
        expr.accept(propertyVisitor);
        return !propertyVisitor.getExprProperties().isEmpty();
    }
}

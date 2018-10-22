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
package com.espertech.esper.common.internal.epl.index.advanced.index.service;

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.epl.expression.core.ExprNodeUtilityCodegen;
import com.espertech.esper.common.internal.epl.expression.core.ExprNodeUtilityPrint;
import com.espertech.esper.common.internal.epl.expression.core.ExprNodeUtilityQuery;
import com.espertech.esper.common.internal.epl.lookup.AdvancedIndexDescWExpr;
import com.espertech.esper.common.internal.epl.lookup.EventAdvancedIndexConfigStatementForge;
import com.espertech.esper.common.internal.epl.lookup.EventAdvancedIndexFactoryForge;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;
import static com.espertech.esper.common.internal.epl.expression.core.ExprNodeUtilityQuery.getPropertiesPerExpressionExpectSingle;

public class EventAdvancedIndexProvisionCompileTime {
    private final AdvancedIndexDescWExpr indexDesc;
    private final ExprNode[] parameters;
    private final EventAdvancedIndexFactoryForge factory;
    private final EventAdvancedIndexConfigStatementForge configStatement;

    public EventAdvancedIndexProvisionCompileTime(AdvancedIndexDescWExpr indexDesc, ExprNode[] parameters, EventAdvancedIndexFactoryForge factory, EventAdvancedIndexConfigStatementForge configStatement) {
        this.indexDesc = indexDesc;
        this.parameters = parameters;
        this.factory = factory;
        this.configStatement = configStatement;
    }

    public AdvancedIndexDescWExpr getIndexDesc() {
        return indexDesc;
    }

    public ExprNode[] getParameters() {
        return parameters;
    }

    public EventAdvancedIndexFactoryForge getFactory() {
        return factory;
    }

    public EventAdvancedIndexConfigStatementForge getConfigStatement() {
        return configStatement;
    }

    public CodegenExpression codegenMake(CodegenMethodScope parent, CodegenClassScope classScope) {
        CodegenMethod method = parent.makeChild(EventAdvancedIndexProvisionRuntime.class, EventAdvancedIndexProvisionCompileTime.class, classScope);
        String[] indexExpressions = ExprNodeUtilityPrint.toExpressionStringMinPrecedenceAsArray(indexDesc.getIndexedExpressions());
        String[] indexProperties = getPropertiesPerExpressionExpectSingle(indexDesc.getIndexedExpressions());
        method.getBlock()
                .declareVar(EventAdvancedIndexProvisionRuntime.class, "desc", newInstance(EventAdvancedIndexProvisionRuntime.class))
                .exprDotMethod(ref("desc"), "setIndexExpressionTexts", constant(indexExpressions))
                .exprDotMethod(ref("desc"), "setIndexProperties", constant(indexProperties))
                .exprDotMethod(ref("desc"), "setIndexExpressionsAllProps", constant(ExprNodeUtilityQuery.isExpressionsAllPropsOnly(indexDesc.getIndexedExpressions())))
                .exprDotMethod(ref("desc"), "setFactory", factory.codegenMake(parent, classScope))
                .exprDotMethod(ref("desc"), "setParameterExpressionTexts", constant(ExprNodeUtilityPrint.toExpressionStringsMinPrecedence(parameters)))
                .exprDotMethod(ref("desc"), "setParameterEvaluators", ExprNodeUtilityCodegen.codegenEvaluators(parameters, parent, this.getClass(), classScope))
                .exprDotMethod(ref("desc"), "setConfigStatement", configStatement.codegenMake(parent, classScope))
                .exprDotMethod(ref("desc"), "setIndexTypeName", constant(indexDesc.getIndexTypeName()))
                .methodReturn(ref("desc"));
        return localMethod(method);
    }

    public EventAdvancedIndexProvisionRuntime toRuntime() {
        EventAdvancedIndexProvisionRuntime runtime = new EventAdvancedIndexProvisionRuntime();
        runtime.setIndexExpressionTexts(ExprNodeUtilityPrint.toExpressionStringMinPrecedenceAsArray(indexDesc.getIndexedExpressions()));
        runtime.setIndexProperties(ExprNodeUtilityQuery.getPropertiesPerExpressionExpectSingle(indexDesc.getIndexedExpressions()));
        runtime.setIndexExpressionsOpt(indexDesc.getIndexedExpressions());
        runtime.setIndexExpressionsAllProps(ExprNodeUtilityQuery.isExpressionsAllPropsOnly(indexDesc.getIndexedExpressions()));
        runtime.setFactory(factory.getRuntimeFactory());
        runtime.setParameterExpressionTexts(ExprNodeUtilityPrint.toExpressionStringsMinPrecedence(parameters));
        runtime.setParameterEvaluators(ExprNodeUtilityQuery.getEvaluatorsNoCompile(parameters));
        runtime.setParameterExpressionsOpt(parameters);
        runtime.setConfigStatement(configStatement.toRuntime());
        runtime.setIndexTypeName(indexDesc.getIndexTypeName());
        return runtime;
    }
}

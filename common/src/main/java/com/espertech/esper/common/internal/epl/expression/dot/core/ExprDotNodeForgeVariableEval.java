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
package com.espertech.esper.common.internal.epl.expression.dot.core;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenBlock;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionField;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.variable.compiletime.VariableMetaData;
import com.espertech.esper.common.internal.epl.variable.compiletime.VariableReaderCodegenFieldSharable;
import com.espertech.esper.common.internal.metrics.instrumentation.InstrumentationCode;
import com.espertech.esper.common.internal.rettype.ClassEPType;
import com.espertech.esper.common.internal.rettype.EPTypeCodegenSharable;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class ExprDotNodeForgeVariableEval {
    public static CodegenExpression codegen(ExprDotNodeForgeVariable forge, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope classScope) {

        CodegenExpressionField variableReader = classScope.addOrGetFieldSharable(new VariableReaderCodegenFieldSharable(forge.getVariable()));

        Class variableType;
        VariableMetaData metaData = forge.getVariable();
        if (metaData.getEventType() != null) {
            variableType = EventBean.class;
        } else {
            variableType = metaData.getType();
        }
        CodegenMethod methodNode = codegenMethodScope.makeChild(forge.getEvaluationType(), ExprDotNodeForgeVariableEval.class, classScope);

        CodegenExpression typeInformation = constantNull();
        if (classScope.isInstrumented()) {
            typeInformation = classScope.addOrGetFieldSharable(new EPTypeCodegenSharable(new ClassEPType(variableType), classScope));
        }

        CodegenBlock block = methodNode.getBlock()
                .declareVar(variableType, "result", cast(variableType, exprDotMethod(variableReader, "getValue")))
                .apply(InstrumentationCode.instblock(classScope, "qExprDotChain", typeInformation, ref("result"), constant(forge.getChainForge().length)));
        CodegenExpression chain = ExprDotNodeUtility.evaluateChainCodegen(methodNode, exprSymbol, classScope, ref("result"), variableType, forge.getChainForge(), forge.getResultWrapLambda());
        block.declareVar(forge.getEvaluationType(), "returned", chain)
                .apply(InstrumentationCode.instblock(classScope, "aExprDotChain"))
                .methodReturn(ref("returned"));
        return localMethod(methodNode);
    }
}

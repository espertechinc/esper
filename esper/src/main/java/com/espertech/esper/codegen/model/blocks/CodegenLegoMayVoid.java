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
package com.espertech.esper.codegen.model.blocks;

import com.espertech.esper.codegen.core.CodegenContext;
import com.espertech.esper.codegen.core.CodegenMethodId;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.codegen.model.method.CodegenParamSetExprPremade;
import com.espertech.esper.epl.expression.core.ExprForge;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.constantNull;
import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.localMethodBuild;

public class CodegenLegoMayVoid {
    public static CodegenExpression expressionMayVoid(ExprForge forge, CodegenParamSetExprPremade premade, CodegenContext context) {
        if (forge.getEvaluationType() != void.class) {
            return forge.evaluateCodegen(premade, context);
        }
        CodegenMethodId method = context.addMethod(Object.class, CodegenLegoMayVoid.class).add(premade).begin()
                .expression(forge.evaluateCodegen(premade, context))
                .methodReturn(constantNull());
        return localMethodBuild(method).passAll(premade).call();
    }
}

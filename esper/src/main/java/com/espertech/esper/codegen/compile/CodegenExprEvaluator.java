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
package com.espertech.esper.codegen.compile;

import com.espertech.esper.codegen.core.CodegenClass;
import com.espertech.esper.codegen.core.CodegenContext;
import com.espertech.esper.codegen.core.CodegenMethod;
import com.espertech.esper.codegen.core.CodegenMethodFootprint;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.codegen.model.method.CodegenParamSet;
import com.espertech.esper.codegen.model.method.CodegenParamSetExprPremade;
import com.espertech.esper.epl.core.EngineImportService;
import com.espertech.esper.epl.expression.core.ExprEvaluator;

import java.util.Collections;
import java.util.function.Supplier;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.clazz;
import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.constantNull;

public class CodegenExprEvaluator {

    private final static CodegenMethodFootprint EVALUATE_FP;
    private final static CodegenMethodFootprint GETTYPE_FP;

    static {
        EVALUATE_FP = new CodegenMethodFootprint(Object.class, "evaluate", Collections.<CodegenParamSet>singletonList(CodegenParamSetExprPremade.INSTANCE), null);
        GETTYPE_FP = new CodegenMethodFootprint(Class.class, "getType", Collections.EMPTY_LIST, null);
    }

    public static ExprEvaluator compile(String engineURI, EngineImportService engineImportService, CodegenExpression expression, CodegenContext codegenContext, Class returnType, Supplier<String> debugInformationProvider) throws CodegenCompilerException {
        CodegenMethod evalMethod = new CodegenMethod(EVALUATE_FP, expression);
        CodegenMethod getTypeMethod = new CodegenMethod(GETTYPE_FP, returnType == null ? constantNull() : clazz(returnType));
        CodegenClass clazz = new CodegenClass(ExprEvaluator.class, codegenContext, engineURI, evalMethod, getTypeMethod);
        return CodegenClassGenerator.compile(clazz, engineImportService, ExprEvaluator.class, debugInformationProvider);
    }
}

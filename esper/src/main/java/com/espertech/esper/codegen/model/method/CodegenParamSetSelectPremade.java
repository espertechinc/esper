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
package com.espertech.esper.codegen.model.method;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.codegen.core.CodegenIndent;
import com.espertech.esper.codegen.core.CodegenNamedParam;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.codegen.model.expression.CodegenExpressionRef;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.ref;
import static com.espertech.esper.codegen.model.method.CodegenParamSetExprPremade.EXPREVALCONTEXT_NAME;
import static com.espertech.esper.codegen.model.method.CodegenParamSetExprPremade.ISNEWDATA_NAME;

public class CodegenParamSetSelectPremade extends CodegenParamSet {

    public final static String EPS_NAME = CodegenParamSetExprPremade.EPS_NAME;
    protected final static String ISSYNTHESIZE_NAME = "isSynthesize";

    protected final static CodegenExpressionRef REF_ISSYNTHESIZE = ref(ISSYNTHESIZE_NAME);

    protected final static CodegenNamedParam FP_ISSYNTHESIZE = new CodegenNamedParam(boolean.class, ISSYNTHESIZE_NAME);
    private final static List<CodegenNamedParam> PARAMS = Arrays.asList(CodegenParamSetExprPremade.FP_EPS, CodegenParamSetExprPremade.FP_ISNEWDATA, FP_ISSYNTHESIZE, CodegenParamSetExprPremade.FP_EXPREVALCONTEXT);

    public final static CodegenParamSetSelectPremade INSTANCE = new CodegenParamSetSelectPremade();

    protected CodegenParamSetSelectPremade() {
    }

    public void mergeClasses(Set<Class> classes) {
        classes.add(EventBean.class);
        classes.add(ExprEvaluatorContext.class);
    }

    public void render(StringBuilder builder, Map<Class, String> imports, CodegenIndent codegenIndent, String optionalComment) {
        CodegenNamedParam.render(builder, imports, PARAMS);
    }

    public CodegenExpression passEPS() {
        return CodegenParamSetExprPremade.REF_EPS;
    }

    public CodegenExpression passIsSynthesize() {
        return REF_ISSYNTHESIZE;
    }

    public CodegenExpression passIsNewData() {
        return CodegenParamSetExprPremade.REF_ISNEWDATA_NAME;
    }

    public CodegenExpression passEvalCtx() {
        return CodegenParamSetExprPremade.REF_EXPREVALCONTEXT_NAME;
    }

    public CodegenPassSet getPassAll() {
        return CodegenPassSetExprPremade.INSTANCE;
    }

    public static class CodegenPassSetExprPremade extends CodegenPassSet {
        protected final static CodegenPassSetExprPremade INSTANCE = new CodegenPassSetExprPremade();

        public void render(StringBuilder builder, Map<Class, String> imports) {
            builder.append(EPS_NAME)
                    .append(",")
                    .append(ISNEWDATA_NAME)
                    .append(",")
                    .append(ISSYNTHESIZE_NAME)
                    .append(",")
                    .append(EXPREVALCONTEXT_NAME);
        }

        public void mergeClasses(Set<Class> classes) {
        }
    }
}


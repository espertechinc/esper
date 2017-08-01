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

public class CodegenParamSetExprPremade extends CodegenParamSet {

    public final static String EPS_NAME = "eps";
    protected final static String ISNEWDATA_NAME = "isNewData";
    protected final static String EXPREVALCONTEXT_NAME = "context";

    protected final static CodegenExpressionRef REF_EPS = ref(EPS_NAME);
    protected final static CodegenExpressionRef REF_ISNEWDATA_NAME = ref(ISNEWDATA_NAME);
    protected final static CodegenExpressionRef REF_EXPREVALCONTEXT_NAME = ref(EXPREVALCONTEXT_NAME);

    protected final static CodegenNamedParam FP_EPS = new CodegenNamedParam(EventBean[].class, EPS_NAME);
    protected final static CodegenNamedParam FP_ISNEWDATA = new CodegenNamedParam(boolean.class, ISNEWDATA_NAME);
    protected final static CodegenNamedParam FP_EXPREVALCONTEXT = new CodegenNamedParam(ExprEvaluatorContext.class, EXPREVALCONTEXT_NAME);
    private final static List<CodegenNamedParam> PARAMS = Arrays.asList(FP_EPS, FP_ISNEWDATA, FP_EXPREVALCONTEXT);

    public final static CodegenParamSetExprPremade INSTANCE = new CodegenParamSetExprPremade();

    protected CodegenParamSetExprPremade() {
    }

    public void mergeClasses(Set<Class> classes) {
        classes.add(EventBean.class);
        classes.add(ExprEvaluatorContext.class);
    }

    public void render(StringBuilder builder, Map<Class, String> imports, CodegenIndent codegenIndent, String optionalComment) {
        CodegenNamedParam.render(builder, imports, PARAMS);
    }

    public CodegenExpression passEPS() {
        return REF_EPS;
    }

    public CodegenExpression passIsNewData() {
        return REF_ISNEWDATA_NAME;
    }

    public CodegenExpression passEvalCtx() {
        return REF_EXPREVALCONTEXT_NAME;
    }

    public CodegenNamedParam receiveEPS() {
        return FP_EPS;
    }

    public CodegenNamedParam receiveEvalCtx() {
        return FP_EXPREVALCONTEXT;
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
                    .append(EXPREVALCONTEXT_NAME);
        }

        public void mergeClasses(Set<Class> classes) {
        }
    }
}


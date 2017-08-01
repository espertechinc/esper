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

public class CodegenParamSetIntervalPremade extends CodegenParamSet {

    public final static String LEFTSTART_NAME = "leftStart";
    public final static String LEFTEND_NAME = "leftEnd";
    public final static String RIGHTSTART_NAME = "rightStart";
    public final static String RIGHTEND_NAME = "rightEnd";

    private final static CodegenExpressionRef REF_LEFTSTART = ref(LEFTSTART_NAME);
    private final static CodegenExpressionRef REF_LEFTEND = ref(LEFTEND_NAME);
    private final static CodegenExpressionRef REF_RIGHTSTART = ref(RIGHTSTART_NAME);
    private final static CodegenExpressionRef REF_RIGHTEND = ref(RIGHTEND_NAME);

    private final static CodegenNamedParam FP_LEFTSTART = new CodegenNamedParam(long.class, LEFTSTART_NAME);
    private final static CodegenNamedParam FP_LEFTEND = new CodegenNamedParam(long.class, LEFTEND_NAME);
    private final static CodegenNamedParam FP_RIGHTSTART = new CodegenNamedParam(long.class, RIGHTSTART_NAME);
    private final static CodegenNamedParam FP_RIGHTEND = new CodegenNamedParam(long.class, RIGHTEND_NAME);
    private final static List<CodegenNamedParam> PARAMS = Arrays.asList(FP_LEFTSTART, FP_LEFTEND, FP_RIGHTSTART, FP_RIGHTEND);

    public final static CodegenParamSetIntervalPremade INSTANCE = new CodegenParamSetIntervalPremade();

    protected CodegenParamSetIntervalPremade() {
    }

    public CodegenExpression leftStart() {
        return REF_LEFTSTART;
    }

    public CodegenExpression leftEnd() {
        return REF_LEFTEND;
    }

    public CodegenExpression rightStart() {
        return REF_RIGHTSTART;
    }

    public CodegenExpression rightEnd() {
        return REF_RIGHTEND;
    }

    public void mergeClasses(Set<Class> classes) {
        classes.add(EventBean.class);
        classes.add(ExprEvaluatorContext.class);
    }

    public void render(StringBuilder builder, Map<Class, String> imports, CodegenIndent codegenIndent, String optionalComment) {
        CodegenNamedParam.render(builder, imports, PARAMS);
    }

    public CodegenPassSet getPassAll() {
        return CodegenPassSetIntervalPremade.INSTANCE;
    }

    public static class CodegenPassSetIntervalPremade extends CodegenPassSet {
        protected final static CodegenPassSetIntervalPremade INSTANCE = new CodegenPassSetIntervalPremade();

        public void render(StringBuilder builder, Map<Class, String> imports) {
            builder.append(LEFTSTART_NAME)
                    .append(",")
                    .append(LEFTEND_NAME)
                    .append(",")
                    .append(RIGHTSTART_NAME)
                    .append(",")
                    .append(RIGHTEND_NAME);
        }

        public void mergeClasses(Set<Class> classes) {
        }
    }
}


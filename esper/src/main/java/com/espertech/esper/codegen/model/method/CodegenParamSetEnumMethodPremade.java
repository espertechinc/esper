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

import java.util.*;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.ref;
import static com.espertech.esper.codegen.model.method.CodegenParamSetExprPremade.*;

public class CodegenParamSetEnumMethodPremade extends CodegenParamSet {

    private final static String ENUMCOLL_NAME = "enumcoll";

    private final static CodegenExpressionRef REF_ENUMCOLL = ref(ENUMCOLL_NAME);

    private final static CodegenNamedParam FP_ENUMCOLL = new CodegenNamedParam(Collection.class, ENUMCOLL_NAME);
    private final static List<CodegenNamedParam> PARAMS = Arrays.asList(FP_EPS, FP_ENUMCOLL, FP_ISNEWDATA, FP_EXPREVALCONTEXT);

    public final static CodegenParamSetEnumMethodPremade INSTANCE = new CodegenParamSetEnumMethodPremade();

    protected CodegenParamSetEnumMethodPremade() {
    }

    public CodegenExpression enumcoll() {
        return REF_ENUMCOLL;
    }

    public void mergeClasses(Set<Class> classes) {
        classes.add(EventBean.class);
        classes.add(ExprEvaluatorContext.class);
    }

    public void render(StringBuilder builder, Map<Class, String> imports, CodegenIndent codegenIndent, String optionalComment) {
        CodegenNamedParam.render(builder, imports, PARAMS);
    }

    public CodegenPassSet getPassAll() {
        return CodegenPassSetEnumMethodPremade.INSTANCE;
    }

    public CodegenExpression eps() {
        return CodegenParamSetExprPremade.REF_EPS;
    }

    public static class CodegenPassSetEnumMethodPremade extends CodegenPassSet {
        protected final static CodegenPassSetEnumMethodPremade INSTANCE = new CodegenPassSetEnumMethodPremade();

        public void render(StringBuilder builder, Map<Class, String> imports) {
            builder.append(EPS_NAME)
                    .append(",")
                    .append(ENUMCOLL_NAME)
                    .append(",")
                    .append(ISNEWDATA_NAME)
                    .append(",")
                    .append(EXPREVALCONTEXT_NAME);
        }

        public void mergeClasses(Set<Class> classes) {
        }
    }
}


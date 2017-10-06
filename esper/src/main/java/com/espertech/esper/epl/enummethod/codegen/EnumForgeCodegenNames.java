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
package com.espertech.esper.epl.enummethod.codegen;

import com.espertech.esper.codegen.core.CodegenNamedParam;
import com.espertech.esper.codegen.model.expression.CodegenExpressionRef;
import com.espertech.esper.epl.expression.codegen.ExprForgeCodegenNames;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.ref;
import static com.espertech.esper.epl.expression.codegen.ExprForgeCodegenNames.FP_EXPREVALCONTEXT;
import static com.espertech.esper.epl.expression.codegen.ExprForgeCodegenNames.FP_ISNEWDATA;

public class EnumForgeCodegenNames {

    public final static CodegenExpressionRef REF_ENUMCOLL = ref("enumcoll");
    public final static CodegenExpressionRef REF_EPS = ref(ExprForgeCodegenNames.NAME_EPS);

    private final static CodegenNamedParam FP_ENUMCOLL = new CodegenNamedParam(Collection.class, REF_ENUMCOLL);

    public final static List<CodegenNamedParam> PARAMS = Arrays.asList(ExprForgeCodegenNames.FP_EPS, FP_ENUMCOLL, FP_ISNEWDATA, FP_EXPREVALCONTEXT);
}


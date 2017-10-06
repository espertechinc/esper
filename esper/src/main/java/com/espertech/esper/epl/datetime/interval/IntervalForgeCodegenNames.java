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
package com.espertech.esper.epl.datetime.interval;

import com.espertech.esper.codegen.core.CodegenNamedParam;
import com.espertech.esper.codegen.model.expression.CodegenExpressionRef;

import java.util.Arrays;
import java.util.List;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.ref;

public class IntervalForgeCodegenNames {

    final static CodegenExpressionRef REF_LEFTSTART = ref("leftStart");
    final static CodegenExpressionRef REF_LEFTEND = ref("leftEnd");
    final static CodegenExpressionRef REF_RIGHTSTART = ref("rightStart");
    final static CodegenExpressionRef REF_RIGHTEND = ref("rightEnd");

    private final static CodegenNamedParam FP_LEFTSTART = new CodegenNamedParam(long.class, REF_LEFTSTART);
    private final static CodegenNamedParam FP_LEFTEND = new CodegenNamedParam(long.class, REF_LEFTEND);
    private final static CodegenNamedParam FP_RIGHTSTART = new CodegenNamedParam(long.class, REF_RIGHTSTART);
    private final static CodegenNamedParam FP_RIGHTEND = new CodegenNamedParam(long.class, REF_RIGHTEND);

    final static List<CodegenNamedParam> PARAMS = Arrays.asList(FP_LEFTSTART, FP_LEFTEND, FP_RIGHTSTART, FP_RIGHTEND);
}


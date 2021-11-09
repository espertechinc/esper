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
package com.espertech.esper.common.internal.epl.enummethod.codegen;

import com.espertech.esper.common.client.type.EPTypePremade;
import com.espertech.esper.common.internal.bytecodemodel.core.CodegenNamedParam;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRef;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenNames;

import java.util.Arrays;
import java.util.List;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.ref;
import static com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenNames.FP_EXPREVALCONTEXT;
import static com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenNames.FP_ISNEWDATA;
import static com.espertech.esper.common.internal.epl.util.EPTypeCollectionConst.EPTYPE_COLLECTION_EVENTBEAN;

public class EnumForgeCodegenNames {

    public final static CodegenExpressionRef REF_ENUMCOLL = ref("enumcoll");
    public final static CodegenExpressionRef REF_EPS = ref(ExprForgeCodegenNames.NAME_EPS);

    private final static CodegenNamedParam FP_ENUMCOLLBEAN = new CodegenNamedParam(EPTYPE_COLLECTION_EVENTBEAN, REF_ENUMCOLL);
    private final static CodegenNamedParam FP_ENUMCOLLOBJ = new CodegenNamedParam(EPTypePremade.COLLECTION.getEPType(), REF_ENUMCOLL);

    public final static List<CodegenNamedParam> PARAMSCOLLBEAN = Arrays.asList(ExprForgeCodegenNames.FP_EPS, FP_ENUMCOLLBEAN, FP_ISNEWDATA, FP_EXPREVALCONTEXT);
    public final static List<CodegenNamedParam> PARAMSCOLLOBJ = Arrays.asList(ExprForgeCodegenNames.FP_EPS, FP_ENUMCOLLOBJ, FP_ISNEWDATA, FP_EXPREVALCONTEXT);
}


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
package com.espertech.esper.common.internal.epl.datetime.plugin;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.hook.datetimemethod.DateTimeMethodOpsReformat;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.epl.datetime.eval.DatetimeMethodDesc;
import com.espertech.esper.common.internal.epl.datetime.reformatop.ReformatForge;
import com.espertech.esper.common.internal.epl.datetime.reformatop.ReformatOp;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;
import com.espertech.esper.common.internal.epl.expression.dot.core.ExprDotNodeFilterAnalyzerInput;
import com.espertech.esper.common.internal.epl.join.analyze.FilterExprAnalyzerAffector;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static com.espertech.esper.common.internal.epl.datetime.plugin.DTMPluginUtil.codegenPluginDTM;
import static com.espertech.esper.common.internal.epl.datetime.plugin.DTMPluginUtil.validateDTMStaticMethodAllowNull;

public class DTMPluginReformatForge implements ReformatForge {

    private final DateTimeMethodOpsReformat reformatOp;
    private final List<ExprNode> reformatOpParams;

    public DTMPluginReformatForge(Class inputType, DateTimeMethodOpsReformat reformatOp, List<ExprNode> reformatOpParams) throws ExprValidationException {
        this.reformatOp = reformatOp;
        this.reformatOpParams = reformatOpParams;
        validateDTMStaticMethodAllowNull(inputType, reformatOp.getLongOp(), long.class, reformatOpParams);
        validateDTMStaticMethodAllowNull(inputType, reformatOp.getDateOp(), Date.class, reformatOpParams);
        validateDTMStaticMethodAllowNull(inputType, reformatOp.getCalendarOp(), Calendar.class, reformatOpParams);
        validateDTMStaticMethodAllowNull(inputType, reformatOp.getLdtOp(), LocalDateTime.class, reformatOpParams);
        validateDTMStaticMethodAllowNull(inputType, reformatOp.getZdtOp(), ZonedDateTime.class, reformatOpParams);
        if (reformatOp.getReturnType() == null || reformatOp.getReturnType() == void.class) {
            throw new ExprValidationException("Invalid return type for reformat operation, return type is " + reformatOp.getReturnType());
        }
    }

    public ReformatOp getOp() {
        throw new UnsupportedOperationException("Evaluation not available at compile-time");
    }

    public Class getReturnType() {
        return reformatOp.getReturnType();
    }

    public FilterExprAnalyzerAffector getFilterDesc(EventType[] typesPerStream, DatetimeMethodDesc currentMethod, List<ExprNode> currentParameters, ExprDotNodeFilterAnalyzerInput inputDesc) {
        return null;
    }

    public CodegenExpression codegenLong(CodegenExpression inner, CodegenMethodScope parent, ExprForgeCodegenSymbol symbols, CodegenClassScope classScope) {
        return codegenPluginDTM(reformatOp.getLongOp(), getReturnType(), long.class, inner, reformatOpParams, parent, symbols, classScope);
    }

    public CodegenExpression codegenDate(CodegenExpression inner, CodegenMethodScope parent, ExprForgeCodegenSymbol symbols, CodegenClassScope classScope) {
        return codegenPluginDTM(reformatOp.getDateOp(), getReturnType(), Date.class, inner, reformatOpParams, parent, symbols, classScope);
    }

    public CodegenExpression codegenCal(CodegenExpression inner, CodegenMethodScope parent, ExprForgeCodegenSymbol symbols, CodegenClassScope classScope) {
        return codegenPluginDTM(reformatOp.getCalendarOp(), getReturnType(), Calendar.class, inner, reformatOpParams, parent, symbols, classScope);
    }

    public CodegenExpression codegenLDT(CodegenExpression inner, CodegenMethodScope parent, ExprForgeCodegenSymbol symbols, CodegenClassScope classScope) {
        return codegenPluginDTM(reformatOp.getLdtOp(), getReturnType(), LocalDateTime.class, inner, reformatOpParams, parent, symbols, classScope);
    }

    public CodegenExpression codegenZDT(CodegenExpression inner, CodegenMethodScope parent, ExprForgeCodegenSymbol symbols, CodegenClassScope classScope) {
        return codegenPluginDTM(reformatOp.getZdtOp(), getReturnType(), ZonedDateTime.class, inner, reformatOpParams, parent, symbols, classScope);
    }
}

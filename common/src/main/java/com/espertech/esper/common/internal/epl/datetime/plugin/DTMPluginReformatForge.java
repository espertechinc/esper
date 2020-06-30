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
import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.common.client.type.EPTypePremade;
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
import com.espertech.esper.common.internal.util.JavaClassHelper;

import java.util.List;

import static com.espertech.esper.common.internal.epl.datetime.plugin.DTMPluginUtil.codegenPluginDTM;
import static com.espertech.esper.common.internal.epl.datetime.plugin.DTMPluginUtil.validateDTMStaticMethodAllowNull;

public class DTMPluginReformatForge implements ReformatForge {

    private final DateTimeMethodOpsReformat reformatOp;
    private final List<ExprNode> reformatOpParams;

    public DTMPluginReformatForge(EPTypeClass inputType, DateTimeMethodOpsReformat reformatOp, List<ExprNode> reformatOpParams) throws ExprValidationException {
        this.reformatOp = reformatOp;
        this.reformatOpParams = reformatOpParams;
        validateDTMStaticMethodAllowNull(inputType, reformatOp.getLongOp(), EPTypePremade.LONGPRIMITIVE.getEPType(), reformatOpParams);
        validateDTMStaticMethodAllowNull(inputType, reformatOp.getDateOp(), EPTypePremade.DATE.getEPType(), reformatOpParams);
        validateDTMStaticMethodAllowNull(inputType, reformatOp.getCalendarOp(), EPTypePremade.CALENDAR.getEPType(), reformatOpParams);
        validateDTMStaticMethodAllowNull(inputType, reformatOp.getLdtOp(), EPTypePremade.LOCALDATETIME.getEPType(), reformatOpParams);
        validateDTMStaticMethodAllowNull(inputType, reformatOp.getZdtOp(), EPTypePremade.ZONEDDATETIME.getEPType(), reformatOpParams);
        if (reformatOp.getReturnType() == null || JavaClassHelper.isTypeVoid(reformatOp.getReturnType())) {
            throw new ExprValidationException("Invalid return type for reformat operation, return type is " + reformatOp.getReturnType());
        }
    }

    public ReformatOp getOp() {
        throw new UnsupportedOperationException("Evaluation not available at compile-time");
    }

    public EPTypeClass getReturnType() {
        return reformatOp.getReturnType();
    }

    public FilterExprAnalyzerAffector getFilterDesc(EventType[] typesPerStream, DatetimeMethodDesc currentMethod, List<ExprNode> currentParameters, ExprDotNodeFilterAnalyzerInput inputDesc) {
        return null;
    }

    public CodegenExpression codegenLong(CodegenExpression inner, CodegenMethodScope parent, ExprForgeCodegenSymbol symbols, CodegenClassScope classScope) {
        return codegenPluginDTM(reformatOp.getLongOp(), getReturnType(), EPTypePremade.LONGPRIMITIVE.getEPType(), inner, reformatOpParams, parent, symbols, classScope);
    }

    public CodegenExpression codegenDate(CodegenExpression inner, CodegenMethodScope parent, ExprForgeCodegenSymbol symbols, CodegenClassScope classScope) {
        return codegenPluginDTM(reformatOp.getDateOp(), getReturnType(), EPTypePremade.DATE.getEPType(), inner, reformatOpParams, parent, symbols, classScope);
    }

    public CodegenExpression codegenCal(CodegenExpression inner, CodegenMethodScope parent, ExprForgeCodegenSymbol symbols, CodegenClassScope classScope) {
        return codegenPluginDTM(reformatOp.getCalendarOp(), getReturnType(), EPTypePremade.CALENDAR.getEPType(), inner, reformatOpParams, parent, symbols, classScope);
    }

    public CodegenExpression codegenLDT(CodegenExpression inner, CodegenMethodScope parent, ExprForgeCodegenSymbol symbols, CodegenClassScope classScope) {
        return codegenPluginDTM(reformatOp.getLdtOp(), getReturnType(), EPTypePremade.LOCALDATETIME.getEPType(), inner, reformatOpParams, parent, symbols, classScope);
    }

    public CodegenExpression codegenZDT(CodegenExpression inner, CodegenMethodScope parent, ExprForgeCodegenSymbol symbols, CodegenClassScope classScope) {
        return codegenPluginDTM(reformatOp.getZdtOp(), getReturnType(), EPTypePremade.ZONEDDATETIME.getEPType(), inner, reformatOpParams, parent, symbols, classScope);
    }
}

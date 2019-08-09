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

import com.espertech.esper.common.client.hook.datetimemethod.DateTimeMethodOpsModify;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.epl.datetime.calop.CalendarForge;
import com.espertech.esper.common.internal.epl.datetime.calop.CalendarOp;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.List;

import static com.espertech.esper.common.internal.epl.datetime.plugin.DTMPluginUtil.codegenPluginDTM;
import static com.espertech.esper.common.internal.epl.datetime.plugin.DTMPluginUtil.validateDTMStaticMethodAllowNull;

public class DTMPluginValueChangeForge implements CalendarForge {

    private final DateTimeMethodOpsModify transformOp;
    private final List<ExprNode> transformOpParams;

    public DTMPluginValueChangeForge(Class inputType, DateTimeMethodOpsModify transformOp, List<ExprNode> transformOpParams) throws ExprValidationException {
        this.transformOp = transformOp;
        this.transformOpParams = transformOpParams;
        validateDTMStaticMethodAllowNull(inputType, transformOp.getCalendarOp(), Calendar.class, transformOpParams);
        validateDTMStaticMethodAllowNull(inputType, transformOp.getLdtOp(), LocalDateTime.class, transformOpParams);
        validateDTMStaticMethodAllowNull(inputType, transformOp.getZdtOp(), ZonedDateTime.class, transformOpParams);
    }

    public CalendarOp getEvalOp() {
        throw new UnsupportedOperationException("Evaluation not available at compile-time");
    }

    public CodegenExpression codegenCalendar(CodegenExpression cal, CodegenMethodScope parent, ExprForgeCodegenSymbol symbols, CodegenClassScope classScope) {
        return codegenPluginDTM(transformOp.getCalendarOp(), void.class, Calendar.class, cal, transformOpParams, parent, symbols, classScope);
    }

    public CodegenExpression codegenLDT(CodegenExpression ldt, CodegenMethodScope parent, ExprForgeCodegenSymbol symbols, CodegenClassScope classScope) {
        return codegenPluginDTM(transformOp.getLdtOp(), LocalDateTime.class, LocalDateTime.class, ldt, transformOpParams, parent, symbols, classScope);
    }

    public CodegenExpression codegenZDT(CodegenExpression zdt, CodegenMethodScope parent, ExprForgeCodegenSymbol symbols, CodegenClassScope classScope) {
        return codegenPluginDTM(transformOp.getZdtOp(), ZonedDateTime.class, ZonedDateTime.class, zdt, transformOpParams, parent, symbols, classScope);
    }
}

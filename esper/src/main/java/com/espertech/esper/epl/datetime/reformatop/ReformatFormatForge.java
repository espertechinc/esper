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
package com.espertech.esper.epl.datetime.reformatop;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventType;
import com.espertech.esper.codegen.base.CodegenBlock;
import com.espertech.esper.codegen.base.CodegenClassScope;
import com.espertech.esper.codegen.base.CodegenMember;
import com.espertech.esper.codegen.base.CodegenMethodScope;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.epl.datetime.eval.DatetimeMethodEnum;
import com.espertech.esper.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.epl.expression.dot.ExprDotNodeFilterAnalyzerInput;
import com.espertech.esper.epl.expression.time.TimeAbacus;
import com.espertech.esper.epl.join.plan.FilterExprAnalyzerAffector;

import java.text.DateFormat;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;

public class ReformatFormatForge implements ReformatForge, ReformatOp {

    private final DateFormat dateFormat;
    private final DateTimeFormatter dateTimeFormatter;
    private final TimeAbacus timeAbacus;

    public ReformatFormatForge(Object formatter, TimeAbacus timeAbacus) {
        if (formatter instanceof DateFormat) {
            dateFormat = (DateFormat) formatter;
            dateTimeFormatter = null;
        } else {
            dateFormat = null;
            dateTimeFormatter = (DateTimeFormatter) formatter;
        }
        this.timeAbacus = timeAbacus;
    }

    public ReformatOp getOp() {
        return this;
    }

    public synchronized Object evaluate(Long ts, EventBean[] eventsPerStream, boolean newData, ExprEvaluatorContext exprEvaluatorContext) {
        if (timeAbacus.getOneSecond() == 1000L) {
            return dateFormat.format(ts);
        }
        return dateFormat.format(timeAbacus.toDate(ts));
    }

    public CodegenExpression codegenLong(CodegenExpression inner, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        CodegenMember df = codegenClassScope.makeAddMember(DateFormat.class, dateFormat);
        CodegenBlock blockMethod = codegenMethodScope.makeChild(String.class, ReformatFormatForge.class, codegenClassScope).addParam(long.class, "ts").getBlock();
        CodegenBlock syncBlock = blockMethod.synchronizedOn(member(df.getMemberId()));
        if (timeAbacus.getOneSecond() == 1000L) {
            syncBlock.blockReturn(exprDotMethod(member(df.getMemberId()), "format", ref("ts")));
        } else {
            syncBlock.blockReturn(exprDotMethod(member(df.getMemberId()), "format", timeAbacus.toDateCodegen(ref("ts"))));
        }
        return localMethodBuild(blockMethod.methodEnd()).pass(inner).call();
    }

    public synchronized Object evaluate(Date d, EventBean[] eventsPerStream, boolean newData, ExprEvaluatorContext exprEvaluatorContext) {
        return dateFormat.format(d);
    }

    public CodegenExpression codegenDate(CodegenExpression inner, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        CodegenMember df = codegenClassScope.makeAddMember(DateFormat.class, dateFormat);
        CodegenBlock blockMethod = codegenMethodScope.makeChild(String.class, ReformatFormatForge.class, codegenClassScope).addParam(Date.class, "d").getBlock()
                .synchronizedOn(member(df.getMemberId()))
                .blockReturn(exprDotMethod(member(df.getMemberId()), "format", ref("d")));
        return localMethodBuild(blockMethod.methodEnd()).pass(inner).call();
    }

    public synchronized Object evaluate(Calendar cal, EventBean[] eventsPerStream, boolean newData, ExprEvaluatorContext exprEvaluatorContext) {
        return dateFormat.format(cal.getTime());
    }

    public CodegenExpression codegenCal(CodegenExpression inner, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        CodegenMember df = codegenClassScope.makeAddMember(DateFormat.class, dateFormat);
        CodegenBlock blockMethod = codegenMethodScope.makeChild(String.class, ReformatFormatForge.class, codegenClassScope).addParam(Calendar.class, "cal").getBlock()
                .synchronizedOn(member(df.getMemberId()))
                .blockReturn(exprDotMethod(member(df.getMemberId()), "format", exprDotMethod(ref("cal"), "getTime")));
        return localMethodBuild(blockMethod.methodEnd()).pass(inner).call();
    }

    public Object evaluate(LocalDateTime ldt, EventBean[] eventsPerStream, boolean newData, ExprEvaluatorContext exprEvaluatorContext) {
        return ldt.format(dateTimeFormatter);
    }

    public CodegenExpression codegenLDT(CodegenExpression inner, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        CodegenMember df = codegenClassScope.makeAddMember(DateTimeFormatter.class, dateTimeFormatter);
        return exprDotMethod(inner, "format", member(df.getMemberId()));
    }

    public Object evaluate(ZonedDateTime zdt, EventBean[] eventsPerStream, boolean newData, ExprEvaluatorContext exprEvaluatorContext) {
        return zdt.format(dateTimeFormatter);
    }

    public CodegenExpression codegenZDT(CodegenExpression inner, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        CodegenMember df = codegenClassScope.makeAddMember(DateTimeFormatter.class, dateTimeFormatter);
        return exprDotMethod(inner, "format", member(df.getMemberId()));
    }

    public Class getReturnType() {
        return String.class;
    }

    public FilterExprAnalyzerAffector getFilterDesc(EventType[] typesPerStream, DatetimeMethodEnum currentMethod, List<ExprNode> currentParameters, ExprDotNodeFilterAnalyzerInput inputDesc) {
        return null;
    }
}

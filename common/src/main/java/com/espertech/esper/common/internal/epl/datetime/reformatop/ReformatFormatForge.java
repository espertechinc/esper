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
package com.espertech.esper.common.internal.epl.datetime.reformatop;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.common.client.type.EPTypePremade;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenBlock;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionField;
import com.espertech.esper.common.internal.epl.datetime.eval.DatetimeMethodDesc;
import com.espertech.esper.common.internal.epl.expression.codegen.CodegenLegoMethodExpression;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.common.internal.epl.expression.core.ExprForge;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.epl.expression.dot.core.ExprDotNodeFilterAnalyzerInput;
import com.espertech.esper.common.internal.epl.expression.time.abacus.TimeAbacus;
import com.espertech.esper.common.internal.epl.join.analyze.FilterExprAnalyzerAffector;

import java.text.DateFormat;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class ReformatFormatForge implements ReformatForge, ReformatOp {

    private final ReformatFormatForgeDesc formatterType;
    private final ExprForge formatter;
    private final TimeAbacus timeAbacus;

    public ReformatFormatForge(ReformatFormatForgeDesc formatterType, ExprForge formatter, TimeAbacus timeAbacus) {
        this.formatterType = formatterType;
        this.formatter = formatter;
        this.timeAbacus = timeAbacus;
    }

    public ReformatOp getOp() {
        return this;
    }

    public synchronized Object evaluate(Long ts, EventBean[] eventsPerStream, boolean newData, ExprEvaluatorContext exprEvaluatorContext) {
        if (timeAbacus.getOneSecond() == 1000L) {
            return getDateFormatFormatter().format(ts);
        }
        return getDateFormatFormatter().format(timeAbacus.toDate(ts));
    }

    public CodegenExpression codegenLong(CodegenExpression inner, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope classScope) {
        CodegenExpressionField formatField = codegenFormatFieldInit(classScope);
        CodegenBlock blockMethod = codegenMethodScope.makeChild(EPTypePremade.STRING.getEPType(), ReformatFormatForge.class, classScope).addParam(EPTypePremade.LONGPRIMITIVE.getEPType(), "ts").getBlock();
        CodegenBlock syncBlock = blockMethod.synchronizedOn(formatField);
        if (timeAbacus.getOneSecond() == 1000L) {
            syncBlock.blockReturn(exprDotMethod(formatField, "format", ref("ts")));
        } else {
            syncBlock.blockReturn(exprDotMethod(formatField, "format", timeAbacus.toDateCodegen(ref("ts"))));
        }
        return localMethodBuild(blockMethod.methodEnd()).pass(inner).call();
    }

    public synchronized Object evaluate(Date d, EventBean[] eventsPerStream, boolean newData, ExprEvaluatorContext exprEvaluatorContext) {
        return getDateFormatFormatter().format(d);
    }

    public CodegenExpression codegenDate(CodegenExpression inner, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        CodegenExpressionField formatField = codegenFormatFieldInit(codegenClassScope);
        CodegenBlock blockMethod = codegenMethodScope.makeChild(EPTypePremade.STRING.getEPType(), ReformatFormatForge.class, codegenClassScope).addParam(EPTypePremade.DATE.getEPType(), "d").getBlock()
            .synchronizedOn(formatField)
            .blockReturn(exprDotMethod(formatField, "format", ref("d")));
        return localMethodBuild(blockMethod.methodEnd()).pass(inner).call();
    }

    public synchronized Object evaluate(Calendar cal, EventBean[] eventsPerStream, boolean newData, ExprEvaluatorContext exprEvaluatorContext) {
        return getDateFormatFormatter().format(cal.getTime());
    }

    public CodegenExpression codegenCal(CodegenExpression inner, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        CodegenExpressionField formatField = codegenFormatFieldInit(codegenClassScope);
        CodegenBlock blockMethod = codegenMethodScope.makeChild(EPTypePremade.STRING.getEPType(), ReformatFormatForge.class, codegenClassScope).addParam(EPTypePremade.CALENDAR.getEPType(), "cal").getBlock()
            .synchronizedOn(formatField)
            .blockReturn(exprDotMethod(formatField, "format", exprDotMethod(ref("cal"), "getTime")));
        return localMethodBuild(blockMethod.methodEnd()).pass(inner).call();
    }

    public Object evaluate(LocalDateTime ldt, EventBean[] eventsPerStream, boolean newData, ExprEvaluatorContext exprEvaluatorContext) {
        return ldt.format(getDateFormatterJava8());
    }

    public CodegenExpression codegenLDT(CodegenExpression inner, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        CodegenExpressionField formatField = codegenFormatFieldInit(codegenClassScope);
        return exprDotMethod(inner, "format", formatField);
    }

    public Object evaluate(ZonedDateTime zdt, EventBean[] eventsPerStream, boolean newData, ExprEvaluatorContext exprEvaluatorContext) {
        return zdt.format(getDateFormatterJava8());
    }

    public CodegenExpression codegenZDT(CodegenExpression inner, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        CodegenExpressionField formatField = codegenFormatFieldInit(codegenClassScope);
        return exprDotMethod(inner, "format", formatField);
    }

    public EPTypeClass getReturnType() {
        return EPTypePremade.STRING.getEPType();
    }

    public FilterExprAnalyzerAffector getFilterDesc(EventType[] typesPerStream, DatetimeMethodDesc currentMethod, List<ExprNode> currentParameters, ExprDotNodeFilterAnalyzerInput inputDesc) {
        return null;
    }

    private CodegenExpressionField codegenFormatFieldInit(CodegenClassScope classScope) {
        CodegenMethod formatEvalCall = CodegenLegoMethodExpression.codegenExpression(formatter, classScope.getPackageScope().getInitMethod(), classScope);
        CodegenExpression formatEval = localMethod(formatEvalCall, constantNull(), constantTrue(), constantNull());
        CodegenExpression init;
        if (formatterType.getFormatterType() != String.class) {
            init = formatEval;
        } else {
            CodegenMethod parse = classScope.getPackageScope().getInitMethod().makeChild(formatterType.isJava8() ? EPTypePremade.DATETIMEFORMATTER.getEPType() : EPTypePremade.DATEFORMAT.getEPType(), this.getClass(), classScope);
            if (formatterType.isJava8()) {
                parse.getBlock().methodReturn(staticMethod(DateTimeFormatter.class, "ofPattern", formatEval));
            } else {
                parse.getBlock().methodReturn(newInstance(EPTypePremade.SIMPLEDATEFORMAT.getEPType(), formatEval));
            }
            init = localMethod(parse);
        }
        return classScope.addFieldUnshared(true, formatterType.isJava8() ? EPTypePremade.DATETIMEFORMATTER.getEPType() : EPTypePremade.DATEFORMAT.getEPType(), init);
    }

    private DateFormat getDateFormatFormatter() {
        return (DateFormat) formatter.getExprEvaluator().evaluate(null, true, null);
    }

    private DateTimeFormatter getDateFormatterJava8() {
        return (DateTimeFormatter) formatter.getExprEvaluator().evaluate(null, true, null);
    }
}

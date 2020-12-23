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
package com.espertech.esper.common.internal.epl.pattern.guard;

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.compile.stage3.StatementCompileTimeServices;
import com.espertech.esper.common.internal.compile.util.CallbackAttribution;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;
import com.espertech.esper.common.internal.context.module.EPStatementInitServices;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.epl.expression.core.ExprNodeUtilityCodegen;
import com.espertech.esper.common.internal.epl.pattern.core.MatchedEventConvertorForge;
import com.espertech.esper.common.internal.schedule.ScheduleHandleTracked;

import java.util.List;
import java.util.function.Function;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;
import static com.espertech.esper.common.internal.util.JavaClassHelper.isTypeBoolean;

public class ExpressionGuardForge implements GuardForge {

    private ExprNode expression;
    private MatchedEventConvertorForge convertor;

    public void setGuardParameters(List<ExprNode> parameters, MatchedEventConvertorForge convertor, StatementCompileTimeServices services) throws GuardParameterException {
        String errorMessage = "Expression pattern guard requires a single expression as a parameter returning a true or false (boolean) value";
        if (parameters.size() != 1) {
            throw new GuardParameterException(errorMessage);
        }
        expression = parameters.get(0);

        if (!isTypeBoolean(parameters.get(0).getForge().getEvaluationType())) {
            throw new GuardParameterException(errorMessage);
        }

        this.convertor = convertor;
    }

    public void collectSchedule(short factoryNodeId, Function<Short, CallbackAttribution> callbackAttribution, List<ScheduleHandleTracked> schedules) {
        // nothing to collect
    }

    public CodegenExpression makeCodegen(CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        CodegenMethod method = parent.makeChild(ExpressionGuardFactory.EPTYPE, this.getClass(), classScope);
        method.getBlock()
                .declareVar(ExpressionGuardFactory.EPTYPE, "factory", exprDotMethodChain(symbols.getAddInitSvc(method)).add(EPStatementInitServices.GETPATTERNFACTORYSERVICE).add("guardWhile"))
                .exprDotMethod(ref("factory"), "setConvertor", convertor.makeAnonymous(method, classScope))
                .exprDotMethod(ref("factory"), "setExpression", ExprNodeUtilityCodegen.codegenEvaluator(expression.getForge(), method, this.getClass(), classScope))
                .methodReturn(ref("factory"));
        return localMethod(method);
    }
}
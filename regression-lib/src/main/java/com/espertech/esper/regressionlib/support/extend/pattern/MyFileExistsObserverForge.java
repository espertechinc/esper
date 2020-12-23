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
package com.espertech.esper.regressionlib.support.extend.pattern;

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.compile.util.CallbackAttribution;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeBuilder;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationContext;
import com.espertech.esper.common.internal.epl.pattern.core.MatchedEventConvertorForge;
import com.espertech.esper.common.internal.epl.pattern.observer.ObserverForge;
import com.espertech.esper.common.internal.epl.pattern.observer.ObserverParameterException;
import com.espertech.esper.common.internal.schedule.ScheduleHandleTracked;

import java.util.List;
import java.util.function.Function;

import static com.espertech.esper.common.internal.util.JavaClassHelper.isTypeString;

public class MyFileExistsObserverForge implements ObserverForge {
    protected ExprNode filenameExpression;
    protected MatchedEventConvertorForge convertor;

    public void setObserverParameters(List<ExprNode> observerParameters, MatchedEventConvertorForge convertor, ExprValidationContext validationContext) throws ObserverParameterException {
        String message = "File exists observer takes a single string filename parameter";
        if (observerParameters.size() != 1) {
            throw new ObserverParameterException(message);
        }
        if (!isTypeString(observerParameters.get(0).getForge().getEvaluationType())) {
            throw new ObserverParameterException(message);
        }

        this.filenameExpression = observerParameters.get(0);
        this.convertor = convertor;
    }

    public CodegenExpression makeCodegen(CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        SAIFFInitializeBuilder builder = new SAIFFInitializeBuilder(MyFileExistsObserverFactory.EPTYPE, this.getClass(), "observerFactory", parent, symbols, classScope);
        return builder.exprnode("filenameExpression", filenameExpression)
            .expression("convertor", convertor.makeAnonymous(builder.getMethod(), classScope))
            .build();
    }

    public void collectSchedule(short factoryNodeId, Function<Short, CallbackAttribution> scheduleAttribution, List<ScheduleHandleTracked> schedules) {
    }
}

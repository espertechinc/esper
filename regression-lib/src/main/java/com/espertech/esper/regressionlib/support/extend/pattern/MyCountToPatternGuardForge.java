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
import com.espertech.esper.common.internal.compile.stage3.StatementCompileTimeServices;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeBuilder;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.epl.pattern.core.MatchedEventConvertorForge;
import com.espertech.esper.common.internal.epl.pattern.guard.GuardForge;
import com.espertech.esper.common.internal.epl.pattern.guard.GuardParameterException;
import com.espertech.esper.common.internal.schedule.ScheduleHandleCallbackProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class MyCountToPatternGuardForge implements GuardForge {
    private static final Logger log = LoggerFactory.getLogger(MyCountToPatternGuardForge.class);

    private ExprNode numCountToExpr;
    private MatchedEventConvertorForge convertor;

    public void setGuardParameters(List<ExprNode> guardParameters, MatchedEventConvertorForge convertor, StatementCompileTimeServices services) throws GuardParameterException {
        String message = "Count-to guard takes a single integer-value expression as parameter";
        if (guardParameters.size() != 1) {
            throw new GuardParameterException(message);
        }

        Class paramType = guardParameters.get(0).getForge().getEvaluationType();
        if (paramType != Integer.class && paramType != int.class) {
            throw new GuardParameterException(message);
        }

        this.numCountToExpr = guardParameters.get(0);
        this.convertor = convertor;
    }

    public void collectSchedule(List<ScheduleHandleCallbackProvider> schedules) {
    }

    public CodegenExpression makeCodegen(CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        SAIFFInitializeBuilder builder = new SAIFFInitializeBuilder(MyCountToPatternGuardFactory.class, this.getClass(), "guardFactory", parent, symbols, classScope);
        return builder.exprnode("numCountToExpr", numCountToExpr)
            .expression("convertor", convertor.makeAnonymous(builder.getMethod(), classScope))
            .build();
    }
}

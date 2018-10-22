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
package com.espertech.esper.common.internal.epl.output.polled;

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.compile.stage2.StatementRawInfo;
import com.espertech.esper.common.internal.compile.stage3.StatementCompileTimeServices;
import com.espertech.esper.common.internal.epl.expression.core.*;
import com.espertech.esper.common.internal.epl.streamtype.StreamTypeServiceImpl;

import java.util.List;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.newInstance;

/**
 * Output condition handling crontab-at schedule output.
 */
public final class OutputConditionPolledCrontabFactoryForge implements OutputConditionPolledFactoryForge {
    private final ExprNode[] expressions;

    public OutputConditionPolledCrontabFactoryForge(List<ExprNode> list, StatementRawInfo statementRawInfo, StatementCompileTimeServices services)
            throws ExprValidationException {

        ExprValidationContext validationContext = new ExprValidationContextBuilder(new StreamTypeServiceImpl(false), statementRawInfo, services).build();
        expressions = new ExprNode[list.size()];
        int count = 0;
        for (ExprNode parameters : list) {
            ExprNode node = ExprNodeUtilityValidate.getValidatedSubtree(ExprNodeOrigin.OUTPUTLIMIT, parameters, validationContext);
            expressions[count++] = node;
        }
    }

    public CodegenExpression make(CodegenMethodScope parent, CodegenClassScope classScope) {
        return newInstance(OutputConditionPolledCrontabFactory.class, ExprNodeUtilityCodegen.codegenEvaluators(expressions, parent, this.getClass(), classScope));
    }
}

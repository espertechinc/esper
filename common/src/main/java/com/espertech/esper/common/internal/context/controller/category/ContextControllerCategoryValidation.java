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
package com.espertech.esper.common.internal.context.controller.category;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRef;
import com.espertech.esper.common.internal.compile.stage2.FilterSpecCompiled;
import com.espertech.esper.common.internal.compile.stage2.StatementSpecCompiled;
import com.espertech.esper.common.internal.compile.stage3.StatementCompileTimeServices;
import com.espertech.esper.common.internal.context.controller.core.ContextControllerPortableInfo;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;
import com.espertech.esper.common.internal.epl.util.StatementSpecCompiledAnalyzer;
import com.espertech.esper.common.internal.epl.util.StatementSpecCompiledAnalyzerResult;
import com.espertech.esper.common.internal.event.core.EventTypeUtility;

import java.util.List;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.newInstance;

public class ContextControllerCategoryValidation implements ContextControllerPortableInfo {
    private final EventType categoryEventType;

    public ContextControllerCategoryValidation(EventType categoryEventType) {
        this.categoryEventType = categoryEventType;
    }

    public EventType getCategoryEventType() {
        return categoryEventType;
    }

    public CodegenExpression make(CodegenExpressionRef addInitSvc) {
        return newInstance(ContextControllerCategoryValidation.class, EventTypeUtility.resolveTypeCodegen(categoryEventType, addInitSvc));
    }

    public void validateStatement(String contextName, StatementSpecCompiled spec, StatementCompileTimeServices compileTimeServices) throws ExprValidationException {
        StatementSpecCompiledAnalyzerResult streamAnalysis = StatementSpecCompiledAnalyzer.analyzeFilters(spec);
        List<FilterSpecCompiled> filters = streamAnalysis.getFilters();

        // Category validation
        boolean isCreateWindow = spec.getRaw().getCreateWindowDesc() != null;
        String message = "Category context '" + contextName + "' requires that any of the events types that are listed in the category context also appear in any of the filter expressions of the statement";

        // if no create-window: at least one of the filters must match one of the filters specified by the context
        if (!isCreateWindow) {
            for (FilterSpecCompiled filter : filters) {
                EventType stmtFilterType = filter.getFilterForEventType();
                if (stmtFilterType == categoryEventType) {
                    return;
                }
                if (EventTypeUtility.isTypeOrSubTypeOf(stmtFilterType, categoryEventType)) {
                    return;
                }
            }

            if (!filters.isEmpty()) {
                throw new ExprValidationException(message);
            }
            return;
        }

        // validate create-window
        String declaredAsName = spec.getRaw().getCreateWindowDesc().getAsEventTypeName();
        if (declaredAsName != null) {
            if (categoryEventType.getName().equals(declaredAsName)) {
                return;
            }
            throw new ExprValidationException(message);
        }
    }
}

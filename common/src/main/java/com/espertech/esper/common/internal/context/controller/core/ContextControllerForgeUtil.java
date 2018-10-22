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
package com.espertech.esper.common.internal.context.controller.core;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.compile.stage2.FilterSpecCompiled;
import com.espertech.esper.common.internal.compile.stage2.StatementSpecCompiled;
import com.espertech.esper.common.internal.compile.stage3.StatementCompileTimeServices;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;
import com.espertech.esper.common.internal.epl.namedwindow.path.NamedWindowMetaData;
import com.espertech.esper.common.internal.epl.util.StatementSpecCompiledAnalyzer;
import com.espertech.esper.common.internal.epl.util.StatementSpecCompiledAnalyzerResult;
import com.espertech.esper.common.internal.event.core.EventTypeUtility;

import java.util.List;
import java.util.function.Supplier;

public class ContextControllerForgeUtil {
    public static void validateStatementKeyAndHash(Supplier<EventType>[] typeProvider, String contextName, StatementSpecCompiled spec, StatementCompileTimeServices compileTimeServices) throws ExprValidationException {
        StatementSpecCompiledAnalyzerResult streamAnalysis = StatementSpecCompiledAnalyzer.analyzeFilters(spec);
        List<FilterSpecCompiled> filters = streamAnalysis.getFilters();

        boolean isCreateWindow = spec.getRaw().getCreateWindowDesc() != null;

        // if no create-window: at least one of the filters must match one of the filters specified by the context
        if (!isCreateWindow) {
            for (FilterSpecCompiled filter : filters) {
                for (Supplier<EventType> item : typeProvider) {
                    EventType itemEventType = item.get();
                    EventType stmtFilterType = filter.getFilterForEventType();
                    if (stmtFilterType == itemEventType) {
                        return;
                    }
                    if (EventTypeUtility.isTypeOrSubTypeOf(stmtFilterType, itemEventType)) {
                        return;
                    }

                    NamedWindowMetaData namedWindow = compileTimeServices.getNamedWindowCompileTimeResolver().resolve(stmtFilterType.getName());
                    if (namedWindow != null) {
                        String namedWindowContextName = namedWindow.getContextName();
                        if (namedWindowContextName != null && namedWindowContextName.equals(contextName)) {
                            return;
                        }
                    }
                }
            }

            if (!filters.isEmpty()) {
                throw new ExprValidationException(getTypeValidationMessage(contextName, filters.get(0).getFilterForEventType().getName()));
            }
            return;
        }

        // validate create-window with column definition: not allowed, requires typed
        if (spec.getRaw().getCreateWindowDesc().getColumns() != null &&
                spec.getRaw().getCreateWindowDesc().getColumns().size() > 0) {
            throw new ExprValidationException("Segmented context '" + contextName +
                    "' requires that named windows are associated to an existing event type and that the event type is listed among the partitions defined by the create-context statement");
        }

        // validate create-window declared type
        String declaredAsName = spec.getRaw().getCreateWindowDesc().getAsEventTypeName();
        if (declaredAsName != null) {
            for (Supplier<EventType> item : typeProvider) {
                EventType itemEventType = item.get();
                if (itemEventType.getName().equals(declaredAsName)) {
                    return;
                }
            }

            throw new ExprValidationException(getTypeValidationMessage(contextName, declaredAsName));
        }
    }

    private static String getTypeValidationMessage(String contextName, String typeNameEx) {
        return "Segmented context '" + contextName + "' requires that any of the event types that are listed in the segmented context also appear in any of the filter expressions of the statement, type '" + typeNameEx + "' is not one of the types listed";
    }
}

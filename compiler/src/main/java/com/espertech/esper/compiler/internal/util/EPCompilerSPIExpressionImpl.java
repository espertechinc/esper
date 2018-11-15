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
package com.espertech.esper.compiler.internal.util;

import com.espertech.esper.common.client.EPException;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.util.StatementType;
import com.espertech.esper.common.internal.compile.stage2.StatementRawInfo;
import com.espertech.esper.common.internal.compile.stage3.ModuleCompileTimeServices;
import com.espertech.esper.common.internal.compile.stage3.StatementCompileTimeServices;
import com.espertech.esper.common.internal.epl.expression.core.*;
import com.espertech.esper.common.internal.epl.streamtype.StreamTypeService;
import com.espertech.esper.common.internal.epl.streamtype.StreamTypeServiceImpl;
import com.espertech.esper.compiler.client.EPCompileException;

import java.util.Arrays;
import java.util.Collections;

public class EPCompilerSPIExpressionImpl implements EPCompilerSPIExpression {
    private final ModuleCompileTimeServices moduleServices;

    public EPCompilerSPIExpressionImpl(ModuleCompileTimeServices moduleServices) {
        this.moduleServices = moduleServices;
    }

    public ExprNode compileExpression(String epl, EventType[] eventTypes, String[] streamNames) throws EPException {
        try {
            return compileValidate(epl, eventTypes, streamNames);
        } catch (Throwable t) {
            throw new EPException(t.getMessage(), t);
        }
    }

    public ExprNode compileValidate(String expression) throws EPCompileException {
        return compileValidate(expression, null, null);
    }

    public ExprNode compileValidate(String expression, EventType[] eventTypes, String[] streamNnames) throws EPCompileException {
        StatementCompileTimeServices services = new StatementCompileTimeServices(0, moduleServices);

        ExprNode node;
        try {
            node = services.getCompilerServices().compileExpression(expression, services);
        } catch (ExprValidationException e) {
            throw new EPCompileException("Failed to compile expression '" + expression + "': " + e.getMessage(), e, Collections.emptyList());
        }

        try {
            ExprNodeUtilityValidate.validatePlainExpression(ExprNodeOrigin.API, node);

            StreamTypeService streamTypeService;
            if (eventTypes == null || eventTypes.length == 0) {
                streamTypeService = new StreamTypeServiceImpl(true);
            } else {
                boolean[] istreamOnly = new boolean[eventTypes.length];
                Arrays.fill(istreamOnly, true);
                streamTypeService = new StreamTypeServiceImpl(eventTypes, streamNnames, istreamOnly, true, false);
            }

            StatementRawInfo statementRawInfo = new StatementRawInfo(0, "API-provided", null, StatementType.INTERNAL_USE_API_COMPILE_EXPR, null, null, new CompilableEPL(expression, 1), "API-provided");
            ExprValidationContext validationContext = new ExprValidationContextBuilder(streamTypeService, statementRawInfo, services).build();
            node = ExprNodeUtilityValidate.getValidatedSubtree(ExprNodeOrigin.API, node, validationContext);
        } catch (ExprValidationException e) {
            throw new EPCompileException("Failed to validate expression '" + expression + "': " + e.getMessage(), e, Collections.emptyList());
        }

        return node;
    }
}

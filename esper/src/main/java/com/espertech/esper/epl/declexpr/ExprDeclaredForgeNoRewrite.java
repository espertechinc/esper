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
package com.espertech.esper.epl.declexpr;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.codegen.base.CodegenClassScope;
import com.espertech.esper.codegen.base.CodegenMethodScope;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.epl.expression.core.ExprForge;

public class ExprDeclaredForgeNoRewrite extends ExprDeclaredForgeBase {

    public ExprDeclaredForgeNoRewrite(ExprDeclaredNodeImpl parent, ExprForge innerForge, boolean isCache, boolean audit, String engineURI, String statementName) {
        super(parent, innerForge, isCache, audit, engineURI, statementName);
    }

    public EventBean[] getEventsPerStreamRewritten(EventBean[] eventsPerStream) {
        return eventsPerStream;
    }

    protected CodegenExpression codegenEventsPerStreamRewritten(CodegenExpression eventsPerStream, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return eventsPerStream;
    }
}
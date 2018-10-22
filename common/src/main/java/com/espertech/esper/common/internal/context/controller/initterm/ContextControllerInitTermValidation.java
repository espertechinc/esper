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
package com.espertech.esper.common.internal.context.controller.initterm;

import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRef;
import com.espertech.esper.common.internal.compile.stage2.StatementSpecCompiled;
import com.espertech.esper.common.internal.compile.stage3.StatementCompileTimeServices;
import com.espertech.esper.common.internal.context.controller.core.ContextControllerPortableInfo;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.publicConstValue;

public class ContextControllerInitTermValidation implements ContextControllerPortableInfo {
    public static final ContextControllerPortableInfo INSTANCE = new ContextControllerInitTermValidation();

    private ContextControllerInitTermValidation() {
    }

    public CodegenExpression make(CodegenExpressionRef addInitSvc) {
        return publicConstValue(ContextControllerInitTermValidation.class, "INSTANCE");
    }

    public void validateStatement(String contextName, StatementSpecCompiled spec, StatementCompileTimeServices compileTimeServices) throws ExprValidationException {
    }
}

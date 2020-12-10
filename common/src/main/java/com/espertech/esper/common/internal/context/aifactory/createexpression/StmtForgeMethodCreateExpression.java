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
package com.espertech.esper.common.internal.context.aifactory.createexpression;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.util.NameAccessModifier;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenPackageScope;
import com.espertech.esper.common.internal.compile.stage1.spec.CreateExpressionDesc;
import com.espertech.esper.common.internal.compile.stage1.spec.ExpressionDeclItem;
import com.espertech.esper.common.internal.compile.stage1.spec.ExpressionScriptProvided;
import com.espertech.esper.common.internal.compile.stage3.StatementBaseInfo;
import com.espertech.esper.common.internal.compile.stage3.StatementCompileTimeServices;
import com.espertech.esper.common.internal.compile.stage3.StmtClassForgeable;
import com.espertech.esper.common.internal.context.aifactory.core.StmtForgeMethodCreateSimpleBase;
import com.espertech.esper.common.internal.context.aifactory.core.StmtForgeMethodRegisterResult;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;

public class StmtForgeMethodCreateExpression extends StmtForgeMethodCreateSimpleBase {

    public StmtForgeMethodCreateExpression(StatementBaseInfo base) {
        super(base);
    }

    protected StmtForgeMethodRegisterResult register(StatementCompileTimeServices services) throws ExprValidationException {
        CreateExpressionDesc spec = base.getStatementSpec().getRaw().getCreateExpressionDesc();

        String expressionName;
        if (spec.getExpression() != null) {
            // register expression
            expressionName = spec.getExpression().getName();
            NameAccessModifier visibility = services.getModuleVisibilityRules().getAccessModifierExpression(base, expressionName);
            checkAlreadyDeclared(expressionName, services, -1);
            ExpressionDeclItem item = spec.getExpression();
            item.setModuleName(base.getModuleName());
            item.setVisibility(visibility);
            services.getExprDeclaredCompileTimeRegistry().newExprDeclared(item);
        } else {
            // register script
            expressionName = spec.getScript().getName();
            int numParameters = spec.getScript().getParameterNames().length;
            checkAlreadyDeclared(expressionName, services, numParameters);
            NameAccessModifier visibility = services.getModuleVisibilityRules().getAccessModifierScript(base, expressionName, numParameters);
            ExpressionScriptProvided item = spec.getScript();
            item.setModuleName(base.getModuleName());
            item.setVisibility(visibility);
            services.getScriptCompileTimeRegistry().newScript(item);
        }
        return new StmtForgeMethodRegisterResult(expressionName, services.getStateMgmtSettingsProvider().newCharge());
    }

    protected StmtClassForgeable aiFactoryForgable(String className, CodegenPackageScope packageScope, EventType statementEventType, String objectName) {
        StatementAgentInstanceFactoryCreateExpressionForge forge = new StatementAgentInstanceFactoryCreateExpressionForge(statementEventType, objectName);
        return new StmtClassForgeableAIFactoryProviderCreateExpression(className, packageScope, forge);
    }

    private void checkAlreadyDeclared(String expressionName, StatementCompileTimeServices services, int numParameters)
            throws ExprValidationException {
        if (services.getExprDeclaredCompileTimeResolver().resolve(expressionName) != null) {
            throw new ExprValidationException("Expression '" + expressionName + "' has already been declared");
        }
        if (services.getScriptCompileTimeResolver().resolve(expressionName, numParameters) != null) {
            throw new ExprValidationException("Script '" + expressionName + "' that takes the same number of parameters has already been declared");
        }
    }
}

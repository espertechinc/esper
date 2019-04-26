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
import com.espertech.esper.common.client.meta.EventTypeApplicationType;
import com.espertech.esper.common.client.meta.EventTypeIdPair;
import com.espertech.esper.common.client.meta.EventTypeMetadata;
import com.espertech.esper.common.client.meta.EventTypeTypeClass;
import com.espertech.esper.common.client.util.EventTypeBusModifier;
import com.espertech.esper.common.client.util.NameAccessModifier;
import com.espertech.esper.common.client.util.StatementProperty;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenPackageScope;
import com.espertech.esper.common.internal.bytecodemodel.core.CodeGenerationIDGenerator;
import com.espertech.esper.common.internal.compile.stage1.spec.CreateExpressionDesc;
import com.espertech.esper.common.internal.compile.stage1.spec.ExpressionDeclItem;
import com.espertech.esper.common.internal.compile.stage1.spec.ExpressionScriptProvided;
import com.espertech.esper.common.internal.compile.stage3.*;
import com.espertech.esper.common.internal.context.module.StatementAIFactoryProvider;
import com.espertech.esper.common.internal.context.module.StatementInformationalsCompileTime;
import com.espertech.esper.common.internal.context.module.StatementProvider;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;
import com.espertech.esper.common.internal.epl.resultset.select.core.SelectSubscriberDescriptor;
import com.espertech.esper.common.internal.event.core.BaseNestableEventUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class StmtForgeMethodCreateExpression implements StmtForgeMethod {

    private final StatementBaseInfo base;

    public StmtForgeMethodCreateExpression(StatementBaseInfo base) {
        this.base = base;
    }

    public StmtForgeMethodResult make(String packageName, String classPostfix, StatementCompileTimeServices services) throws ExprValidationException {
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

        // define output event type
        String statementEventTypeName = services.getEventTypeNameGeneratorStatement().getAnonymousTypeName();
        EventTypeMetadata statementTypeMetadata = new EventTypeMetadata(statementEventTypeName, base.getModuleName(), EventTypeTypeClass.STATEMENTOUT, EventTypeApplicationType.MAP, NameAccessModifier.TRANSIENT, EventTypeBusModifier.NONBUS, false, EventTypeIdPair.unassigned());
        EventType statementEventType = BaseNestableEventUtil.makeMapTypeCompileTime(statementTypeMetadata, Collections.emptyMap(), null, null, null, null, services.getBeanEventTypeFactoryPrivate(), services.getEventTypeCompileTimeResolver());
        services.getEventTypeCompileTimeRegistry().newType(statementEventType);

        CodegenPackageScope packageScope = new CodegenPackageScope(packageName, null, services.isInstrumented());

        String aiFactoryProviderClassName = CodeGenerationIDGenerator.generateClassNameSimple(StatementAIFactoryProvider.class, classPostfix);
        StatementAgentInstanceFactoryCreateExpressionForge forge = new StatementAgentInstanceFactoryCreateExpressionForge(statementEventType, expressionName);
        StmtClassForgeableAIFactoryProviderCreateExpression aiFactoryForgeable = new StmtClassForgeableAIFactoryProviderCreateExpression(aiFactoryProviderClassName, packageScope, forge);

        SelectSubscriberDescriptor selectSubscriberDescriptor = new SelectSubscriberDescriptor();
        StatementInformationalsCompileTime informationals = StatementInformationalsUtil.getInformationals(base, Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), false, selectSubscriberDescriptor, packageScope, services);
        informationals.getProperties().put(StatementProperty.CREATEOBJECTNAME, expressionName);
        String statementProviderClassName = CodeGenerationIDGenerator.generateClassNameSimple(StatementProvider.class, classPostfix);
        StmtClassForgeableStmtProvider stmtProvider = new StmtClassForgeableStmtProvider(aiFactoryProviderClassName, statementProviderClassName, informationals, packageScope);

        List<StmtClassForgeable> forgeables = new ArrayList<>();
        forgeables.add(aiFactoryForgeable);
        forgeables.add(stmtProvider);
        return new StmtForgeMethodResult(forgeables, Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
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

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
package com.espertech.esper.common.internal.context.aifactory.createvariable;

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
import com.espertech.esper.common.internal.compile.stage1.spec.CreateVariableDesc;
import com.espertech.esper.common.internal.compile.stage1.spec.SelectClauseElementWildcard;
import com.espertech.esper.common.internal.compile.stage1.spec.SelectClauseStreamSelectorEnum;
import com.espertech.esper.common.internal.compile.stage2.StatementSpecCompiled;
import com.espertech.esper.common.internal.compile.stage3.*;
import com.espertech.esper.common.internal.context.compile.ContextMetaData;
import com.espertech.esper.common.internal.context.module.StatementAIFactoryProvider;
import com.espertech.esper.common.internal.context.module.StatementFields;
import com.espertech.esper.common.internal.context.module.StatementInformationalsCompileTime;
import com.espertech.esper.common.internal.context.module.StatementProvider;
import com.espertech.esper.common.internal.epl.expression.core.*;
import com.espertech.esper.common.internal.epl.resultset.core.ResultSetProcessorDesc;
import com.espertech.esper.common.internal.epl.resultset.core.ResultSetProcessorFactoryFactory;
import com.espertech.esper.common.internal.epl.resultset.core.ResultSetProcessorFactoryProvider;
import com.espertech.esper.common.internal.epl.resultset.core.ResultSetSpec;
import com.espertech.esper.common.internal.epl.streamtype.StreamTypeService;
import com.espertech.esper.common.internal.epl.streamtype.StreamTypeServiceImpl;
import com.espertech.esper.common.internal.epl.util.EPLValidationUtil;
import com.espertech.esper.common.internal.epl.variable.compiletime.VariableMetaData;
import com.espertech.esper.common.internal.epl.variable.core.VariableUtil;
import com.espertech.esper.common.internal.event.core.BaseNestableEventUtil;
import com.espertech.esper.common.internal.event.core.EventBeanTypedEventFactoryCompileTime;
import com.espertech.esper.common.internal.event.map.MapEventType;
import com.espertech.esper.common.internal.serde.compiletime.resolve.DataInputOutputSerdeForge;
import com.espertech.esper.common.internal.serde.compiletime.resolve.DataInputOutputSerdeForgeNotApplicable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class StmtForgeMethodCreateVariable implements StmtForgeMethod {

    private final StatementBaseInfo base;

    public StmtForgeMethodCreateVariable(StatementBaseInfo base) {
        this.base = base;
    }

    public StmtForgeMethodResult make(String packageName, String classPostfix, StatementCompileTimeServices services) throws ExprValidationException {
        StatementSpecCompiled statementSpec = base.getStatementSpec();

        CreateVariableDesc createDesc = statementSpec.getRaw().getCreateVariableDesc();

        // Check if the variable is already declared
        EPLValidationUtil.validateAlreadyExistsTableOrVariable(createDesc.getVariableName(), services.getVariableCompileTimeResolver(), services.getTableCompileTimeResolver(), services.getEventTypeCompileTimeResolver());

        // Get assignment value when compile-time-constant
        Object initialValue = null;
        ExprForge initialValueExpr = null;
        if (createDesc.getAssignment() != null) {
            // Evaluate assignment expression
            StreamTypeService typeService = new StreamTypeServiceImpl(new EventType[0], new String[0], new boolean[0], false, false);
            ExprValidationContext validationContext = new ExprValidationContextBuilder(typeService, base.getStatementRawInfo(), services).build();
            ExprNode validated = ExprNodeUtilityValidate.getValidatedSubtree(ExprNodeOrigin.VARIABLEASSIGN, createDesc.getAssignment(), validationContext);
            if (validated.getForge().getForgeConstantType() == ExprForgeConstantType.COMPILETIMECONST) {
                initialValue = validated.getForge().getExprEvaluator().evaluate(null, true, null);
            }
            createDesc.setAssignment(validated);
            initialValueExpr = validated.getForge();
        }


        String contextName = statementSpec.getRaw().getOptionalContextName();
        NameAccessModifier contextVisibility = null;
        String contextModuleName = null;
        if (contextName != null) {
            ContextMetaData contextDetail = services.getContextCompileTimeResolver().getContextInfo(contextName);
            if (contextDetail == null) {
                throw new ExprValidationException("Failed to find context '" + contextName + "'");
            }
            contextVisibility = contextDetail.getContextVisibility();
            contextModuleName = contextDetail.getContextModuleName();
        }

        // get visibility
        NameAccessModifier visibility = services.getModuleVisibilityRules().getAccessModifierVariable(base, createDesc.getVariableName());

        // Compile metadata
        boolean compileTimeConstant = createDesc.isConstant() && initialValueExpr != null && initialValueExpr.getForgeConstantType().isCompileTimeConstant();
        VariableMetaData metaData = VariableUtil.compileVariable(createDesc.getVariableName(), base.getModuleName(), visibility, contextName, contextVisibility, contextModuleName, createDesc.getVariableType(),
                createDesc.isConstant(), compileTimeConstant, initialValue, services.getClasspathImportServiceCompileTime(), EventBeanTypedEventFactoryCompileTime.INSTANCE, services.getEventTypeRepositoryPreconfigured(), services.getBeanEventTypeFactoryPrivate());

        // Register variable
        services.getVariableCompileTimeRegistry().newVariable(metaData);

        // Statement event type
        Map<String, Object> eventTypePropertyTypes = Collections.singletonMap(metaData.getVariableName(), metaData.getType());
        String eventTypeName = services.getEventTypeNameGeneratorStatement().getAnonymousTypeName();
        EventTypeMetadata eventTypeMetadata = new EventTypeMetadata(eventTypeName, base.getModuleName(), EventTypeTypeClass.STATEMENTOUT, EventTypeApplicationType.MAP, NameAccessModifier.TRANSIENT, EventTypeBusModifier.NONBUS, false, EventTypeIdPair.unassigned());
        MapEventType outputEventType = BaseNestableEventUtil.makeMapTypeCompileTime(eventTypeMetadata, eventTypePropertyTypes, null, null, null, null, services.getBeanEventTypeFactoryPrivate(), services.getEventTypeCompileTimeResolver());
        services.getEventTypeCompileTimeRegistry().newType(outputEventType);

        // Handle output format
        StatementSpecCompiled defaultSelectAllSpec = new StatementSpecCompiled();
        defaultSelectAllSpec.getSelectClauseCompiled().setSelectExprList(new SelectClauseElementWildcard());
        defaultSelectAllSpec.getRaw().setSelectStreamDirEnum(SelectClauseStreamSelectorEnum.RSTREAM_ISTREAM_BOTH);
        StreamTypeService streamTypeService = new StreamTypeServiceImpl(new EventType[]{outputEventType}, new String[]{"trigger_stream"}, new boolean[]{true}, false, false);
        ResultSetProcessorDesc resultSetProcessor = ResultSetProcessorFactoryFactory.getProcessorPrototype(new ResultSetSpec(defaultSelectAllSpec),
                streamTypeService, null, new boolean[1], false, base.getContextPropertyRegistry(), false, false, base.getStatementRawInfo(), services);

        // Code generation
        String statementFieldsClassName = CodeGenerationIDGenerator.generateClassNameSimple(StatementFields.class, classPostfix);
        String aiFactoryProviderClassName = CodeGenerationIDGenerator.generateClassNameSimple(StatementAIFactoryProvider.class, classPostfix);
        String classNameRSP = CodeGenerationIDGenerator.generateClassNameSimple(ResultSetProcessorFactoryProvider.class, classPostfix);
        CodegenPackageScope packageScope = new CodegenPackageScope(packageName, statementFieldsClassName, services.isInstrumented());

        // serde
        DataInputOutputSerdeForge serde = DataInputOutputSerdeForgeNotApplicable.INSTANCE;
        if (metaData.getEventType() == null) {
            serde = services.getSerdeResolver().serdeForVariable(metaData.getType(), metaData.getVariableName(), base.getStatementRawInfo());
        }

        StatementAgentInstanceFactoryCreateVariableForge forge = new StatementAgentInstanceFactoryCreateVariableForge(createDesc.getVariableName(), initialValueExpr, classNameRSP);
        StmtClassForgeableAIFactoryProviderCreateVariable aiFactoryForgeable = new StmtClassForgeableAIFactoryProviderCreateVariable(aiFactoryProviderClassName, packageScope, forge, createDesc.getVariableName(), serde);

        StatementInformationalsCompileTime informationals = StatementInformationalsUtil.getInformationals(base, Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), true, resultSetProcessor.getSelectSubscriberDescriptor(), packageScope, services);
        informationals.getProperties().put(StatementProperty.CREATEOBJECTNAME, createDesc.getVariableName());
        String statementProviderClassName = CodeGenerationIDGenerator.generateClassNameSimple(StatementProvider.class, classPostfix);
        StmtClassForgeableStmtProvider stmtProvider = new StmtClassForgeableStmtProvider(aiFactoryProviderClassName, statementProviderClassName, informationals, packageScope);

        List<StmtClassForgeable> forgeables = new ArrayList<>();
        forgeables.add(new StmtClassForgeableRSPFactoryProvider(classNameRSP, resultSetProcessor, packageScope, base.getStatementRawInfo()));
        forgeables.add(aiFactoryForgeable);
        forgeables.add(stmtProvider);
        forgeables.add(new StmtClassForgeableStmtFields(statementFieldsClassName, packageScope, 0));
        return new StmtForgeMethodResult(forgeables, Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
    }
}

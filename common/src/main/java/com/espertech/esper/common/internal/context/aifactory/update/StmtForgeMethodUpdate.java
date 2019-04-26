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
package com.espertech.esper.common.internal.context.aifactory.update;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenPackageScope;
import com.espertech.esper.common.internal.bytecodemodel.core.CodeGenerationIDGenerator;
import com.espertech.esper.common.internal.compile.stage1.spec.*;
import com.espertech.esper.common.internal.compile.stage2.FilterSpecCompiled;
import com.espertech.esper.common.internal.compile.stage2.FilterStreamSpecCompiled;
import com.espertech.esper.common.internal.compile.stage2.StatementSpecCompiled;
import com.espertech.esper.common.internal.compile.stage3.*;
import com.espertech.esper.common.internal.context.module.StatementAIFactoryProvider;
import com.espertech.esper.common.internal.context.module.StatementFields;
import com.espertech.esper.common.internal.context.module.StatementInformationalsCompileTime;
import com.espertech.esper.common.internal.context.module.StatementProvider;
import com.espertech.esper.common.internal.epl.expression.core.*;
import com.espertech.esper.common.internal.epl.expression.subquery.ExprSubselectNode;
import com.espertech.esper.common.internal.epl.resultset.select.core.SelectSubscriberDescriptor;
import com.espertech.esper.common.internal.epl.streamtype.StreamTypeService;
import com.espertech.esper.common.internal.epl.streamtype.StreamTypeServiceImpl;
import com.espertech.esper.common.internal.epl.subselect.*;
import com.espertech.esper.common.internal.statement.helper.EPStatementStartMethodHelperValidate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class StmtForgeMethodUpdate implements StmtForgeMethod {

    private final StatementBaseInfo base;

    public StmtForgeMethodUpdate(StatementBaseInfo base) {
        this.base = base;
    }

    public StmtForgeMethodResult make(String packageName, String classPostfix, StatementCompileTimeServices services) throws ExprValidationException {
        StatementSpecCompiled statementSpec = base.getStatementSpec();

        // determine context
        final String contextName = base.getStatementSpec().getRaw().getOptionalContextName();
        if (contextName != null) {
            throw new ExprValidationException("Update IStream is not supported in conjunction with a context");
        }

        StreamSpecCompiled streamSpec = statementSpec.getStreamSpecs()[0];
        UpdateDesc updateSpec = statementSpec.getRaw().getUpdateDesc();
        List<StmtClassForgeableFactory> additionalForgeables = new ArrayList<>(2);
        String triggereventTypeName;
        EventType streamEventType;

        if (streamSpec instanceof FilterStreamSpecCompiled) {
            FilterStreamSpecCompiled filterStreamSpec = (FilterStreamSpecCompiled) streamSpec;
            triggereventTypeName = filterStreamSpec.getFilterSpecCompiled().getFilterForEventTypeName();
            streamEventType = filterStreamSpec.getFilterSpecCompiled().getFilterForEventType();
        } else if (streamSpec instanceof NamedWindowConsumerStreamSpec) {
            NamedWindowConsumerStreamSpec namedSpec = (NamedWindowConsumerStreamSpec) streamSpec;
            streamEventType = namedSpec.getNamedWindow().getEventType();
            triggereventTypeName = streamEventType.getName();
        } else if (streamSpec instanceof TableQueryStreamSpec) {
            throw new ExprValidationException("Tables cannot be used in an update-istream statement");
        } else {
            throw new ExprValidationException("Unknown stream specification streamEventType: " + streamSpec);
        }

        // determine a stream name
        String streamName = triggereventTypeName;
        if (updateSpec.getOptionalStreamName() != null) {
            streamName = updateSpec.getOptionalStreamName();
        }
        StreamTypeService typeService = new StreamTypeServiceImpl(new EventType[]{streamEventType}, new String[]{streamName}, new boolean[]{true}, false, false);

        // create subselect information
        List<FilterSpecCompiled> filterSpecCompileds = new ArrayList<>();
        List<NamedWindowConsumerStreamSpec> namedWindowConsumers = new ArrayList<>();
        SubSelectActivationDesc subSelectActivationDesc = SubSelectHelperActivations.createSubSelectActivation(filterSpecCompileds, namedWindowConsumers, base, services);
        additionalForgeables.addAll(subSelectActivationDesc.getAdditionalForgeables());
        Map<ExprSubselectNode, SubSelectActivationPlan> subselectActivation = subSelectActivationDesc.getSubselects();

        // handle subselects
        SubSelectHelperForgePlan subSelectForgePlan = SubSelectHelperForgePlanner.planSubSelect(base, subselectActivation, typeService.getStreamNames(), typeService.getEventTypes(), new String[]{triggereventTypeName}, services);
        Map<ExprSubselectNode, SubSelectFactoryForge> subselectForges = subSelectForgePlan.getSubselects();
        additionalForgeables.addAll(subSelectForgePlan.getAdditionalForgeables());

        ExprValidationContext validationContext = new ExprValidationContextBuilder(typeService, base.getStatementRawInfo(), services).build();

        for (OnTriggerSetAssignment assignment : updateSpec.getAssignments()) {
            ExprNode validated = ExprNodeUtilityValidate.getValidatedAssignment(assignment, validationContext);
            assignment.setExpression(validated);
            EPStatementStartMethodHelperValidate.validateNoAggregations(validated, "Aggregation functions may not be used within an update-clause");
        }
        if (updateSpec.getOptionalWhereClause() != null) {
            ExprNode validated = ExprNodeUtilityValidate.getValidatedSubtree(ExprNodeOrigin.WHERE, updateSpec.getOptionalWhereClause(), validationContext);
            updateSpec.setOptionalWhereClause(validated);
            EPStatementStartMethodHelperValidate.validateNoAggregations(validated, "Aggregation functions may not be used within an update-clause");
        }

        // build route information
        InternalEventRouterDescForge routerDesc = InternalEventRouterDescFactory.getValidatePreprocessing(streamEventType, updateSpec, base.getStatementRawInfo().getAnnotations());

        String statementFieldsClassName = CodeGenerationIDGenerator.generateClassNameSimple(StatementFields.class, classPostfix);
        CodegenPackageScope packageScope = new CodegenPackageScope(packageName, statementFieldsClassName, services.isInstrumented());

        String aiFactoryProviderClassName = CodeGenerationIDGenerator.generateClassNameSimple(StatementAIFactoryProvider.class, classPostfix);
        StatementAgentInstanceFactoryUpdateForge forge = new StatementAgentInstanceFactoryUpdateForge(routerDesc, subselectForges);
        StmtClassForgeableAIFactoryProviderUpdate aiFactoryForgeable = new StmtClassForgeableAIFactoryProviderUpdate(aiFactoryProviderClassName, packageScope, forge);

        SelectSubscriberDescriptor selectSubscriberDescriptor = new SelectSubscriberDescriptor(new Class[]{streamEventType.getUnderlyingType()},
                new String[]{"*"}, false, null, null);
        StatementInformationalsCompileTime informationals = StatementInformationalsUtil.getInformationals(base, filterSpecCompileds, Collections.emptyList(), Collections.emptyList(), false, selectSubscriberDescriptor, packageScope, services);
        String statementProviderClassName = CodeGenerationIDGenerator.generateClassNameSimple(StatementProvider.class, classPostfix);
        StmtClassForgeableStmtProvider stmtProvider = new StmtClassForgeableStmtProvider(aiFactoryProviderClassName, statementProviderClassName, informationals, packageScope);

        List<StmtClassForgeable> forgeables = new ArrayList<>();
        for (StmtClassForgeableFactory additional : additionalForgeables) {
            forgeables.add(additional.make(packageScope, classPostfix));
        }
        forgeables.add(aiFactoryForgeable);
        forgeables.add(stmtProvider);
        forgeables.add(new StmtClassForgeableStmtFields(statementFieldsClassName, packageScope, 0));
        return new StmtForgeMethodResult(forgeables, filterSpecCompileds, Collections.emptyList(), namedWindowConsumers, Collections.emptyList());
    }
}

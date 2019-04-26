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
package com.espertech.esper.common.internal.context.aifactory.ontrigger.onset;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.meta.EventTypeApplicationType;
import com.espertech.esper.common.client.meta.EventTypeIdPair;
import com.espertech.esper.common.client.meta.EventTypeMetadata;
import com.espertech.esper.common.client.meta.EventTypeTypeClass;
import com.espertech.esper.common.client.util.EventTypeBusModifier;
import com.espertech.esper.common.client.util.NameAccessModifier;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenPackageScope;
import com.espertech.esper.common.internal.bytecodemodel.core.CodeGenerationIDGenerator;
import com.espertech.esper.common.internal.compile.stage1.spec.OnTriggerSetAssignment;
import com.espertech.esper.common.internal.compile.stage1.spec.OnTriggerSetDesc;
import com.espertech.esper.common.internal.compile.stage1.spec.SelectClauseElementWildcard;
import com.espertech.esper.common.internal.compile.stage1.spec.SelectClauseStreamSelectorEnum;
import com.espertech.esper.common.internal.compile.stage2.StatementSpecCompiled;
import com.espertech.esper.common.internal.compile.stage3.StatementBaseInfo;
import com.espertech.esper.common.internal.compile.stage3.StatementCompileTimeServices;
import com.espertech.esper.common.internal.compile.stage3.StmtClassForgeable;
import com.espertech.esper.common.internal.compile.stage3.StmtClassForgeableRSPFactoryProvider;
import com.espertech.esper.common.internal.context.aifactory.ontrigger.core.OnTriggerActivatorDesc;
import com.espertech.esper.common.internal.context.aifactory.ontrigger.core.StmtClassForgeableAIFactoryProviderOnTrigger;
import com.espertech.esper.common.internal.epl.expression.core.*;
import com.espertech.esper.common.internal.epl.expression.subquery.ExprSubselectNode;
import com.espertech.esper.common.internal.epl.expression.table.ExprTableAccessNode;
import com.espertech.esper.common.internal.epl.resultset.core.ResultSetProcessorDesc;
import com.espertech.esper.common.internal.epl.resultset.core.ResultSetProcessorFactoryFactory;
import com.espertech.esper.common.internal.epl.resultset.core.ResultSetProcessorFactoryProvider;
import com.espertech.esper.common.internal.epl.resultset.core.ResultSetSpec;
import com.espertech.esper.common.internal.epl.streamtype.StreamTypeService;
import com.espertech.esper.common.internal.epl.streamtype.StreamTypeServiceImpl;
import com.espertech.esper.common.internal.epl.subselect.SubSelectActivationPlan;
import com.espertech.esper.common.internal.epl.subselect.SubSelectFactoryForge;
import com.espertech.esper.common.internal.epl.subselect.SubSelectHelperForgePlan;
import com.espertech.esper.common.internal.epl.subselect.SubSelectHelperForgePlanner;
import com.espertech.esper.common.internal.epl.table.strategy.ExprTableEvalHelperPlan;
import com.espertech.esper.common.internal.epl.table.strategy.ExprTableEvalStrategyFactoryForge;
import com.espertech.esper.common.internal.epl.variable.core.VariableReadWritePackageForge;
import com.espertech.esper.common.internal.event.core.BaseNestableEventUtil;
import com.espertech.esper.common.internal.event.map.MapEventType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class OnTriggerSetUtil {
    public static OnTriggerSetPlan handleSetVariable(String className,
                                                     CodegenPackageScope packageScope,
                                                     String classPostfix,
                                                     OnTriggerActivatorDesc activatorResult,
                                                     String optionalStreamName,
                                                     Map<ExprSubselectNode, SubSelectActivationPlan> subselectActivation,
                                                     OnTriggerSetDesc desc,
                                                     StatementBaseInfo base,
                                                     StatementCompileTimeServices services)
            throws ExprValidationException {

        StreamTypeService typeService = new StreamTypeServiceImpl(new EventType[]{activatorResult.getActivatorResultEventType()}, new String[]{optionalStreamName}, new boolean[]{true}, false, false);
        ExprValidationContext validationContext = new ExprValidationContextBuilder(typeService, base.getStatementRawInfo(), services).withAllowBindingConsumption(true).build();

        // handle subselects
        SubSelectHelperForgePlan subSelectForgePlan = SubSelectHelperForgePlanner.planSubSelect(base, subselectActivation, new String[]{optionalStreamName}, new EventType[]{activatorResult.getActivatorResultEventType()}, new String[]{activatorResult.getTriggerEventTypeName()}, services);
        Map<ExprSubselectNode, SubSelectFactoryForge> subselectForges = subSelectForgePlan.getSubselects();

        // validate assignments
        for (OnTriggerSetAssignment assignment : desc.getAssignments()) {
            ExprNode validated = ExprNodeUtilityValidate.getValidatedAssignment(assignment, validationContext);
            assignment.setExpression(validated);
        }

        // create read-write logic
        VariableReadWritePackageForge variableReadWritePackageForge = new VariableReadWritePackageForge(desc.getAssignments(), services);

        // plan table access
        Map<ExprTableAccessNode, ExprTableEvalStrategyFactoryForge> tableAccessForges = ExprTableEvalHelperPlan.planTableAccess(base.getStatementSpec().getTableAccessNodes());

        // create output event type
        String eventTypeName = services.getEventTypeNameGeneratorStatement().getAnonymousTypeName();
        EventTypeMetadata eventTypeMetadata = new EventTypeMetadata(eventTypeName, base.getModuleName(), EventTypeTypeClass.STATEMENTOUT, EventTypeApplicationType.MAP, NameAccessModifier.TRANSIENT, EventTypeBusModifier.NONBUS, false, EventTypeIdPair.unassigned());
        MapEventType eventType = BaseNestableEventUtil.makeMapTypeCompileTime(eventTypeMetadata, variableReadWritePackageForge.getVariableTypes(), null, null, null, null, services.getBeanEventTypeFactoryPrivate(), services.getEventTypeCompileTimeResolver());
        services.getEventTypeCompileTimeRegistry().newType(eventType);

        // Handle output format
        StatementSpecCompiled defaultSelectAllSpec = new StatementSpecCompiled();
        defaultSelectAllSpec.getSelectClauseCompiled().setSelectExprList(new SelectClauseElementWildcard());
        defaultSelectAllSpec.getRaw().setSelectStreamDirEnum(SelectClauseStreamSelectorEnum.RSTREAM_ISTREAM_BOTH);
        StreamTypeService streamTypeService = new StreamTypeServiceImpl(new EventType[]{eventType}, new String[]{"trigger_stream"}, new boolean[]{true}, false, false);
        ResultSetProcessorDesc resultSetProcessor = ResultSetProcessorFactoryFactory.getProcessorPrototype(new ResultSetSpec(defaultSelectAllSpec),
                streamTypeService, null, new boolean[1], false, base.getContextPropertyRegistry(), false, false, base.getStatementRawInfo(), services);
        String classNameRSP = CodeGenerationIDGenerator.generateClassNameSimple(ResultSetProcessorFactoryProvider.class, classPostfix);

        StatementAgentInstanceFactoryOnTriggerSetForge forge = new StatementAgentInstanceFactoryOnTriggerSetForge(activatorResult.getActivator(), eventType, subselectForges, tableAccessForges, variableReadWritePackageForge, classNameRSP);
        List<StmtClassForgeable> forgeables = new ArrayList<>();
        forgeables.add(new StmtClassForgeableRSPFactoryProvider(classNameRSP, resultSetProcessor, packageScope, base.getStatementRawInfo()));

        StmtClassForgeableAIFactoryProviderOnTrigger onTrigger = new StmtClassForgeableAIFactoryProviderOnTrigger(className, packageScope, forge);
        return new OnTriggerSetPlan(onTrigger, forgeables, resultSetProcessor.getSelectSubscriberDescriptor(), subSelectForgePlan.getAdditionalForgeables());
    }
}

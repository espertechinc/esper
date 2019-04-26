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
package com.espertech.esper.common.internal.context.aifactory.ontrigger.core;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.soda.StreamSelector;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenPackageScope;
import com.espertech.esper.common.internal.bytecodemodel.core.CodeGenerationIDGenerator;
import com.espertech.esper.common.internal.compile.stage1.spec.*;
import com.espertech.esper.common.internal.compile.stage2.FilterSpecCompiled;
import com.espertech.esper.common.internal.compile.stage2.FilterStreamSpecCompiled;
import com.espertech.esper.common.internal.compile.stage3.*;
import com.espertech.esper.common.internal.context.activator.ViewableActivatorFilterForge;
import com.espertech.esper.common.internal.context.activator.ViewableActivatorNamedWindowForge;
import com.espertech.esper.common.internal.context.activator.ViewableActivatorPatternForge;
import com.espertech.esper.common.internal.context.aifactory.ontrigger.onset.OnTriggerSetPlan;
import com.espertech.esper.common.internal.context.aifactory.ontrigger.onset.OnTriggerSetUtil;
import com.espertech.esper.common.internal.context.aifactory.ontrigger.onsplit.OnSplitStreamUtil;
import com.espertech.esper.common.internal.context.aifactory.ontrigger.ontrigger.OnTriggerPlan;
import com.espertech.esper.common.internal.context.aifactory.ontrigger.ontrigger.OnTriggerWindowPlan;
import com.espertech.esper.common.internal.context.aifactory.ontrigger.ontrigger.OnTriggerWindowUtil;
import com.espertech.esper.common.internal.context.module.StatementAIFactoryProvider;
import com.espertech.esper.common.internal.context.module.StatementFields;
import com.espertech.esper.common.internal.context.module.StatementInformationalsCompileTime;
import com.espertech.esper.common.internal.context.module.StatementProvider;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.epl.expression.core.ExprNodeUtilityMake;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;
import com.espertech.esper.common.internal.epl.expression.subquery.ExprSubselectNode;
import com.espertech.esper.common.internal.epl.join.querygraph.QueryGraphForge;
import com.espertech.esper.common.internal.epl.namedwindow.path.NamedWindowMetaData;
import com.espertech.esper.common.internal.epl.pattern.core.EvalForgeNode;
import com.espertech.esper.common.internal.epl.pattern.core.PatternContext;
import com.espertech.esper.common.internal.epl.streamtype.StreamTypeServiceImpl;
import com.espertech.esper.common.internal.epl.subselect.SubSelectActivationDesc;
import com.espertech.esper.common.internal.epl.subselect.SubSelectActivationPlan;
import com.espertech.esper.common.internal.epl.subselect.SubSelectHelperActivations;
import com.espertech.esper.common.internal.epl.table.compiletime.TableMetaData;
import com.espertech.esper.common.internal.epl.util.EPLValidationUtil;
import com.espertech.esper.common.internal.event.map.MapEventType;
import com.espertech.esper.common.internal.schedule.ScheduleHandleCallbackProvider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class StmtForgeMethodOnTrigger implements StmtForgeMethod {
    private final StatementBaseInfo base;

    public StmtForgeMethodOnTrigger(StatementBaseInfo base) {
        this.base = base;
    }

    public StmtForgeMethodResult make(String packageName, String classPostfix, StatementCompileTimeServices services) throws ExprValidationException {
        // determine context
        final String contextName = base.getStatementSpec().getRaw().getOptionalContextName();

        List<FilterSpecCompiled> filterSpecCompileds = new ArrayList<>();
        List<ScheduleHandleCallbackProvider> schedules = new ArrayList<>();
        List<NamedWindowConsumerStreamSpec> namedWindowConsumers = new ArrayList<>();
        List<StmtClassForgeableFactory> additionalForgeables = new ArrayList<>();

        // create subselect information
        SubSelectActivationDesc subSelectActivationDesc = SubSelectHelperActivations.createSubSelectActivation(filterSpecCompileds, namedWindowConsumers, base, services);
        Map<ExprSubselectNode, SubSelectActivationPlan> subselectActivation = subSelectActivationDesc.getSubselects();
        additionalForgeables.addAll(subSelectActivationDesc.getAdditionalForgeables());

        // obtain activator
        final StreamSpecCompiled streamSpec = base.getStatementSpec().getStreamSpecs()[0];
        StreamSelector optionalStreamSelector = null;
        OnTriggerActivatorDesc activatorResult;

        if (streamSpec instanceof FilterStreamSpecCompiled) {
            FilterStreamSpecCompiled filterStreamSpec = (FilterStreamSpecCompiled) streamSpec;
            activatorResult = activatorFilter(filterStreamSpec, services);
            filterSpecCompileds.add(filterStreamSpec.getFilterSpecCompiled());
        } else if (streamSpec instanceof PatternStreamSpecCompiled) {
            PatternStreamSpecCompiled patternStreamSpec = (PatternStreamSpecCompiled) streamSpec;
            List<EvalForgeNode> forges = patternStreamSpec.getRoot().collectFactories();
            for (EvalForgeNode forge : forges) {
                forge.collectSelfFilterAndSchedule(filterSpecCompileds, schedules);
            }
            activatorResult = activatorPattern(patternStreamSpec, services);
        } else if (streamSpec instanceof NamedWindowConsumerStreamSpec) {
            NamedWindowConsumerStreamSpec namedSpec = (NamedWindowConsumerStreamSpec) streamSpec;
            activatorResult = activatorNamedWindow(namedSpec, services);
            namedWindowConsumers.add(namedSpec);
        } else if (streamSpec instanceof TableQueryStreamSpec) {
            throw new ExprValidationException("Tables cannot be used in an on-action statement triggering stream");
        } else {
            throw new ExprValidationException("Unknown stream specification type: " + streamSpec);
        }

        String statementFieldsClassName = CodeGenerationIDGenerator.generateClassNameSimple(StatementFields.class, classPostfix);
        CodegenPackageScope packageScope = new CodegenPackageScope(packageName, statementFieldsClassName, services.isInstrumented());
        String aiFactoryProviderClassName = CodeGenerationIDGenerator.generateClassNameSimple(StatementAIFactoryProvider.class, classPostfix);

        // context-factory creation
        //
        // handle on-merge for table
        OnTriggerDesc onTriggerDesc = base.getStatementSpec().getRaw().getOnTriggerDesc();
        OnTriggerPlan onTriggerPlan;

        if (onTriggerDesc instanceof OnTriggerWindowDesc) {
            OnTriggerWindowDesc desc = (OnTriggerWindowDesc) onTriggerDesc;

            NamedWindowMetaData namedWindow = services.getNamedWindowCompileTimeResolver().resolve(desc.getWindowName());
            TableMetaData table = null;
            if (namedWindow == null) {
                table = services.getTableCompileTimeResolver().resolve(desc.getWindowName());
                if (table == null) {
                    throw new ExprValidationException("A named window or table '" + desc.getWindowName() + "' has not been declared");
                }
            }

            OnTriggerWindowPlan planDesc = new OnTriggerWindowPlan(desc, contextName, activatorResult, optionalStreamSelector, subselectActivation, streamSpec);
            onTriggerPlan = OnTriggerWindowUtil.handleContextFactoryOnTrigger(aiFactoryProviderClassName, packageScope,
                    classPostfix, namedWindow, table, planDesc, base, services);
        } else if (onTriggerDesc instanceof OnTriggerSetDesc) {
            // variable assignments
            OnTriggerSetDesc desc = (OnTriggerSetDesc) onTriggerDesc;
            OnTriggerSetPlan plan = OnTriggerSetUtil.handleSetVariable(aiFactoryProviderClassName, packageScope, classPostfix, activatorResult, streamSpec.getOptionalStreamName(), subselectActivation, desc, base, services);
            onTriggerPlan = new OnTriggerPlan(plan.getForgeable(), plan.getForgeables(), plan.getSelectSubscriberDescriptor(), plan.getAdditionalForgeables());
        } else {
            // split-stream use case
            OnTriggerSplitStreamDesc desc = (OnTriggerSplitStreamDesc) onTriggerDesc;
            onTriggerPlan = OnSplitStreamUtil.handleSplitStream(aiFactoryProviderClassName, packageScope,
                    classPostfix, desc, streamSpec, activatorResult, subselectActivation, base, services);
        }
        additionalForgeables.addAll(onTriggerPlan.getAdditionalForgeables());

        // build forge list
        List<StmtClassForgeable> forgeables = new ArrayList<>(2);
        for (StmtClassForgeableFactory additional : additionalForgeables) {
            forgeables.add(additional.make(packageScope, classPostfix));
        }

        forgeables.addAll(onTriggerPlan.getForgeables());
        forgeables.add(onTriggerPlan.getFactory());

        String statementProviderClassName = CodeGenerationIDGenerator.generateClassNameSimple(StatementProvider.class, classPostfix);
        StatementInformationalsCompileTime informationals = StatementInformationalsUtil.getInformationals(base, filterSpecCompileds, schedules, namedWindowConsumers, true, onTriggerPlan.getSubscriberDescriptor(), packageScope, services);
        forgeables.add(new StmtClassForgeableStmtProvider(aiFactoryProviderClassName, statementProviderClassName, informationals, packageScope));
        forgeables.add(new StmtClassForgeableStmtFields(statementFieldsClassName, packageScope, 2));

        return new StmtForgeMethodResult(forgeables, filterSpecCompileds, schedules, namedWindowConsumers, FilterSpecCompiled.makeExprNodeList(filterSpecCompileds, Collections.emptyList()));
    }

    private OnTriggerActivatorDesc activatorNamedWindow(NamedWindowConsumerStreamSpec namedSpec, StatementCompileTimeServices services) {
        NamedWindowMetaData namedWindow = namedSpec.getNamedWindow();
        String triggerEventTypeName = namedSpec.getNamedWindow().getEventType().getName();

        StreamTypeServiceImpl typesFilterValidation = new StreamTypeServiceImpl(namedWindow.getEventType(), namedSpec.getOptionalStreamName(), false);
        ExprNode filterSingle = ExprNodeUtilityMake.connectExpressionsByLogicalAndWhenNeeded(namedSpec.getFilterExpressions());
        QueryGraphForge filterQueryGraph = EPLValidationUtil.validateFilterGetQueryGraphSafe(filterSingle, typesFilterValidation, base.getStatementRawInfo(), services);
        ViewableActivatorNamedWindowForge activator = new ViewableActivatorNamedWindowForge(namedSpec, namedWindow, filterSingle, filterQueryGraph, false, namedSpec.getOptPropertyEvaluator());

        EventType activatorResultEventType = namedWindow.getEventType();
        if (namedSpec.getOptPropertyEvaluator() != null) {
            activatorResultEventType = namedSpec.getOptPropertyEvaluator().getFragmentEventType();
        }
        return new OnTriggerActivatorDesc(activator, triggerEventTypeName, activatorResultEventType);
    }

    private OnTriggerActivatorDesc activatorFilter(FilterStreamSpecCompiled filterStreamSpec, StatementCompileTimeServices services) {
        String triggerEventTypeName = filterStreamSpec.getFilterSpecCompiled().getFilterForEventTypeName();
        ViewableActivatorFilterForge activator = new ViewableActivatorFilterForge(filterStreamSpec.getFilterSpecCompiled(), false, 0, false, -1);
        EventType activatorResultEventType = filterStreamSpec.getFilterSpecCompiled().getResultEventType();
        return new OnTriggerActivatorDesc(activator, triggerEventTypeName, activatorResultEventType);
    }

    private OnTriggerActivatorDesc activatorPattern(PatternStreamSpecCompiled patternStreamSpec, StatementCompileTimeServices services) {
        String triggerEventTypeName = patternStreamSpec.getOptionalStreamName();
        MapEventType patternType = ViewableActivatorPatternForge.makeRegisterPatternType(base, 0, patternStreamSpec, services);
        PatternContext patternContext = new PatternContext(0, patternStreamSpec.getMatchedEventMapMeta(), false, -1, false);
        ViewableActivatorPatternForge activator = new ViewableActivatorPatternForge(patternType, patternStreamSpec, patternContext, false);
        return new OnTriggerActivatorDesc(activator, triggerEventTypeName, patternType);
    }
}

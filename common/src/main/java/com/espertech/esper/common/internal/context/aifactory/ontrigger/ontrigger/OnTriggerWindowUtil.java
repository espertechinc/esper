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
package com.espertech.esper.common.internal.context.aifactory.ontrigger.ontrigger;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.util.NameAccessModifier;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenPackageScope;
import com.espertech.esper.common.internal.bytecodemodel.core.CodeGenerationIDGenerator;
import com.espertech.esper.common.internal.compile.multikey.MultiKeyPlan;
import com.espertech.esper.common.internal.compile.multikey.MultiKeyPlanner;
import com.espertech.esper.common.internal.compile.stage1.spec.*;
import com.espertech.esper.common.internal.compile.stage2.StatementSpecCompiled;
import com.espertech.esper.common.internal.compile.stage3.*;
import com.espertech.esper.common.internal.context.activator.ViewableActivatorForge;
import com.espertech.esper.common.internal.context.aifactory.ontrigger.core.StmtClassForgeableAIFactoryProviderOnTrigger;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;
import com.espertech.esper.common.internal.epl.expression.subquery.ExprSubselectNode;
import com.espertech.esper.common.internal.epl.expression.table.ExprTableAccessNode;
import com.espertech.esper.common.internal.epl.join.hint.ExcludePlanHint;
import com.espertech.esper.common.internal.epl.join.hint.IndexHint;
import com.espertech.esper.common.internal.epl.lookupplansubord.*;
import com.espertech.esper.common.internal.epl.namedwindow.path.NamedWindowMetaData;
import com.espertech.esper.common.internal.epl.ontrigger.InfraOnMergeHelperForge;
import com.espertech.esper.common.internal.epl.resultset.core.ResultSetProcessorDesc;
import com.espertech.esper.common.internal.epl.resultset.core.ResultSetProcessorFactoryFactory;
import com.espertech.esper.common.internal.epl.resultset.core.ResultSetProcessorFactoryProvider;
import com.espertech.esper.common.internal.epl.resultset.core.ResultSetSpec;
import com.espertech.esper.common.internal.epl.streamtype.StreamTypeService;
import com.espertech.esper.common.internal.epl.streamtype.StreamTypeServiceImpl;
import com.espertech.esper.common.internal.epl.subselect.SubSelectFactoryForge;
import com.espertech.esper.common.internal.epl.table.compiletime.TableMetaData;
import com.espertech.esper.common.internal.epl.table.strategy.ExprTableEvalStrategyFactoryForge;
import com.espertech.esper.common.internal.epl.updatehelper.EventBeanUpdateHelperForge;
import com.espertech.esper.common.internal.epl.updatehelper.EventBeanUpdateHelperForgeFactory;
import com.espertech.esper.common.internal.event.core.EventTypeSPI;
import com.espertech.esper.common.internal.metrics.audit.AuditPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class OnTriggerWindowUtil {
    private static final Logger QUERY_PLAN_LOG = LoggerFactory.getLogger(AuditPath.QUERYPLAN_LOG);

    public static OnTriggerPlan handleContextFactoryOnTrigger(
            String className, CodegenPackageScope packageScope, String classPostfix,
            NamedWindowMetaData namedWindow, TableMetaData table, OnTriggerWindowPlan planDesc,
            StatementBaseInfo base, StatementCompileTimeServices services)
            throws ExprValidationException {

        // validate context
        String infraName = planDesc.getOnTriggerDesc().getWindowName();
        String infraTitle = (namedWindow != null ? "Named window" : "Table") + " '" + infraName + "'";
        String infraContextName = namedWindow != null ? namedWindow.getContextName() : table.getOptionalContextName();
        String infraModuleName = namedWindow != null ? namedWindow.getNamedWindowModuleName() : table.getTableModuleName();
        EventType infraEventType = namedWindow != null ? namedWindow.getEventType() : table.getInternalEventType();
        EventType resultEventType = namedWindow != null ? namedWindow.getEventType() : table.getPublicEventType();
        NameAccessModifier infraVisibility = namedWindow != null ? namedWindow.getEventType().getMetadata().getAccessModifier() : table.getTableVisibility();
        validateOnExpressionContext(planDesc.getContextName(), infraContextName, infraTitle);
        List<StmtClassForgeableFactory> additionalForgeables = new ArrayList<>(1);

        // validate expressions and plan subselects
        OnTriggerPlanValidationResult validationResult = OnTriggerPlanValidator.validateOnTriggerPlan(infraEventType, planDesc.getOnTriggerDesc(), planDesc.getStreamSpec(), planDesc.getActivatorResult(), planDesc.getSubselectActivation(), base, services);
        additionalForgeables.addAll(validationResult.getAdditionalForgeables());

        ExprNode validatedJoin = validationResult.getValidatedJoin();
        EventType activatorResultEventType = planDesc.getActivatorResult().getActivatorResultEventType();

        IndexHintPair pair = IndexHintPair.getIndexHintPair(planDesc.getOnTriggerDesc(), base.getStatementSpec().getStreamSpecs()[0].getOptionalStreamName(), base.getStatementRawInfo(), services);
        IndexHint indexHint = pair.getIndexHint();
        ExcludePlanHint excludePlanHint = pair.getExcludePlanHint();

        boolean enabledSubqueryIndexShare = namedWindow != null && namedWindow.isEnableIndexShare();
        boolean isVirtualWindow = namedWindow != null && namedWindow.isVirtualDataWindow();
        EventTableIndexMetadata indexMetadata = namedWindow != null ? namedWindow.getIndexMetadata() : table.getIndexMetadata();
        Set<String> optionalUniqueKeySet = namedWindow != null ? namedWindow.getUniquenessAsSet() : table.getUniquenessAsSet();

        // query plan
        boolean onlyUseExistingIndexes = table != null;
        SubordinateWMatchExprQueryPlanResult planResult = SubordinateQueryPlanner.planOnExpression(
            validatedJoin, activatorResultEventType, indexHint, enabledSubqueryIndexShare, -1, excludePlanHint, isVirtualWindow,
            indexMetadata, infraEventType, optionalUniqueKeySet, onlyUseExistingIndexes, base.getStatementRawInfo(), services);
        SubordinateWMatchExprQueryPlanForge queryPlan = planResult.getForge();
        additionalForgeables.addAll(planResult.getAdditionalForgeables());

        // indicate index dependencies
        if (queryPlan.getIndexes() != null && infraVisibility == NameAccessModifier.PUBLIC) {
            for (SubordinateQueryIndexDescForge index : queryPlan.getIndexes()) {
                services.getModuleDependenciesCompileTime().addPathIndex(namedWindow != null, infraName, infraModuleName, index.getIndexName(), index.getIndexModuleName(), services.getNamedWindowCompileTimeRegistry(), services.getTableCompileTimeRegistry());
            }
        }

        OnTriggerType onTriggerType = planDesc.getOnTriggerDesc().getOnTriggerType();
        ViewableActivatorForge activator = planDesc.getActivatorResult().getActivator();
        Map<ExprSubselectNode, SubSelectFactoryForge> subselectForges = validationResult.getSubselectForges();
        Map<ExprTableAccessNode, ExprTableEvalStrategyFactoryForge> tableAccessForges = validationResult.getTableAccessForges();

        List<StmtClassForgeable> forgeables = new ArrayList<>(2);
        StatementAgentInstanceFactoryOnTriggerInfraBaseForge forge;
        String classNameRSP = CodeGenerationIDGenerator.generateClassNameSimple(ResultSetProcessorFactoryProvider.class, classPostfix);
        ResultSetProcessorDesc resultSetProcessor;

        if (onTriggerType == OnTriggerType.ON_SELECT) {
            resultSetProcessor = validationResult.getResultSetProcessorPrototype();
            EventType outputEventType = resultSetProcessor.getResultEventType();

            boolean insertInto = false;
            TableMetaData optionalInsertIntoTable = null;
            InsertIntoDesc insertIntoDesc = base.getStatementSpec().getRaw().getInsertIntoDesc();
            boolean addToFront = false;
            if (insertIntoDesc != null) {
                insertInto = true;
                optionalInsertIntoTable = services.getTableCompileTimeResolver().resolve(insertIntoDesc.getEventTypeName());
                NamedWindowMetaData optionalInsertIntoNamedWindow = services.getNamedWindowCompileTimeResolver().resolve(insertIntoDesc.getEventTypeName());
                addToFront = optionalInsertIntoNamedWindow != null || optionalInsertIntoTable != null;
            }

            boolean selectAndDelete = planDesc.getOnTriggerDesc().isDeleteAndSelect();
            boolean distinct = base.getStatementSpec().getSelectClauseCompiled().isDistinct();
            MultiKeyPlan distinctMultiKeyPlan = MultiKeyPlanner.planMultiKeyDistinct(distinct, outputEventType, base.getStatementRawInfo(), services.getSerdeResolver());
            additionalForgeables.addAll(distinctMultiKeyPlan.getMultiKeyForgeables());
            forge = new StatementAgentInstanceFactoryOnTriggerInfraSelectForge(activator, outputEventType, subselectForges, tableAccessForges, namedWindow, table, queryPlan, classNameRSP, insertInto, addToFront, optionalInsertIntoTable, selectAndDelete, distinct, distinctMultiKeyPlan.getClassRef());
        } else {
            StatementSpecCompiled defaultSelectAllSpec = new StatementSpecCompiled();
            defaultSelectAllSpec.getSelectClauseCompiled().setSelectExprList(new SelectClauseElementWildcard());
            defaultSelectAllSpec.getRaw().setSelectStreamDirEnum(SelectClauseStreamSelectorEnum.RSTREAM_ISTREAM_BOTH);
            StreamTypeService typeService = new StreamTypeServiceImpl(new EventType[]{resultEventType}, new String[]{infraName}, new boolean[]{false}, false, false);
            resultSetProcessor = ResultSetProcessorFactoryFactory.getProcessorPrototype(new ResultSetSpec(defaultSelectAllSpec),
                    typeService, null, new boolean[1], false, base.getContextPropertyRegistry(), false, false, base.getStatementRawInfo(), services);

            if (onTriggerType == OnTriggerType.ON_DELETE) {
                forge = new StatementAgentInstanceFactoryOnTriggerInfraDeleteForge(activator, resultEventType, subselectForges, tableAccessForges, classNameRSP, namedWindow, table, queryPlan);
            } else if (onTriggerType == OnTriggerType.ON_UPDATE) {
                OnTriggerWindowUpdateDesc updateDesc = (OnTriggerWindowUpdateDesc) planDesc.getOnTriggerDesc();
                EventBeanUpdateHelperForge updateHelper = EventBeanUpdateHelperForgeFactory.make(infraName, (EventTypeSPI) infraEventType, updateDesc.getAssignments(),
                        validationResult.getZeroStreamAliasName(), activatorResultEventType, namedWindow != null, base.getStatementName(), services.getEventTypeAvroHandler());
                forge = new StatementAgentInstanceFactoryOnTriggerInfraUpdateForge(activator, resultEventType, subselectForges, tableAccessForges, classNameRSP, namedWindow, table, queryPlan, updateHelper);
            } else if (onTriggerType == OnTriggerType.ON_MERGE) {
                OnTriggerMergeDesc onMergeTriggerDesc = (OnTriggerMergeDesc) planDesc.getOnTriggerDesc();
                InfraOnMergeHelperForge onMergeHelper = new InfraOnMergeHelperForge(onMergeTriggerDesc, activatorResultEventType, planDesc.getStreamSpec().getOptionalStreamName(), infraName, (EventTypeSPI) infraEventType, base.getStatementRawInfo(), services, table);
                forge = new StatementAgentInstanceFactoryOnTriggerInfraMergeForge(activator, resultEventType, subselectForges, tableAccessForges, classNameRSP, namedWindow, table, queryPlan, onMergeHelper);
            } else {
                throw new IllegalStateException("Unrecognized trigger type " + onTriggerType);
            }
        }
        forgeables.add(new StmtClassForgeableRSPFactoryProvider(classNameRSP, resultSetProcessor, packageScope, base.getStatementRawInfo()));

        boolean queryPlanLogging = services.getConfiguration().getCommon().getLogging().isEnableQueryPlan();
        SubordinateQueryPlannerUtil.queryPlanLogOnExpr(queryPlanLogging, QUERY_PLAN_LOG,
                queryPlan, base.getStatementSpec().getAnnotations(), services.getClasspathImportServiceCompileTime());

        StmtClassForgeableAIFactoryProviderOnTrigger onTrigger = new StmtClassForgeableAIFactoryProviderOnTrigger(className, packageScope, forge);
        return new OnTriggerPlan(onTrigger, forgeables, resultSetProcessor.getSelectSubscriberDescriptor(), additionalForgeables);
    }

    protected static void validateOnExpressionContext(String onExprContextName, String desiredContextName, String title)
            throws ExprValidationException {
        if (onExprContextName == null) {
            if (desiredContextName != null) {
                throw new ExprValidationException("Cannot create on-trigger expression: " + title + " was declared with context '" + desiredContextName + "', please declare the same context name");
            }
            return;
        }
        if (!onExprContextName.equals(desiredContextName)) {
            String text = desiredContextName == null ?
                "without a context" :
                "with context '" + desiredContextName + "', please use the same context instead";
            throw new ExprValidationException("Cannot create on-trigger expression: " + title + " was declared " + text);
        }
    }
}

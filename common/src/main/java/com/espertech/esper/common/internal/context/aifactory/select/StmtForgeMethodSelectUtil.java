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
package com.espertech.esper.common.internal.context.aifactory.select;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.annotation.HookType;
import com.espertech.esper.common.client.annotation.IterableUnbound;
import com.espertech.esper.common.client.hook.type.SQLColumnTypeConversion;
import com.espertech.esper.common.client.hook.type.SQLOutputRowConversion;
import com.espertech.esper.common.client.util.NameAccessModifier;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenPackageScope;
import com.espertech.esper.common.internal.bytecodemodel.core.CodeGenerationIDGenerator;
import com.espertech.esper.common.internal.compile.stage1.spec.*;
import com.espertech.esper.common.internal.compile.stage2.FilterSpecCompiled;
import com.espertech.esper.common.internal.compile.stage2.FilterStreamSpecCompiled;
import com.espertech.esper.common.internal.compile.stage2.StatementSpecCompiled;
import com.espertech.esper.common.internal.compile.stage3.*;
import com.espertech.esper.common.internal.context.activator.*;
import com.espertech.esper.common.internal.context.module.StatementAIFactoryProvider;
import com.espertech.esper.common.internal.context.module.StatementFields;
import com.espertech.esper.common.internal.context.module.StatementInformationalsCompileTime;
import com.espertech.esper.common.internal.context.module.StatementProvider;
import com.espertech.esper.common.internal.epl.annotation.AnnotationUtil;
import com.espertech.esper.common.internal.epl.expression.core.*;
import com.espertech.esper.common.internal.epl.expression.subquery.ExprSubselectNode;
import com.espertech.esper.common.internal.epl.expression.table.ExprTableAccessNode;
import com.espertech.esper.common.internal.epl.historical.common.HistoricalEventViewableForge;
import com.espertech.esper.common.internal.epl.historical.common.HistoricalViewableDesc;
import com.espertech.esper.common.internal.epl.historical.database.core.HistoricalEventViewableDatabaseForge;
import com.espertech.esper.common.internal.epl.historical.database.core.HistoricalEventViewableDatabaseForgeFactory;
import com.espertech.esper.common.internal.epl.historical.method.core.HistoricalEventViewableMethodForge;
import com.espertech.esper.common.internal.epl.historical.method.core.HistoricalEventViewableMethodForgeFactory;
import com.espertech.esper.common.internal.epl.join.analyze.OuterJoinAnalyzer;
import com.espertech.esper.common.internal.epl.join.base.JoinSetComposerPrototypeForge;
import com.espertech.esper.common.internal.epl.join.base.JoinSetComposerPrototypeForgeFactory;
import com.espertech.esper.common.internal.epl.join.querygraph.QueryGraphForge;
import com.espertech.esper.common.internal.epl.join.queryplan.QueryPlanForge;
import com.espertech.esper.common.internal.epl.join.queryplan.QueryPlanNodeForge;
import com.espertech.esper.common.internal.epl.join.queryplan.TableLookupIndexReqKey;
import com.espertech.esper.common.internal.epl.namedwindow.path.NamedWindowMetaData;
import com.espertech.esper.common.internal.epl.output.core.OutputProcessViewFactoryForge;
import com.espertech.esper.common.internal.epl.output.core.OutputProcessViewFactoryProvider;
import com.espertech.esper.common.internal.epl.output.core.OutputProcessViewForgeFactory;
import com.espertech.esper.common.internal.epl.pattern.core.EvalForgeNode;
import com.espertech.esper.common.internal.epl.pattern.core.PatternContext;
import com.espertech.esper.common.internal.epl.resultset.core.ResultSetProcessorDesc;
import com.espertech.esper.common.internal.epl.resultset.core.ResultSetProcessorFactoryFactory;
import com.espertech.esper.common.internal.epl.resultset.core.ResultSetProcessorFactoryProvider;
import com.espertech.esper.common.internal.epl.resultset.core.ResultSetSpec;
import com.espertech.esper.common.internal.epl.rowrecog.core.RowRecogDescForge;
import com.espertech.esper.common.internal.epl.rowrecog.core.RowRecogNFAViewFactoryForge;
import com.espertech.esper.common.internal.epl.rowrecog.core.RowRecogNFAViewPlanUtil;
import com.espertech.esper.common.internal.epl.streamtype.StreamTypeService;
import com.espertech.esper.common.internal.epl.streamtype.StreamTypeServiceImpl;
import com.espertech.esper.common.internal.epl.subselect.SubSelectActivationPlan;
import com.espertech.esper.common.internal.epl.subselect.SubSelectFactoryForge;
import com.espertech.esper.common.internal.epl.subselect.SubSelectHelperActivations;
import com.espertech.esper.common.internal.epl.subselect.SubSelectHelperForgePlanner;
import com.espertech.esper.common.internal.epl.table.compiletime.TableMetaData;
import com.espertech.esper.common.internal.epl.table.strategy.ExprTableEvalHelperPlan;
import com.espertech.esper.common.internal.epl.table.strategy.ExprTableEvalStrategyFactoryForge;
import com.espertech.esper.common.internal.epl.util.EPLValidationUtil;
import com.espertech.esper.common.internal.epl.util.ViewResourceVerifyHelper;
import com.espertech.esper.common.internal.event.map.MapEventType;
import com.espertech.esper.common.internal.schedule.ScheduleHandleCallbackProvider;
import com.espertech.esper.common.internal.settings.ClasspathImportUtil;
import com.espertech.esper.common.internal.statement.helper.EPStatementStartMethodHelperValidate;
import com.espertech.esper.common.internal.view.access.ViewResourceDelegateDesc;
import com.espertech.esper.common.internal.view.access.ViewResourceDelegateExpr;
import com.espertech.esper.common.internal.view.core.ViewFactoryForge;
import com.espertech.esper.common.internal.view.core.ViewFactoryForgeArgs;
import com.espertech.esper.common.internal.view.core.ViewFactoryForgeUtil;
import com.espertech.esper.common.internal.view.core.ViewForgeVisitorSchedulesCollector;
import com.espertech.esper.common.internal.view.prior.PriorEventViewForge;

import java.util.*;

import static com.espertech.esper.common.internal.context.aifactory.select.StatementForgeMethodSelectUtil.*;

public class StmtForgeMethodSelectUtil {

    public static StmtForgeMethodSelectResult make(boolean dataflowOperator, String packageName, String classPostfix, StatementBaseInfo base, StatementCompileTimeServices services) throws ExprValidationException {
        List<FilterSpecCompiled> filterSpecCompileds = new ArrayList<>();
        List<ScheduleHandleCallbackProvider> scheduleHandleCallbackProviders = new ArrayList<>();
        List<NamedWindowConsumerStreamSpec> namedWindowConsumers = new ArrayList<>();
        StatementSpecCompiled statementSpec = base.getStatementSpec();

        String[] streamNames = determineStreamNames(statementSpec.getStreamSpecs());
        int numStreams = streamNames.length;
        if (numStreams == 0) {
            throw new ExprValidationException("The from-clause is required but has not been specified");
        }

        // first we create streams for subselects, if there are any
        Map<ExprSubselectNode, SubSelectActivationPlan> subselectActivation = SubSelectHelperActivations.createSubSelectActivation(filterSpecCompileds, namedWindowConsumers, base, services);

        // verify for joins that required views are present
        StreamJoinAnalysisResultCompileTime joinAnalysisResult = verifyJoinViews(statementSpec, services.getNamedWindowCompileTimeResolver());

        EventType[] streamEventTypes = new EventType[statementSpec.getStreamSpecs().length];
        String[] eventTypeNames = new String[numStreams];
        boolean[] isNamedWindow = new boolean[numStreams];
        ViewableActivatorForge[] viewableActivatorForges = new ViewableActivatorForge[numStreams];
        List<ViewFactoryForge>[] viewForges = new List[numStreams];
        HistoricalEventViewableForge[] historicalEventViewables = new HistoricalEventViewableForge[numStreams];

        for (int stream = 0; stream < numStreams; stream++) {
            StreamSpecCompiled streamSpec = statementSpec.getStreamSpecs()[stream];
            boolean isCanIterateUnbound = streamSpec.getViewSpecs().length == 0 &&
                    (services.getConfiguration().getCompiler().getViewResources().isIterableUnbound() ||
                            AnnotationUtil.findAnnotation(statementSpec.getAnnotations(), IterableUnbound.class) != null);
            ViewFactoryForgeArgs args = new ViewFactoryForgeArgs(stream, false, -1, streamSpec.getOptions(), null, base.getStatementRawInfo(), services);

            if (dataflowOperator) {
                DataFlowActivationResult dfResult = handleDataflowActivation(args, streamSpec);
                streamEventTypes[stream] = dfResult.getStreamEventType();
                eventTypeNames[stream] = dfResult.getEventTypeName();
                viewableActivatorForges[stream] = dfResult.getViewableActivatorForge();
                viewForges[stream] = dfResult.getViewForges();
            } else if (streamSpec instanceof FilterStreamSpecCompiled) {
                FilterStreamSpecCompiled filterStreamSpec = (FilterStreamSpecCompiled) statementSpec.getStreamSpecs()[stream];
                FilterSpecCompiled filterSpecCompiled = filterStreamSpec.getFilterSpecCompiled();
                streamEventTypes[stream] = filterSpecCompiled.getResultEventType();
                eventTypeNames[stream] = filterStreamSpec.getFilterSpecCompiled().getFilterForEventTypeName();

                viewableActivatorForges[stream] = new ViewableActivatorFilterForge(filterSpecCompiled, isCanIterateUnbound, stream, false, -1);
                viewForges[stream] = ViewFactoryForgeUtil.createForges(streamSpec.getViewSpecs(), args, streamEventTypes[stream]);
                filterSpecCompileds.add(filterSpecCompiled);
            } else if (streamSpec instanceof PatternStreamSpecCompiled) {
                PatternStreamSpecCompiled patternStreamSpec = (PatternStreamSpecCompiled) streamSpec;
                List<EvalForgeNode> forges = patternStreamSpec.getRoot().collectFactories();
                for (EvalForgeNode forge : forges) {
                    forge.collectSelfFilterAndSchedule(filterSpecCompileds, scheduleHandleCallbackProviders);
                }

                MapEventType patternType = ViewableActivatorPatternForge.makeRegisterPatternType(base, stream, patternStreamSpec, services);
                PatternContext patternContext = new PatternContext(0, patternStreamSpec.getMatchedEventMapMeta(), false, -1, false);
                viewableActivatorForges[stream] = new ViewableActivatorPatternForge(patternType, patternStreamSpec, patternContext, isCanIterateUnbound);
                streamEventTypes[stream] = patternType;
                viewForges[stream] = ViewFactoryForgeUtil.createForges(streamSpec.getViewSpecs(), args, patternType);
            } else if (streamSpec instanceof NamedWindowConsumerStreamSpec) {
                NamedWindowConsumerStreamSpec namedSpec = (NamedWindowConsumerStreamSpec) streamSpec;
                NamedWindowMetaData namedWindow = services.getNamedWindowCompileTimeResolver().resolve(namedSpec.getNamedWindow().getEventType().getName());
                EventType namedWindowType = namedWindow.getEventType();
                if (namedSpec.getOptPropertyEvaluator() != null) {
                    namedWindowType = namedSpec.getOptPropertyEvaluator().getFragmentEventType();
                }

                StreamTypeServiceImpl typesFilterValidation = new StreamTypeServiceImpl(namedWindowType, namedSpec.getOptionalStreamName(), false);
                ExprNode filterSingle = ExprNodeUtilityMake.connectExpressionsByLogicalAndWhenNeeded(namedSpec.getFilterExpressions());
                QueryGraphForge filterQueryGraph = EPLValidationUtil.validateFilterGetQueryGraphSafe(filterSingle, typesFilterValidation, base.getStatementRawInfo(), services);

                namedWindowConsumers.add(namedSpec);
                viewableActivatorForges[stream] = new ViewableActivatorNamedWindowForge(namedSpec, namedWindow, filterSingle, filterQueryGraph, true, namedSpec.getOptPropertyEvaluator());
                streamEventTypes[stream] = namedWindowType;
                viewForges[stream] = Collections.emptyList();
                joinAnalysisResult.setNamedWindowsPerStream(stream, namedWindow);
                eventTypeNames[stream] = namedSpec.getNamedWindow().getEventType().getName();
                isNamedWindow[stream] = true;

                // Consumers to named windows cannot declare a data window view onto the named window to avoid duplicate remove streams
                viewForges[stream] = ViewFactoryForgeUtil.createForges(streamSpec.getViewSpecs(), args, namedWindowType);
                EPStatementStartMethodHelperValidate.validateNoDataWindowOnNamedWindow(viewForges[stream]);
            } else if (streamSpec instanceof TableQueryStreamSpec) {
                validateNoViews(streamSpec, "Table data");
                TableQueryStreamSpec tableStreamSpec = (TableQueryStreamSpec) streamSpec;
                if (numStreams > 1 && tableStreamSpec.getFilterExpressions().size() > 0) {
                    throw new ExprValidationException("Joins with tables do not allow table filter expressions, please add table filters to the where-clause instead");
                }
                TableMetaData table = tableStreamSpec.getTable();
                EPLValidationUtil.validateContextName(true, table.getTableName(), table.getOptionalContextName(), statementSpec.getRaw().getOptionalContextName(), false);
                ExprNode filter = ExprNodeUtilityMake.connectExpressionsByLogicalAndWhenNeeded(tableStreamSpec.getFilterExpressions());
                viewableActivatorForges[stream] = new ViewableActivatorTableForge(table, filter);
                viewForges[stream] = Collections.emptyList();
                eventTypeNames[stream] = tableStreamSpec.getTable().getTableName();
                streamEventTypes[stream] = tableStreamSpec.getTable().getInternalEventType();
                joinAnalysisResult.setTablesForStream(stream, table);

                if (tableStreamSpec.getOptions().isUnidirectional()) {
                    throw new ExprValidationException("Tables cannot be marked as unidirectional");
                }
                if (tableStreamSpec.getOptions().isRetainIntersection() || tableStreamSpec.getOptions().isRetainUnion()) {
                    throw new ExprValidationException("Tables cannot be marked with retain");
                }
            } else if (streamSpec instanceof DBStatementStreamSpec) {
                validateNoViews(streamSpec, "Historical data");
                DBStatementStreamSpec sqlStreamSpec = (DBStatementStreamSpec) streamSpec;
                SQLColumnTypeConversion typeConversionHook = (SQLColumnTypeConversion) ClasspathImportUtil.getAnnotationHook(statementSpec.getAnnotations(), HookType.SQLCOL, SQLColumnTypeConversion.class, services.getClasspathImportServiceCompileTime());
                SQLOutputRowConversion outputRowConversionHook = (SQLOutputRowConversion) ClasspathImportUtil.getAnnotationHook(statementSpec.getAnnotations(), HookType.SQLROW, SQLOutputRowConversion.class, services.getClasspathImportServiceCompileTime());
                HistoricalEventViewableDatabaseForge viewable = HistoricalEventViewableDatabaseForgeFactory.createDBStatementView(stream, sqlStreamSpec, typeConversionHook, outputRowConversionHook, base, services);
                streamEventTypes[stream] = viewable.getEventType();
                viewForges[stream] = Collections.emptyList();
                viewableActivatorForges[stream] = new ViewableActivatorHistoricalForge(viewable);
                historicalEventViewables[stream] = viewable;
            } else if (streamSpec instanceof MethodStreamSpec) {
                validateNoViews(streamSpec, "Method data");
                MethodStreamSpec methodStreamSpec = (MethodStreamSpec) streamSpec;
                HistoricalEventViewableMethodForge viewable = HistoricalEventViewableMethodForgeFactory.createMethodStatementView(stream, methodStreamSpec, base, services);
                historicalEventViewables[stream] = viewable;
                streamEventTypes[stream] = viewable.getEventType();
                viewForges[stream] = Collections.emptyList();
                viewableActivatorForges[stream] = new ViewableActivatorHistoricalForge(viewable);
                historicalEventViewables[stream] = viewable;
            } else {
                throw new IllegalStateException("Unrecognized stream " + streamSpec);
            }
        }

        // handle match-recognize pattern
        if (statementSpec.getRaw().getMatchRecognizeSpec() != null) {
            if (numStreams > 1) {
                throw new ExprValidationException("Joins are not allowed when using match-recognize");
            }
            if (joinAnalysisResult.getTablesPerStream()[0] != null) {
                throw new ExprValidationException("Tables cannot be used with match-recognize");
            }
            boolean isUnbound = (viewForges[0].isEmpty()) && (!(statementSpec.getStreamSpecs()[0] instanceof NamedWindowConsumerStreamSpec));
            EventType eventType = viewForges[0].isEmpty() ? streamEventTypes[0] : viewForges[0].get(viewForges[0].size() - 1).getEventType();
            RowRecogDescForge desc = RowRecogNFAViewPlanUtil.validateAndPlan(eventType, isUnbound, base, services);
            RowRecogNFAViewFactoryForge forge = new RowRecogNFAViewFactoryForge(desc);
            scheduleHandleCallbackProviders.add(forge);
            viewForges[0].add(forge);
        }

        // Obtain event types from view factory chains
        for (int i = 0; i < viewForges.length; i++) {
            streamEventTypes[i] = viewForges[i].isEmpty() ? streamEventTypes[i] : viewForges[i].get(viewForges[i].size() - 1).getEventType();
        }

        // add unique-information to join analysis
        joinAnalysisResult.addUniquenessInfo(viewForges, statementSpec.getAnnotations());

        // plan sub-selects
        Map<ExprSubselectNode, SubSelectFactoryForge> subselectForges = SubSelectHelperForgePlanner.planSubSelect(base, subselectActivation, streamNames, streamEventTypes, eventTypeNames, services);
        determineViewSchedules(subselectForges, scheduleHandleCallbackProviders);

        // determine view schedules
        ViewResourceDelegateExpr viewResourceDelegateExpr = new ViewResourceDelegateExpr();
        ViewFactoryForgeUtil.determineViewSchedules(viewForges, scheduleHandleCallbackProviders);

        boolean[] hasIStreamOnly = getHasIStreamOnly(isNamedWindow, viewForges);
        boolean optionalStreamsIfAny = OuterJoinAnalyzer.optionalStreamsIfAny(statementSpec.getRaw().getOuterJoinDescList());
        StreamTypeService typeService = new StreamTypeServiceImpl(streamEventTypes, streamNames, hasIStreamOnly, false, optionalStreamsIfAny);

        // Validate views that require validation, specifically streams that don't have
        // sub-views such as DB SQL joins
        HistoricalViewableDesc historicalViewableDesc = new HistoricalViewableDesc(numStreams);
        for (int stream = 0; stream < historicalEventViewables.length; stream++) {
            HistoricalEventViewableForge historicalEventViewable = historicalEventViewables[stream];
            if (historicalEventViewable == null) {
                continue;
            }
            scheduleHandleCallbackProviders.add(historicalEventViewable);
            historicalEventViewable.validate(typeService, base, services);
            historicalViewableDesc.setHistorical(stream, historicalEventViewable.getRequiredStreams());
            if (historicalEventViewable.getRequiredStreams().contains(stream)) {
                throw new ExprValidationException("Parameters for historical stream " + stream + " indicate that the stream is subordinate to itself as stream parameters originate in the same stream");
            }
        }

        // Validate where-clause filter tree, outer join clause and output limit expression
        ExprNode whereClauseValidated = EPStatementStartMethodHelperValidate.validateNodes(statementSpec.getRaw(), typeService, viewResourceDelegateExpr, base.getStatementRawInfo(), services);
        ExprForge whereClauseForge = whereClauseValidated == null ? null : whereClauseValidated.getForge();

        // Obtain result set processor
        ResultSetProcessorDesc resultSetProcessorDesc = ResultSetProcessorFactoryFactory.getProcessorPrototype(new ResultSetSpec(statementSpec), typeService, viewResourceDelegateExpr, joinAnalysisResult.getUnidirectionalInd(), true, base.getContextPropertyRegistry(), false, false, base.getStatementRawInfo(), services);

        // Handle 'prior' function nodes in terms of view requirements
        ViewResourceDelegateDesc[] viewResourceDelegateDesc = ViewResourceVerifyHelper.verifyPreviousAndPriorRequirements(viewForges, viewResourceDelegateExpr);
        boolean hasPrior = ViewResourceDelegateDesc.hasPrior(viewResourceDelegateDesc);
        if (hasPrior) {
            for (int stream = 0; stream < numStreams; stream++) {
                if (!viewResourceDelegateDesc[stream].getPriorRequests().isEmpty()) {
                    viewForges[stream].add(new PriorEventViewForge(viewForges[stream].isEmpty(), streamEventTypes[stream]));
                }
            }
        }

        OutputProcessViewFactoryForge outputProcessViewFactoryForge = OutputProcessViewForgeFactory.make(typeService.getEventTypes(), resultSetProcessorDesc.getResultEventType(), resultSetProcessorDesc.getResultSetProcessorType(), statementSpec, base.getStatementRawInfo(), services);
        outputProcessViewFactoryForge.collectSchedules(scheduleHandleCallbackProviders);

        JoinSetComposerPrototypeForge joinForge = null;
        if (numStreams > 1) {
            boolean hasAggregations = !resultSetProcessorDesc.getAggregationServiceForgeDesc().getExpressions().isEmpty();
            joinForge = JoinSetComposerPrototypeForgeFactory.makeComposerPrototype(statementSpec, joinAnalysisResult, typeService, historicalViewableDesc, false, hasAggregations, base.getStatementRawInfo(), services);
            handleIndexDependencies(joinForge.getOptionalQueryPlan(), services);
        }

        // plan table access
        Map<ExprTableAccessNode, ExprTableEvalStrategyFactoryForge> tableAccessForges = ExprTableEvalHelperPlan.planTableAccess(base.getStatementSpec().getTableAccessNodes());
        validateTableAccessUse(statementSpec.getRaw().getIntoTableSpec(), statementSpec.getRaw().getTableExpressions());
        if (joinAnalysisResult.isUnidirectional() && statementSpec.getRaw().getIntoTableSpec() != null) {
            throw new ExprValidationException("Into-table does not allow unidirectional joins");
        }

        boolean orderByWithoutOutputLimit = statementSpec.getRaw().getOrderByList() != null && !statementSpec.getRaw().getOrderByList().isEmpty() && (statementSpec.getRaw().getOutputLimitSpec() == null);

        String statementAIFactoryProviderClassName = CodeGenerationIDGenerator.generateClassNameSimple(StatementAIFactoryProvider.class, classPostfix);
        String resultSetProcessorProviderClassName = CodeGenerationIDGenerator.generateClassNameSimple(ResultSetProcessorFactoryProvider.class, classPostfix);
        String outputProcessViewProviderClassName = CodeGenerationIDGenerator.generateClassNameSimple(OutputProcessViewFactoryProvider.class, classPostfix);
        String statementProviderClassName = CodeGenerationIDGenerator.generateClassNameSimple(StatementProvider.class, classPostfix);
        String statementFieldsClassName = CodeGenerationIDGenerator.generateClassNameSimple(StatementFields.class, classPostfix);

        StatementAgentInstanceFactorySelectForge forge = new StatementAgentInstanceFactorySelectForge(typeService.getStreamNames(), viewableActivatorForges, resultSetProcessorProviderClassName, viewForges, viewResourceDelegateDesc, whereClauseForge, joinForge, outputProcessViewProviderClassName, subselectForges, tableAccessForges, orderByWithoutOutputLimit, joinAnalysisResult.isUnidirectional());

        CodegenPackageScope packageScope = new CodegenPackageScope(packageName, statementFieldsClassName, services.isInstrumented());
        List<StmtClassForgable> forgables = new ArrayList<>();

        forgables.add(new StmtClassForgableRSPFactoryProvider(resultSetProcessorProviderClassName, resultSetProcessorDesc, packageScope, base.getStatementRawInfo()));
        forgables.add(new StmtClassForgableOPVFactoryProvider(outputProcessViewProviderClassName, outputProcessViewFactoryForge, packageScope, numStreams, base.getStatementRawInfo()));
        forgables.add(new StmtClassForgableAIFactoryProviderSelect(statementAIFactoryProviderClassName, packageScope, forge));
        forgables.add(new StmtClassForgableStmtFields(packageScope.getFieldsClassNameOptional(), packageScope, numStreams));

        if (!dataflowOperator) {
            StatementInformationalsCompileTime informationals = StatementInformationalsUtil.getInformationals(base, filterSpecCompileds, scheduleHandleCallbackProviders, namedWindowConsumers, true, resultSetProcessorDesc.getSelectSubscriberDescriptor(), packageScope, services);
            forgables.add(new StmtClassForgableStmtProvider(statementAIFactoryProviderClassName, statementProviderClassName, informationals, packageScope));
        }

        StmtForgeMethodResult forgableResult = new StmtForgeMethodResult(forgables, filterSpecCompileds, scheduleHandleCallbackProviders, namedWindowConsumers, FilterSpecCompiled.makeExprNodeList(filterSpecCompileds, Collections.emptyList()));
        return new StmtForgeMethodSelectResult(forgableResult, resultSetProcessorDesc.getResultEventType(), numStreams);
    }

    private static DataFlowActivationResult handleDataflowActivation(ViewFactoryForgeArgs args, StreamSpecCompiled streamSpec)
            throws ExprValidationException {
        if (!(streamSpec instanceof FilterStreamSpecCompiled)) {
            throw new ExprValidationException("Dataflow operator only allows filters for event types and does not allow tables, named windows or patterns");
        }
        FilterStreamSpecCompiled filterStreamSpec = (FilterStreamSpecCompiled) streamSpec;
        FilterSpecCompiled filterSpecCompiled = filterStreamSpec.getFilterSpecCompiled();
        EventType eventType = filterSpecCompiled.getResultEventType();
        String typeName = filterStreamSpec.getFilterSpecCompiled().getFilterForEventTypeName();
        List<ViewFactoryForge> views = ViewFactoryForgeUtil.createForges(streamSpec.getViewSpecs(), args, eventType);
        ViewableActivatorDataFlowForge viewableActivator = new ViewableActivatorDataFlowForge(eventType);
        return new DataFlowActivationResult(eventType, typeName, viewableActivator, views);
    }

    private static void determineViewSchedules(Map<ExprSubselectNode, SubSelectFactoryForge> subselects, List<ScheduleHandleCallbackProvider> scheduleHandleCallbackProviders) {
        ViewForgeVisitorSchedulesCollector collector = new ViewForgeVisitorSchedulesCollector(scheduleHandleCallbackProviders);
        for (Map.Entry<ExprSubselectNode, SubSelectFactoryForge> subselect : subselects.entrySet()) {
            for (ViewFactoryForge forge : subselect.getValue().getViewForges()) {
                forge.accept(collector);
            }
        }
    }

    private static void validateNoViews(StreamSpecCompiled streamSpec, String conceptName)
            throws ExprValidationException {
        if (streamSpec.getViewSpecs().length > 0) {
            throw new ExprValidationException(conceptName + " joins do not allow views onto the data, view '"
                    + streamSpec.getViewSpecs()[0].getObjectName() + "' is not valid in this context");
        }
    }

    private static void validateTableAccessUse(IntoTableSpec intoTableSpec, Set<ExprTableAccessNode> tableNodes)
            throws ExprValidationException {
        if (intoTableSpec != null && tableNodes != null && tableNodes.size() > 0) {
            for (ExprTableAccessNode node : tableNodes) {
                if (node.getTableName().equals(intoTableSpec.getName())) {
                    throw new ExprValidationException("Invalid use of table '" + intoTableSpec.getName() + "', aggregate-into requires write-only, the expression '" +
                            ExprNodeUtilityPrint.toExpressionStringMinPrecedenceSafe(node) + "' is not allowed");
                }
            }
        }
    }

    private static void handleIndexDependencies(QueryPlanForge queryPlan, StatementCompileTimeServices services) {
        if (queryPlan == null) {
            return;
        }
        HashSet<TableLookupIndexReqKey> indexes = new HashSet<>();
        for (int streamnum = 0; streamnum < queryPlan.getExecNodeSpecs().length; streamnum++) {
            QueryPlanNodeForge node = queryPlan.getExecNodeSpecs()[streamnum];
            indexes.clear();
            node.addIndexes(indexes);
            for (TableLookupIndexReqKey index : indexes) {
                if (index.getTableName() != null) {
                    TableMetaData tableMeta = services.getTableCompileTimeResolver().resolve(index.getTableName());
                    if (tableMeta.getTableVisibility() == NameAccessModifier.PUBLIC) {
                        services.getModuleDependenciesCompileTime().addPathIndex(false, index.getTableName(), tableMeta.getTableModuleName(), index.getIndexName(), index.getIndexModuleName(), services.getNamedWindowCompileTimeRegistry(), services.getTableCompileTimeRegistry());
                    }
                }
            }
        }
    }

    private static class DataFlowActivationResult {
        private final EventType streamEventType;
        private final String eventTypeName;
        private final ViewableActivatorForge viewableActivatorForge;
        private final List<ViewFactoryForge> viewForges;

        public DataFlowActivationResult(EventType streamEventType, String eventTypeName, ViewableActivatorForge viewableActivatorForge, List<ViewFactoryForge> viewForges) {
            this.streamEventType = streamEventType;
            this.eventTypeName = eventTypeName;
            this.viewableActivatorForge = viewableActivatorForge;
            this.viewForges = viewForges;
        }

        public EventType getStreamEventType() {
            return streamEventType;
        }

        public String getEventTypeName() {
            return eventTypeName;
        }

        public ViewableActivatorForge getViewableActivatorForge() {
            return viewableActivatorForge;
        }

        public List<ViewFactoryForge> getViewForges() {
            return viewForges;
        }
    }
}

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
package com.espertech.esper.common.internal.epl.fafquery.querymethod;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.compile.multikey.MultiKeyClassRef;
import com.espertech.esper.common.internal.compile.multikey.MultiKeyPlan;
import com.espertech.esper.common.internal.compile.multikey.MultiKeyPlanner;
import com.espertech.esper.common.internal.compile.stage1.Compilable;
import com.espertech.esper.common.internal.compile.stage1.spec.NamedWindowConsumerStreamSpec;
import com.espertech.esper.common.internal.compile.stage1.spec.StreamSpecCompiled;
import com.espertech.esper.common.internal.compile.stage1.spec.TableQueryStreamSpec;
import com.espertech.esper.common.internal.compile.stage2.StatementLifecycleSvcUtil;
import com.espertech.esper.common.internal.compile.stage2.StatementRawInfo;
import com.espertech.esper.common.internal.compile.stage2.StatementSpecCompiled;
import com.espertech.esper.common.internal.compile.stage3.StatementBaseInfo;
import com.espertech.esper.common.internal.compile.stage3.StatementCompileTimeServices;
import com.espertech.esper.common.internal.compile.stage3.StmtClassForgeableFactory;
import com.espertech.esper.common.internal.context.aifactory.select.StreamJoinAnalysisResultCompileTime;
import com.espertech.esper.common.internal.epl.expression.core.*;
import com.espertech.esper.common.internal.epl.expression.subquery.ExprSubselectNode;
import com.espertech.esper.common.internal.epl.expression.table.ExprTableAccessNode;
import com.espertech.esper.common.internal.epl.fafquery.processor.FireAndForgetProcessorForge;
import com.espertech.esper.common.internal.epl.fafquery.processor.FireAndForgetProcessorForgeFactory;
import com.espertech.esper.common.internal.epl.historical.common.HistoricalViewableDesc;
import com.espertech.esper.common.internal.epl.join.analyze.FilterExprAnalyzer;
import com.espertech.esper.common.internal.epl.join.analyze.OuterJoinAnalyzer;
import com.espertech.esper.common.internal.epl.join.base.JoinSetComposerPrototypeDesc;
import com.espertech.esper.common.internal.epl.join.base.JoinSetComposerPrototypeForge;
import com.espertech.esper.common.internal.epl.join.base.JoinSetComposerPrototypeForgeFactory;
import com.espertech.esper.common.internal.epl.join.hint.ExcludePlanHint;
import com.espertech.esper.common.internal.epl.join.querygraph.QueryGraphForge;
import com.espertech.esper.common.internal.epl.resultset.core.ResultSetProcessorDesc;
import com.espertech.esper.common.internal.epl.resultset.core.ResultSetProcessorFactoryFactory;
import com.espertech.esper.common.internal.epl.resultset.core.ResultSetSpec;
import com.espertech.esper.common.internal.epl.streamtype.StreamTypeService;
import com.espertech.esper.common.internal.epl.streamtype.StreamTypeServiceImpl;
import com.espertech.esper.common.internal.epl.subselect.*;
import com.espertech.esper.common.internal.epl.table.strategy.ExprTableEvalHelperPlan;
import com.espertech.esper.common.internal.epl.table.strategy.ExprTableEvalStrategyFactoryForge;
import com.espertech.esper.common.internal.metrics.audit.AuditPath;
import com.espertech.esper.common.internal.serde.compiletime.resolve.SerdeCompileTimeResolverNonHA;
import com.espertech.esper.common.internal.statement.helper.EPStatementStartMethodHelperValidate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.util.*;

/**
 * Starts and provides the stop method for EPL statements.
 */
public class FAFQueryMethodSelectDesc {
    private static final Logger QUERY_PLAN_LOG = LoggerFactory.getLogger(AuditPath.QUERYPLAN_LOG);
    private static final Logger log = LoggerFactory.getLogger(FAFQueryMethodSelectDesc.class);

    private final FireAndForgetProcessorForge[] processors;
    private final ResultSetProcessorDesc resultSetProcessor;
    private final QueryGraphForge queryGraph;
    private final ExprNode whereClause;
    private final ExprNode[] consumerFilters;
    private final JoinSetComposerPrototypeForge joins;
    private final Annotation[] annotations;
    private final String contextName;
    private boolean hasTableAccess;
    private final boolean isDistinct;
    private final MultiKeyClassRef distinctMultiKey;
    private Map<ExprTableAccessNode, ExprTableEvalStrategyFactoryForge> tableAccessForges;
    private final List<StmtClassForgeableFactory> additionalForgeables = new ArrayList<>(2);
    private final Map<ExprSubselectNode, SubSelectFactoryForge> subselectForges;

    public FAFQueryMethodSelectDesc(StatementSpecCompiled statementSpec,
                                    Compilable compilable,
                                    StatementRawInfo statementRawInfo,
                                    StatementCompileTimeServices services)
            throws ExprValidationException {
        this.annotations = statementSpec.getAnnotations();
        this.contextName = statementSpec.getRaw().getOptionalContextName();

        boolean queryPlanLogging = services.getConfiguration().getCommon().getLogging().isEnableQueryPlan();
        if (queryPlanLogging) {
            QUERY_PLAN_LOG.info("Query plans for Fire-and-forget query '" + compilable.toEPL() + "'");
        }

        this.hasTableAccess = statementSpec.getTableAccessNodes() != null && statementSpec.getTableAccessNodes().size() > 0;
        for (StreamSpecCompiled streamSpec : statementSpec.getStreamSpecs()) {
            hasTableAccess |= streamSpec instanceof TableQueryStreamSpec;
        }
        hasTableAccess |= StatementLifecycleSvcUtil.isSubqueryWithTable(statementSpec.getSubselectNodes(), services.getTableCompileTimeResolver());
        this.isDistinct = statementSpec.getSelectClauseCompiled().isDistinct();

        FAFQueryMethodHelper.validateFAFQuery(statementSpec);

        int numStreams = statementSpec.getStreamSpecs().length;
        EventType[] typesPerStream = new EventType[numStreams];
        String[] namesPerStream = new String[numStreams];
        String[] eventTypeNames = new String[numStreams];
        processors = new FireAndForgetProcessorForge[numStreams];
        consumerFilters = new ExprNode[numStreams];

        // check context partition use
        if (statementSpec.getRaw().getOptionalContextName() != null) {
            if (numStreams > 1) {
                throw new ExprValidationException("Joins in runtime queries for context partitions are not supported");
            }
        }

        // resolve types and processors
        for (int i = 0; i < numStreams; i++) {
            final StreamSpecCompiled streamSpec = statementSpec.getStreamSpecs()[i];
            processors[i] = FireAndForgetProcessorForgeFactory.validateResolveProcessor(streamSpec);
            if (numStreams > 1 && processors[i].getContextName() != null) {
                throw new ExprValidationException("Joins against named windows that are under context are not supported");
            }

            String streamName = processors[i].getNamedWindowOrTableName();
            if (streamSpec.getOptionalStreamName() != null) {
                streamName = streamSpec.getOptionalStreamName();
            }
            namesPerStream[i] = streamName;
            typesPerStream[i] = processors[i].getEventTypeRSPInputEvents();
            eventTypeNames[i] = typesPerStream[i].getName();

            List<ExprNode> consumerFilterExprs;
            if (streamSpec instanceof NamedWindowConsumerStreamSpec) {
                NamedWindowConsumerStreamSpec namedSpec = (NamedWindowConsumerStreamSpec) streamSpec;
                consumerFilterExprs = namedSpec.getFilterExpressions();
            } else {
                TableQueryStreamSpec tableSpec = (TableQueryStreamSpec) streamSpec;
                consumerFilterExprs = tableSpec.getFilterExpressions();
            }
            consumerFilters[i] = ExprNodeUtilityMake.connectExpressionsByLogicalAndWhenNeeded(consumerFilterExprs);
        }

        // compile filter to optimize access to named window
        boolean optionalStreamsIfAny = OuterJoinAnalyzer.optionalStreamsIfAny(statementSpec.getRaw().getOuterJoinDescList());
        StreamTypeServiceImpl types = new StreamTypeServiceImpl(typesPerStream, namesPerStream, new boolean[numStreams], false, optionalStreamsIfAny);
        ExcludePlanHint excludePlanHint = ExcludePlanHint.getHint(types.getStreamNames(), statementRawInfo, services);
        queryGraph = new QueryGraphForge(numStreams, excludePlanHint, false);
        if (statementSpec.getRaw().getWhereClause() != null) {
            for (int i = 0; i < numStreams; i++) {
                try {
                    ExprValidationContext validationContext = new ExprValidationContextBuilder(types, statementRawInfo, services)
                            .withAllowBindingConsumption(true).withIsFilterExpression(true).build();
                    ExprNode validated = ExprNodeUtilityValidate.getValidatedSubtree(ExprNodeOrigin.FILTER, statementSpec.getRaw().getWhereClause(), validationContext);
                    FilterExprAnalyzer.analyze(validated, queryGraph, false);
                } catch (Exception ex) {
                    log.warn("Unexpected exception analyzing filter paths: " + ex.getMessage(), ex);
                }
            }
        }

        // handle subselects
        // first we create streams for subselects, if there are any
        StatementBaseInfo base = new StatementBaseInfo(compilable, statementSpec, null, statementRawInfo, null);
        List<NamedWindowConsumerStreamSpec> subqueryNamedWindowConsumers = new ArrayList<>();
        SubSelectActivationDesc subSelectActivationDesc = SubSelectHelperActivations.createSubSelectActivation(Collections.emptyList(), subqueryNamedWindowConsumers, base, services);
        Map<ExprSubselectNode, SubSelectActivationPlan> subselectActivation = subSelectActivationDesc.getSubselects();
        additionalForgeables.addAll(subSelectActivationDesc.getAdditionalForgeables());

        SubSelectHelperForgePlan subSelectForgePlan = SubSelectHelperForgePlanner.planSubSelect(base, subselectActivation, namesPerStream, typesPerStream, eventTypeNames, services);
        subselectForges = subSelectForgePlan.getSubselects();
        additionalForgeables.addAll(subSelectForgePlan.getAdditionalForgeables());

        // obtain result set processor
        boolean[] isIStreamOnly = new boolean[namesPerStream.length];
        Arrays.fill(isIStreamOnly, true);
        StreamTypeService typeService = new StreamTypeServiceImpl(typesPerStream, namesPerStream, isIStreamOnly, true, optionalStreamsIfAny);
        whereClause = EPStatementStartMethodHelperValidate.validateNodes(statementSpec.getRaw(), typeService, null, statementRawInfo, services);

        ResultSetSpec resultSetSpec = new ResultSetSpec(statementSpec);
        resultSetProcessor = ResultSetProcessorFactoryFactory.getProcessorPrototype(resultSetSpec,
                typeService, null, new boolean[0], true, null,
                true, false, statementRawInfo, services);
        additionalForgeables.addAll(resultSetProcessor.getAdditionalForgeables());

        // plan table access
        tableAccessForges = ExprTableEvalHelperPlan.planTableAccess(statementSpec.getRaw().getTableExpressions());

        // plan joins or simple queries
        if (numStreams > 1) {
            StreamJoinAnalysisResultCompileTime streamJoinAnalysisResult = new StreamJoinAnalysisResultCompileTime(numStreams);
            Arrays.fill(streamJoinAnalysisResult.getNamedWindowsPerStream(), null);
            for (int i = 0; i < numStreams; i++) {
                String[][] uniqueIndexes = processors[i].getUniqueIndexes();
                streamJoinAnalysisResult.getUniqueKeys()[i] = uniqueIndexes;
            }

            boolean hasAggregations = resultSetProcessor.getResultSetProcessorType().isAggregated();
            JoinSetComposerPrototypeDesc desc = JoinSetComposerPrototypeForgeFactory.makeComposerPrototype(statementSpec, streamJoinAnalysisResult,
                types, new HistoricalViewableDesc(numStreams), true, hasAggregations, statementRawInfo, services);
            additionalForgeables.addAll(desc.getAdditionalForgeables());
            joins = desc.getForge();
        } else {
            joins = null;
        }

        MultiKeyPlan multiKeyPlan = MultiKeyPlanner.planMultiKeyDistinct(isDistinct, resultSetProcessor.getResultEventType(), statementRawInfo, SerdeCompileTimeResolverNonHA.INSTANCE);
        additionalForgeables.addAll(multiKeyPlan.getMultiKeyForgeables());
        this.distinctMultiKey = multiKeyPlan.getClassRef();
    }

    public JoinSetComposerPrototypeForge getJoins() {
        return joins;
    }

    public FireAndForgetProcessorForge[] getProcessors() {
        return processors;
    }

    public ResultSetProcessorDesc getResultSetProcessor() {
        return resultSetProcessor;
    }

    public QueryGraphForge getQueryGraph() {
        return queryGraph;
    }

    public boolean isHasTableAccess() {
        return hasTableAccess;
    }

    public ExprNode getWhereClause() {
        return whereClause;
    }

    public ExprNode[] getConsumerFilters() {
        return consumerFilters;
    }

    public Annotation[] getAnnotations() {
        return annotations;
    }

    public String getContextName() {
        return contextName;
    }

    public Map<ExprTableAccessNode, ExprTableEvalStrategyFactoryForge> getTableAccessForges() {
        return tableAccessForges;
    }

    public boolean isDistinct() {
        return isDistinct;
    }

    public List<StmtClassForgeableFactory> getAdditionalForgeables() {
        return additionalForgeables;
    }

    public MultiKeyClassRef getDistinctMultiKey() {
        return distinctMultiKey;
    }

    public Map<ExprSubselectNode, SubSelectFactoryForge> getSubselectForges() {
        return subselectForges;
    }
}

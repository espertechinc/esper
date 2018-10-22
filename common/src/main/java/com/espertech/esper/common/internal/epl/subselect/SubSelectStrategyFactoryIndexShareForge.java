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
package com.espertech.esper.common.internal.epl.subselect;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.util.NameAccessModifier;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.compile.stage3.StatementBaseInfo;
import com.espertech.esper.common.internal.compile.stage3.StatementCompileTimeServices;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;
import com.espertech.esper.common.internal.epl.agg.core.AggregationServiceForgeDesc;
import com.espertech.esper.common.internal.epl.expression.core.*;
import com.espertech.esper.common.internal.epl.join.hint.IndexHint;
import com.espertech.esper.common.internal.epl.lookupplan.SubordPropPlan;
import com.espertech.esper.common.internal.epl.lookupplansubord.SubordinateQueryIndexDescForge;
import com.espertech.esper.common.internal.epl.lookupplansubord.SubordinateQueryPlanDescForge;
import com.espertech.esper.common.internal.epl.lookupplansubord.SubordinateQueryPlanner;
import com.espertech.esper.common.internal.epl.lookupplansubord.SubordinateQueryPlannerUtil;
import com.espertech.esper.common.internal.epl.namedwindow.core.NamedWindowDeployTimeResolver;
import com.espertech.esper.common.internal.epl.namedwindow.path.NamedWindowMetaData;
import com.espertech.esper.common.internal.epl.table.compiletime.TableMetaData;
import com.espertech.esper.common.internal.epl.table.core.TableDeployTimeResolver;
import com.espertech.esper.common.internal.metrics.audit.AuditPath;
import com.espertech.esper.common.internal.view.core.ViewFactoryForge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class SubSelectStrategyFactoryIndexShareForge implements SubSelectStrategyFactoryForge {
    private static final Logger QUERY_PLAN_LOG = LoggerFactory.getLogger(AuditPath.QUERYPLAN_LOG);

    private final int subqueryNumber;
    private final NamedWindowMetaData namedWindow;
    private final TableMetaData table;
    private final ExprForge filterExprEval;
    private final ExprNode[] groupKeys;
    private final AggregationServiceForgeDesc aggregationServiceForgeDesc;
    private final SubordinateQueryPlanDescForge queryPlan;

    public SubSelectStrategyFactoryIndexShareForge(int subqueryNumber, SubSelectActivationPlan subselectActivation, EventType[] outerEventTypesSelect, NamedWindowMetaData namedWindow, TableMetaData table, boolean fullTableScan, IndexHint indexHint, SubordPropPlan joinedPropPlan, ExprForge filterExprEval, ExprNode[] groupKeys, AggregationServiceForgeDesc aggregationServiceForgeDesc, StatementBaseInfo statement, StatementCompileTimeServices services)
            throws ExprValidationException {
        this.subqueryNumber = subqueryNumber;
        this.namedWindow = namedWindow;
        this.table = table;
        this.filterExprEval = filterExprEval;
        this.groupKeys = groupKeys;
        this.aggregationServiceForgeDesc = aggregationServiceForgeDesc;

        boolean queryPlanLogging = services.getConfiguration().getCommon().getLogging().isEnableQueryPlan();

        // We only use existing indexes in all cases. This means "create index" is required.
        if (table != null) {
            queryPlan = SubordinateQueryPlanner.planSubquery(outerEventTypesSelect, joinedPropPlan, false, fullTableScan, indexHint, true, subqueryNumber,
                    false, table.getIndexMetadata(), table.getUniquenessAsSet(), true,
                    table.getInternalEventType(), statement.getStatementRawInfo(), services);

            if (queryPlan != null && queryPlan.getIndexDescs() != null) {
                for (int i = 0; i < queryPlan.getIndexDescs().length; i++) {
                    SubordinateQueryIndexDescForge index = queryPlan.getIndexDescs()[i];
                    if (table.getTableVisibility() == NameAccessModifier.PUBLIC) {
                        services.getModuleDependenciesCompileTime().addPathIndex(false, table.getTableName(), table.getTableModuleName(), index.getIndexName(), index.getIndexModuleName(), services.getNamedWindowCompileTimeRegistry(), services.getTableCompileTimeRegistry());
                    }
                }
            }
        } else {
            queryPlan = SubordinateQueryPlanner.planSubquery(outerEventTypesSelect, joinedPropPlan, false, fullTableScan, indexHint, true, subqueryNumber,
                    namedWindow.isVirtualDataWindow(), namedWindow.getIndexMetadata(), namedWindow.getUniquenessAsSet(), true,
                    namedWindow.getEventType(), statement.getStatementRawInfo(), services);

            if (queryPlan != null && queryPlan.getIndexDescs() != null) {
                for (int i = 0; i < queryPlan.getIndexDescs().length; i++) {
                    SubordinateQueryIndexDescForge index = queryPlan.getIndexDescs()[i];
                    if (namedWindow.getEventType().getMetadata().getAccessModifier() == NameAccessModifier.PUBLIC) {
                        services.getModuleDependenciesCompileTime().addPathIndex(true, namedWindow.getEventType().getName(), namedWindow.getNamedWindowModuleName(), index.getIndexName(), index.getIndexModuleName(), services.getNamedWindowCompileTimeRegistry(), services.getTableCompileTimeRegistry());
                    }
                }
            }
        }

        SubordinateQueryPlannerUtil.queryPlanLogOnSubq(queryPlanLogging, QUERY_PLAN_LOG, queryPlan, subqueryNumber, statement.getStatementRawInfo().getAnnotations(), services.getClasspathImportServiceCompileTime());
    }

    public CodegenExpression makeCodegen(CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        CodegenMethod method = parent.makeChild(SubSelectStrategyFactoryIndexShare.class, this.getClass(), classScope);

        CodegenExpression groupKeyEval = constantNull();
        if (groupKeys != null) {
            groupKeyEval = ExprNodeUtilityCodegen.codegenEvaluatorMayMultiKeyWCoerce(ExprNodeUtilityQuery.getForges(groupKeys), null, method, this.getClass(), classScope);
        }

        method.getBlock()
                .declareVar(SubSelectStrategyFactoryIndexShare.class, "s", newInstance(SubSelectStrategyFactoryIndexShare.class))
                .exprDotMethod(ref("s"), "setTable", table == null ? constantNull() : TableDeployTimeResolver.makeResolveTable(table, symbols.getAddInitSvc(method)))
                .exprDotMethod(ref("s"), "setNamedWindow", namedWindow == null ? constantNull() : NamedWindowDeployTimeResolver.makeResolveNamedWindow(namedWindow, symbols.getAddInitSvc(method)))
                .exprDotMethod(ref("s"), "setAggregationServiceFactory", SubSelectStrategyFactoryLocalViewPreloadedForge.makeAggregationService(subqueryNumber, aggregationServiceForgeDesc, classScope, method, symbols))
                .exprDotMethod(ref("s"), "setFilterExprEval", filterExprEval == null ? constantNull() : ExprNodeUtilityCodegen.codegenEvaluatorNoCoerce(filterExprEval, method, this.getClass(), classScope))
                .exprDotMethod(ref("s"), "setGroupKeyEval", groupKeyEval)
                .exprDotMethod(ref("s"), "setQueryPlan", queryPlan == null ? constantNull() : queryPlan.make(method, symbols, classScope))
                .methodReturn(ref("s"));
        return localMethod(method);
    }

    public List<ViewFactoryForge> getViewForges() {
        return Collections.emptyList();
    }

    public boolean hasAggregation() {
        return aggregationServiceForgeDesc != null;
    }

    public boolean hasPrior() {
        return false;
    }

    public boolean hasPrevious() {
        return false;
    }
}

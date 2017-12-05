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
package com.espertech.esper.epl.expression.subquery;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventType;
import com.espertech.esper.codegen.base.CodegenClassScope;
import com.espertech.esper.codegen.base.CodegenMethodScope;
import com.espertech.esper.epl.expression.codegen.CodegenLegoEvaluateSelf;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.epl.agg.service.common.AggregationService;
import com.espertech.esper.epl.core.engineimport.EngineImportService;
import com.espertech.esper.epl.core.streamtype.StreamTypeService;
import com.espertech.esper.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.epl.expression.core.*;
import com.espertech.esper.core.service.speccompiled.StatementSpecCompiled;
import com.espertech.esper.epl.spec.StatementSpecRaw;
import com.espertech.esper.epl.util.ExprNodeUtilityRich;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;

import java.io.StringWriter;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Represents a subselect in an expression tree.
 */
public abstract class ExprSubselectNode extends ExprNodeBase implements ExprEvaluator, ExprEnumerationForge, ExprEnumerationEval, ExprTypableReturnForge, ExprTypableReturnEval, ExprForge {
    public static final ExprSubselectNode[] EMPTY_SUBSELECT_ARRAY = new ExprSubselectNode[0];
    private static final long serialVersionUID = -2469169635913155764L;

    /**
     * The validated select clause.
     */
    protected ExprNode[] selectClause;
    protected transient ExprEvaluator[] selectClauseEvaluator;

    protected String[] selectAsNames;

    /**
     * The validate filter expression.
     */
    protected transient ExprEvaluator filterExpr;

    /**
     * The validated having expression.
     */
    protected transient ExprEvaluator havingExpr;

    /**
     * The event type generated for wildcard selects.
     */
    protected transient EventType rawEventType;

    protected String statementName;

    private transient StreamTypeService filterSubqueryStreamTypes;
    private StatementSpecRaw statementSpecRaw;
    private transient StatementSpecCompiled statementSpecCompiled;
    private transient ExprSubselectStrategy strategy;
    private transient SubqueryAggregationType subselectAggregationType;
    protected int subselectNumber;
    private boolean filterStreamSubselect;
    protected transient AggregationService subselectAggregationService;

    /**
     * Evaluate the lookup expression returning an evaluation result object.
     *
     * @param eventsPerStream      is the events for each stream in a join
     * @param isNewData            is true for new data, or false for old data
     * @param matchingEvents       is filtered results from the table of stored lookup events
     * @param exprEvaluatorContext context for expression evalauation
     * @return evaluation result
     */
    public abstract Object evaluate(EventBean[] eventsPerStream, boolean isNewData, Collection<EventBean> matchingEvents, ExprEvaluatorContext exprEvaluatorContext);

    public abstract Collection<EventBean> evaluateGetCollEvents(EventBean[] eventsPerStream, boolean isNewData, Collection<EventBean> matchingEvents, ExprEvaluatorContext exprEvaluatorContext);

    public abstract Collection evaluateGetCollScalar(EventBean[] eventsPerStream, boolean isNewData, Collection<EventBean> matchingEvents, ExprEvaluatorContext exprEvaluatorContext);

    public abstract EventBean evaluateGetEventBean(EventBean[] eventsPerStream, boolean isNewData, Collection<EventBean> matchingEvents, ExprEvaluatorContext exprEvaluatorContext);

    public abstract boolean isAllowMultiColumnSelect();

    public abstract void validateSubquery(ExprValidationContext validationContext) throws ExprValidationException;

    public abstract LinkedHashMap<String, Object> typableGetRowProperties() throws ExprValidationException;

    public abstract Object[] evaluateTypableSingle(EventBean[] eventsPerStream, boolean isNewData, Collection<EventBean> matchingEvents, ExprEvaluatorContext exprEvaluatorContext);

    public abstract Object[][] evaluateTypableMulti(EventBean[] eventsPerStream, boolean isNewData, Collection<EventBean> matchingEvents, ExprEvaluatorContext exprEvaluatorContext);

    /**
     * Ctor.
     *
     * @param statementSpec is the lookup statement spec from the parser, unvalidated
     */
    public ExprSubselectNode(StatementSpecRaw statementSpec) {
        this.statementSpecRaw = statementSpec;
    }

    public ExprEvaluator getExprEvaluator() {
        return this;
    }

    public boolean isConstantResult() {
        return false;
    }

    public ExprForge getForge() {
        return this;
    }

    public ExprNodeRenderable getForgeRenderable() {
        return this;
    }

    public ExprNode validate(ExprValidationContext validationContext) throws ExprValidationException {
        this.statementName = validationContext.getStatementName();
        validateSubquery(validationContext);
        return null;
    }

    /**
     * Supplies a compiled statement spec.
     *
     * @param statementSpecCompiled compiled validated filters
     * @param subselectNumber       subselect assigned number
     */
    public void setStatementSpecCompiled(StatementSpecCompiled statementSpecCompiled, int subselectNumber) {
        this.statementSpecCompiled = statementSpecCompiled;
        this.subselectNumber = subselectNumber;
    }

    /**
     * Returns the compiled statement spec.
     *
     * @return compiled statement
     */
    public StatementSpecCompiled getStatementSpecCompiled() {
        return statementSpecCompiled;
    }

    /**
     * Sets the validates select clause
     *
     * @param selectClause is the expression representing the select clause
     * @param engineImportService engine import service
     * @param statementName name
     */
    public void setSelectClause(ExprNode[] selectClause, EngineImportService engineImportService, String statementName) {
        this.selectClause = selectClause;
        this.selectClauseEvaluator = ExprNodeUtilityRich.getEvaluatorsMayCompile(selectClause, engineImportService, ExprSubselectNode.class, false, statementName);
    }

    public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qExprSubselect(this);
            Collection<EventBean> matchingEvents = evaluateMatching(eventsPerStream, exprEvaluatorContext);
            Object result = evaluate(eventsPerStream, isNewData, matchingEvents, exprEvaluatorContext);
            InstrumentationHelper.get().aExprSubselect(result);
            return result;
        }
        Collection<EventBean> matchingEvents = evaluateMatching(eventsPerStream, exprEvaluatorContext);
        return evaluate(eventsPerStream, isNewData, matchingEvents, exprEvaluatorContext);
    }

    public CodegenExpression evaluateCodegen(Class requiredType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        return CodegenLegoEvaluateSelf.evaluateSelfPlainWithCast(requiredType, this, getEvaluationType(), codegenMethodScope, exprSymbol, codegenClassScope);
    }

    public ExprForgeComplexityEnum getComplexity() {
        return ExprForgeComplexityEnum.SELF;
    }

    public Collection<EventBean> evaluateGetROCollectionEvents(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        Collection<EventBean> matchingEvents = evaluateMatching(eventsPerStream, exprEvaluatorContext);
        return evaluateGetCollEvents(eventsPerStream, isNewData, matchingEvents, exprEvaluatorContext);
    }

    public CodegenExpression evaluateGetROCollectionEventsCodegen(CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        return CodegenLegoEvaluateSelf.evaluateSelfGetROCollectionEvents(this, codegenMethodScope, exprSymbol, codegenClassScope);
    }

    public Collection evaluateGetROCollectionScalar(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        Collection<EventBean> matchingEvents = evaluateMatching(eventsPerStream, exprEvaluatorContext);
        return evaluateGetCollScalar(eventsPerStream, isNewData, matchingEvents, exprEvaluatorContext);
    }

    public CodegenExpression evaluateGetROCollectionScalarCodegen(CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        return CodegenLegoEvaluateSelf.evaluateSelfGetROCollectionScalar(this, codegenMethodScope, exprSymbol, codegenClassScope);
    }

    public EventBean evaluateGetEventBean(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        Collection<EventBean> matchingEvents = evaluateMatching(eventsPerStream, context);
        return evaluateGetEventBean(eventsPerStream, isNewData, matchingEvents, context);
    }

    private Collection<EventBean> evaluateMatching(EventBean[] eventsPerStream, ExprEvaluatorContext exprEvaluatorContext) {
        return strategy.evaluateMatching(eventsPerStream, exprEvaluatorContext);
    }

    public LinkedHashMap<String, Object> getRowProperties() throws ExprValidationException {
        return typableGetRowProperties();
    }

    public Boolean isMultirow() {
        return true;   // subselect can always return multiple rows
    }

    public Object[] evaluateTypableSingle(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        Collection<EventBean> matching = strategy.evaluateMatching(eventsPerStream, context);
        return evaluateTypableSingle(eventsPerStream, isNewData, matching, context);
    }

    public CodegenExpression evaluateTypableSingleCodegen(CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        return CodegenLegoEvaluateSelf.evaluateSelfTypableSingle(this, codegenMethodScope, exprSymbol, codegenClassScope);
    }

    public Object[][] evaluateTypableMulti(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        Collection<EventBean> matching = strategy.evaluateMatching(eventsPerStream, context);
        return evaluateTypableMulti(eventsPerStream, isNewData, matching, context);
    }

    public CodegenExpression evaluateTypableMultiCodegen(CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        return CodegenLegoEvaluateSelf.evaluateSelfTypableMulti(this, codegenMethodScope, exprSymbol, codegenClassScope);
    }

    public ExprEnumerationEval getExprEvaluatorEnumeration() {
        return this;
    }

    public CodegenExpression evaluateGetEventBeanCodegen(CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        return CodegenLegoEvaluateSelf.evaluateSelfGetEventBean(this, codegenMethodScope, exprSymbol, codegenClassScope);
    }

    /**
     * Returns the uncompiled statement spec.
     *
     * @return statement spec uncompiled
     */
    public StatementSpecRaw getStatementSpecRaw() {
        return statementSpecRaw;
    }

    /**
     * Supplies the name of the select expression as-tag
     *
     * @param selectAsNames is the as-name(s)
     */
    public void setSelectAsNames(String[] selectAsNames) {
        this.selectAsNames = selectAsNames;
    }

    /**
     * Sets the validated filter expression, or null if there is none.
     *
     * @param filterExpr is the filter
     */
    public void setFilterExpr(ExprEvaluator filterExpr) {
        this.filterExpr = filterExpr;
    }

    public void setHavingExpr(ExprEvaluator havingExpr) {
        this.havingExpr = havingExpr;
    }

    public void toPrecedenceFreeEPL(StringWriter writer) {
        if ((selectAsNames != null) && (selectAsNames[0] != null)) {
            writer.append(selectAsNames[0]);
            return;
        }
        writer.append("subselect_");
        writer.append(Integer.toString(subselectNumber));
    }

    public ExprPrecedenceEnum getPrecedence() {
        return ExprPrecedenceEnum.UNARY;
    }

    public boolean equalsNode(ExprNode node, boolean ignoreStreamPrefix) {
        return false;   // 2 subselects are never equivalent
    }

    /**
     * Sets the strategy for boiling down the table of lookup events into a subset against which to run the filter.
     *
     * @param strategy is the looking strategy (full table scan or indexed)
     */
    public void setStrategy(ExprSubselectStrategy strategy) {
        this.strategy = strategy;
    }

    /**
     * Sets the event type generated for wildcard selects.
     *
     * @param rawEventType is the wildcard type (parent view)
     */
    public void setRawEventType(EventType rawEventType) {
        this.rawEventType = rawEventType;
    }

    /**
     * Returns the select clause or null if none.
     *
     * @return clause
     */
    public ExprNode[] getSelectClause() {
        return selectClause;
    }

    /**
     * Returns filter expr or null if none.
     *
     * @return filter
     */
    public ExprEvaluator getFilterExpr() {
        return filterExpr;
    }

    public ExprEvaluator getHavingExpr() {
        return havingExpr;
    }

    /**
     * Returns the event type.
     *
     * @return type
     */
    public EventType getRawEventType() {
        return rawEventType;
    }

    /**
     * Return stream types.
     *
     * @return types
     */
    public StreamTypeService getFilterSubqueryStreamTypes() {
        return filterSubqueryStreamTypes;
    }

    /**
     * Set stream types.
     *
     * @param filterSubqueryStreamTypes types
     */
    public void setFilterSubqueryStreamTypes(StreamTypeService filterSubqueryStreamTypes) {
        this.filterSubqueryStreamTypes = filterSubqueryStreamTypes;
    }

    public SubqueryAggregationType getSubselectAggregationType() {
        return subselectAggregationType;
    }

    public void setSubselectAggregationType(SubqueryAggregationType subselectAggregationType) {
        this.subselectAggregationType = subselectAggregationType;
    }

    public int getSubselectNumber() {
        return subselectNumber;
    }

    public void setFilterStreamSubselect(boolean filterStreamSubselect) {
        this.filterStreamSubselect = filterStreamSubselect;
    }

    public boolean isFilterStreamSubselect() {
        return filterStreamSubselect;
    }

    public static ExprSubselectNode[] toArray(List<ExprSubselectNode> subselectNodes) {
        if (subselectNodes.isEmpty()) {
            return EMPTY_SUBSELECT_ARRAY;
        }
        return subselectNodes.toArray(new ExprSubselectNode[subselectNodes.size()]);
    }

    public void setSubselectAggregationService(AggregationService subselectAggregationService) {
        this.subselectAggregationService = subselectAggregationService;
    }

    public AggregationService getSubselectAggregationService() {
        return subselectAggregationService;
    }

    public ExprTypableReturnEval getTypableReturnEvaluator() {
        return this;
    }

    public static enum SubqueryAggregationType {
        NONE,
        FULLY_AGGREGATED_NOPROPS,
        FULLY_AGGREGATED_WPROPS
    }
}

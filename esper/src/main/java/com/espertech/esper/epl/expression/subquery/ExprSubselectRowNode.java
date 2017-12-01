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
import com.espertech.esper.client.FragmentEventType;
import com.espertech.esper.epl.expression.core.*;
import com.espertech.esper.epl.spec.StatementSpecRaw;
import com.espertech.esper.epl.table.mgmt.TableMetadata;
import com.espertech.esper.event.EventAdapterService;
import com.espertech.esper.util.CollectionUtil;
import com.espertech.esper.util.JavaClassHelper;

import java.util.*;

/**
 * Represents a subselect in an expression tree.
 */
public class ExprSubselectRowNode extends ExprSubselectNode {
    private static final long serialVersionUID = -7865711714805807559L;

    public static final SubselectEvalStrategyRow UNFILTERED_SELECTED = new SubselectEvalStrategyRowUnfilteredSelected();
    public static final SubselectEvalStrategyRow FILTERED_UNSELECTED = new SubselectEvalStrategyRowFilteredUnselected();
    public static final SubselectEvalStrategyRow FILTERED_SELECTED = new SubselectEvalStrategyRowFilteredSelected();
    public static final SubselectEvalStrategyRow HAVING_SELECTED = new SubselectEvalStrategyRowHavingSelected();
    public static final SubselectEvalStrategyRow UNFILTERED_SELECTED_GROUPED = new SubselectEvalStrategyRowUnfilteredSelectedGroupedNoHaving();

    protected transient SubselectMultirowType subselectMultirowType;
    private transient SubselectEvalStrategyRow evalStrategy;

    /**
     * Ctor.
     *
     * @param statementSpec is the lookup statement spec from the parser, unvalidated
     */
    public ExprSubselectRowNode(StatementSpecRaw statementSpec) {
        super(statementSpec);
    }

    public Class getEvaluationType() {
        if (selectClause == null) {   // wildcards allowed
            return rawEventType.getUnderlyingType();
        }
        if (selectClause.length == 1) {
            return JavaClassHelper.getBoxedType(selectClause[0].getForge().getEvaluationType());
        }
        return Map.class;
    }

    public void validateSubquery(ExprValidationContext validationContext) throws ExprValidationException {
        // Strategy for subselect depends on presence of filter + presence of select clause expressions
        // the filter expression is handled elsewhere if there is any aggregation
        if (filterExpr == null) {
            if (selectClause == null) {
                TableMetadata tableMetadata = validationContext.getTableService().getTableMetadataFromEventType(rawEventType);
                if (tableMetadata != null) {
                    evalStrategy = new SubselectEvalStrategyRowUnfilteredUnselectedTable(tableMetadata);
                } else {
                    evalStrategy = SubselectEvalStrategyRowUnfilteredUnselected.INSTANCE;
                }
            } else {
                if (getStatementSpecCompiled().getGroupByExpressions() != null && getStatementSpecCompiled().getGroupByExpressions().getGroupByNodes().length > 0) {
                    if (havingExpr != null) {
                        evalStrategy = new SubselectEvalStrategyRowUnfilteredSelectedGroupedWHaving(havingExpr);
                    } else {
                        evalStrategy = UNFILTERED_SELECTED_GROUPED;
                    }
                } else {
                    if (havingExpr != null) {
                        evalStrategy = HAVING_SELECTED;
                    } else {
                        evalStrategy = UNFILTERED_SELECTED;
                    }
                }
            }
        } else {
            if (selectClause == null) {
                TableMetadata tableMetadata = validationContext.getTableService().getTableMetadataFromEventType(rawEventType);
                if (tableMetadata != null) {
                    evalStrategy = new SubselectEvalStrategyRowFilteredUnselectedTable(tableMetadata);
                } else {
                    evalStrategy = FILTERED_UNSELECTED;
                }
            } else {
                evalStrategy = FILTERED_SELECTED;
            }
        }
    }

    public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, Collection<EventBean> matchingEvents, ExprEvaluatorContext exprEvaluatorContext) {
        if (matchingEvents == null || matchingEvents.size() == 0) {
            return null;
        }
        return evalStrategy.evaluate(eventsPerStream, isNewData, matchingEvents, exprEvaluatorContext, this);
    }

    public Collection<EventBean> evaluateGetCollEvents(EventBean[] eventsPerStream, boolean isNewData, Collection<EventBean> matchingEvents, ExprEvaluatorContext context) {
        if (matchingEvents == null) {
            return null;
        }
        if (matchingEvents.size() == 0) {
            return Collections.emptyList();
        }
        return evalStrategy.evaluateGetCollEvents(eventsPerStream, isNewData, matchingEvents, context, this);
    }

    public Collection evaluateGetCollScalar(EventBean[] eventsPerStream, boolean isNewData, Collection<EventBean> matchingEvents, ExprEvaluatorContext context) {
        if (matchingEvents == null) {
            return null;
        }
        if (matchingEvents.size() == 0) {
            return Collections.emptyList();
        }
        return evalStrategy.evaluateGetCollScalar(eventsPerStream, isNewData, matchingEvents, context, this);
    }

    public EventBean evaluateGetEventBean(EventBean[] eventsPerStream, boolean isNewData, Collection<EventBean> matchingEvents, ExprEvaluatorContext exprEvaluatorContext) {
        if (matchingEvents == null || matchingEvents.size() == 0) {
            return null;
        }
        return evalStrategy.evaluateGetEventBean(eventsPerStream, isNewData, matchingEvents, exprEvaluatorContext, this);
    }

    public Object[] evaluateTypableSingle(EventBean[] eventsPerStream, boolean isNewData, Collection<EventBean> matchingEvents, ExprEvaluatorContext exprEvaluatorContext) {

        if (matchingEvents == null || matchingEvents.size() == 0) {
            return null;
        }
        return evalStrategy.typableEvaluate(eventsPerStream, isNewData, matchingEvents, exprEvaluatorContext, this);
    }

    public Object[][] evaluateTypableMulti(EventBean[] eventsPerStream, boolean isNewData, Collection<EventBean> matchingEvents, ExprEvaluatorContext exprEvaluatorContext) {
        if (matchingEvents == null) {
            return null;
        }
        if (matchingEvents.size() == 0) {
            return CollectionUtil.OBJECTARRAYARRAY_EMPTY;
        }
        return evalStrategy.typableEvaluateMultirow(eventsPerStream, isNewData, matchingEvents, exprEvaluatorContext, this);
    }

    public LinkedHashMap<String, Object> typableGetRowProperties() throws ExprValidationException {
        if ((selectClause == null) || (selectClause.length < 2)) {
            return null;
        }
        return getRowType();
    }

    public EventType getEventTypeSingle(EventAdapterService eventAdapterService, int statementId) throws ExprValidationException {
        if (selectClause == null) {
            return null;
        }
        if (this.getSubselectAggregationType() != SubqueryAggregationType.FULLY_AGGREGATED_NOPROPS) {
            return null;
        }
        return getAssignAnonymousType(eventAdapterService, statementId);
    }

    public EventType getEventTypeCollection(EventAdapterService eventAdapterService, int statementId) throws ExprValidationException {
        if (selectClause == null) {   // wildcards allowed
            return rawEventType;
        }

        // special case: selecting a single property that is itself an event
        if (selectClause.length == 1 && selectClause[0] instanceof ExprIdentNode) {
            ExprIdentNode identNode = (ExprIdentNode) selectClause[0];
            FragmentEventType fragment = rawEventType.getFragmentType(identNode.getResolvedPropertyName());
            if (fragment != null && !fragment.isIndexed()) {
                return fragment.getFragmentType();
            }
        }

        // select of a single value otherwise results in a collection of scalar values
        if (selectClause.length == 1) {
            return null;
        }

        // fully-aggregated always returns zero or one row
        if (this.getSubselectAggregationType() == SubqueryAggregationType.FULLY_AGGREGATED_NOPROPS) {
            return null;
        }

        return getAssignAnonymousType(eventAdapterService, statementId);
    }

    private EventType getAssignAnonymousType(EventAdapterService eventAdapterService, int statementId) throws ExprValidationException {
        Map<String, Object> rowType = getRowType();
        EventType resultEventType = eventAdapterService.createAnonymousMapType(statementId + "_subquery_" + this.getSubselectNumber(), rowType, true);
        subselectMultirowType = new SubselectMultirowType(resultEventType, eventAdapterService);
        return resultEventType;
    }

    public Class getComponentTypeCollection() throws ExprValidationException {
        if (selectClause == null) {   // wildcards allowed
            return null;
        }
        if (selectClauseEvaluator.length > 1) {
            return null;
        }
        return selectClause[0].getForge().getEvaluationType();
    }

    public boolean isAllowMultiColumnSelect() {
        return true;
    }

    private LinkedHashMap<String, Object> getRowType() throws ExprValidationException {
        Set<String> uniqueNames = new HashSet<String>();
        LinkedHashMap<String, Object> type = new LinkedHashMap<String, Object>();

        for (int i = 0; i < selectClause.length; i++) {
            String assignedName = this.selectAsNames[i];
            if (assignedName == null) {
                assignedName = ExprNodeUtilityCore.toExpressionStringMinPrecedenceSafe(selectClause[i]);
            }
            if (uniqueNames.add(assignedName)) {
                type.put(assignedName, selectClause[i].getForge().getEvaluationType());
            } else {
                throw new ExprValidationException("Column " + i + " in subquery does not have a unique column name assigned");
            }
        }
        return type;
    }

    public String getMultirowMessage() {
        return "Subselect of statement '" + statementName + "' returned more then one row in subselect " + subselectNumber + " '" + ExprNodeUtilityCore.toExpressionStringMinPrecedenceSafe(this) + "', returning null result";
    }

    protected Map<String, Object> evaluateRow(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        Map<String, Object> map = new HashMap<String, Object>();
        for (int i = 0; i < selectClauseEvaluator.length; i++) {
            Object resultEntry = selectClauseEvaluator[i].evaluate(eventsPerStream, isNewData, context);
            map.put(selectAsNames[i], resultEntry);
        }
        return map;
    }

    protected static class SubselectMultirowType {
        private final EventType eventType;
        private final EventAdapterService eventAdapterService;

        private SubselectMultirowType(EventType eventType, EventAdapterService eventAdapterService) {
            this.eventType = eventType;
            this.eventAdapterService = eventAdapterService;
        }

        public EventType getEventType() {
            return eventType;
        }

        public EventAdapterService getEventAdapterService() {
            return eventAdapterService;
        }
    }
}

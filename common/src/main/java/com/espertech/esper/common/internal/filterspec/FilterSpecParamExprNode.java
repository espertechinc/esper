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
package com.espertech.esper.common.internal.filterspec;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;
import com.espertech.esper.common.internal.epl.expression.core.ExprFilterSpecLookupable;
import com.espertech.esper.common.internal.event.core.EventBeanTypedEventFactory;

/**
 * This class represents an arbitrary expression node returning a boolean value as a filter parameter in an {@link FilterSpecActivatable} filter specification.
 */
public abstract class FilterSpecParamExprNode extends FilterSpecParam {
    private ExprEvaluator exprNode;
    private String exprText;
    private EventBeanTypedEventFactory eventBeanTypedEventFactory;
    protected FilterBooleanExpressionFactory filterBooleanExpressionFactory; // subclasses by generated code
    private boolean hasVariable;
    private boolean useLargeThreadingProfile;
    private boolean hasFilterStreamSubquery;
    private boolean hasTableAccess;
    private int statementIdBooleanExpr;
    private int filterBoolExprId;
    private EventType[] eventTypesProvidedBy;

    public FilterSpecParamExprNode(ExprFilterSpecLookupable lookupable, FilterOperator filterOperator) {
        super(lookupable, filterOperator);
    }

    public void setExprNode(ExprEvaluator exprNode) {
        this.exprNode = exprNode;
    }

    public void setExprText(String exprText) {
        this.exprText = exprText;
    }

    public void setEventBeanTypedEventFactory(EventBeanTypedEventFactory eventBeanTypedEventFactory) {
        this.eventBeanTypedEventFactory = eventBeanTypedEventFactory;
    }

    public void setFilterBooleanExpressionFactory(FilterBooleanExpressionFactory filterBooleanExpressionFactory) {
        this.filterBooleanExpressionFactory = filterBooleanExpressionFactory;
    }

    public void setHasVariable(boolean hasVariable) {
        this.hasVariable = hasVariable;
    }

    public void setHasFilterStreamSubquery(boolean hasFilterStreamSubquery) {
        this.hasFilterStreamSubquery = hasFilterStreamSubquery;
    }

    public void setHasTableAccess(boolean hasTableAccess) {
        this.hasTableAccess = hasTableAccess;
    }

    public void setEventTypesProvidedBy(EventType[] eventTypesProvidedBy) {
        this.eventTypesProvidedBy = eventTypesProvidedBy;
    }

    public ExprEvaluator getExprNode() {
        return exprNode;
    }

    public int getFilterBoolExprId() {
        return filterBoolExprId;
    }

    public void setFilterBoolExprId(int filterBoolExprId) {
        this.filterBoolExprId = filterBoolExprId;
    }

    public EventBeanTypedEventFactory getEventBeanTypedEventFactory() {
        return eventBeanTypedEventFactory;
    }

    public FilterBooleanExpressionFactory getFilterBooleanExpressionFactory() {
        return filterBooleanExpressionFactory;
    }

    public boolean isHasVariable() {
        return hasVariable;
    }

    public boolean isUseLargeThreadingProfile() {
        return useLargeThreadingProfile;
    }

    public boolean isHasFilterStreamSubquery() {
        return hasFilterStreamSubquery;
    }

    public boolean isHasTableAccess() {
        return hasTableAccess;
    }

    public String getExprText() {
        return exprText;
    }

    public EventType[] getEventTypesProvidedBy() {
        return eventTypesProvidedBy;
    }

    public void setStatementIdBooleanExpr(int statementIdBooleanExpr) {
        this.statementIdBooleanExpr = statementIdBooleanExpr;
    }

    public int getStatementIdBooleanExpr() {
        return statementIdBooleanExpr;
    }

    public void setUseLargeThreadingProfile(boolean useLargeThreadingProfile) {
        this.useLargeThreadingProfile = useLargeThreadingProfile;
    }
}

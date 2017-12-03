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
package com.espertech.esper.epl.property;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventType;
import com.espertech.esper.client.FragmentEventType;
import com.espertech.esper.epl.expression.core.ExprNodeUtilityCore;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.filterspec.PropertyEvaluator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Property evaluator that considers only level one and considers a where-clause,
 * but does not consider a select clause or N-level.
 */
public class PropertyEvaluatorSimple implements PropertyEvaluator {
    private static final Logger log = LoggerFactory.getLogger(PropertyEvaluatorSimple.class);
    private final ContainedEventEval containedEventEval;
    private final FragmentEventType fragmentEventType;
    private final ExprEvaluator filter;
    private final String expressionText;

    /**
     * Ctor.
     *
     * @param containedEventEval property getter or other evaluator
     * @param fragmentEventType  property event type
     * @param filter             optional where-clause expression
     * @param expressionText     the property name
     */
    public PropertyEvaluatorSimple(ContainedEventEval containedEventEval, FragmentEventType fragmentEventType, ExprEvaluator filter, String expressionText) {
        this.fragmentEventType = fragmentEventType;
        this.containedEventEval = containedEventEval;
        this.filter = filter;
        this.expressionText = expressionText;
    }

    public EventBean[] getProperty(EventBean theEvent, ExprEvaluatorContext exprEvaluatorContext) {
        try {
            Object result = containedEventEval.getFragment(theEvent, new EventBean[]{theEvent}, exprEvaluatorContext);

            EventBean[] rows;
            if (fragmentEventType.isIndexed()) {
                rows = (EventBean[]) result;
            } else {
                rows = new EventBean[]{(EventBean) result};
            }

            if (filter == null) {
                return rows;
            }
            return ExprNodeUtilityCore.applyFilterExpression(filter, theEvent, (EventBean[]) result, exprEvaluatorContext);
        } catch (RuntimeException ex) {
            log.error("Unexpected error evaluating property expression for event of type '" +
                    theEvent.getEventType().getName() +
                    "' and property '" +
                    expressionText + "': " + ex.getMessage(), ex);
        }
        return null;
    }

    public EventType getFragmentEventType() {
        return fragmentEventType.getFragmentType();
    }

    /**
     * Returns the property name.
     *
     * @return property name
     */
    public String getExpressionText() {
        return expressionText;
    }

    /**
     * Returns the filter.
     *
     * @return filter
     */
    public ExprEvaluator getFilter() {
        return filter;
    }

    public boolean compareTo(PropertyEvaluator otherEval) {
        if (!(otherEval instanceof PropertyEvaluatorSimple)) {
            return false;
        }
        PropertyEvaluatorSimple other = (PropertyEvaluatorSimple) otherEval;
        if (!other.getExpressionText().equals(this.getExpressionText())) {
            return false;
        }
        if ((other.getFilter() == null) && (this.getFilter() == null)) {
            return true;
        }
        return false;
    }
}

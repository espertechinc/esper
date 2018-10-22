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
package com.espertech.esper.common.internal.epl.contained;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.common.internal.epl.expression.core.ExprNodeUtilityEvaluate;
import com.espertech.esper.common.internal.filterspec.PropertyEvaluator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Property evaluator that considers only level one and considers a where-clause,
 * but does not consider a select clause or N-level.
 */
public class PropertyEvaluatorSimple implements PropertyEvaluator {
    private static final Logger log = LoggerFactory.getLogger(PropertyEvaluatorSimple.class);
    private ContainedEventEval containedEventEval;
    private ExprEvaluator filter;
    private boolean fragmentIsIndexed;
    private EventType eventType;
    private String expressionText;

    public void setContainedEventEval(ContainedEventEval containedEventEval) {
        this.containedEventEval = containedEventEval;
    }

    public void setFilter(ExprEvaluator filter) {
        this.filter = filter;
    }

    public void setFragmentIsIndexed(boolean fragmentIsIndexed) {
        this.fragmentIsIndexed = fragmentIsIndexed;
    }

    public void setExpressionText(String expressionText) {
        this.expressionText = expressionText;
    }

    public void setEventType(EventType eventType) {
        this.eventType = eventType;
    }

    public EventBean[] getProperty(EventBean theEvent, ExprEvaluatorContext exprEvaluatorContext) {
        try {
            Object result = containedEventEval.getFragment(theEvent, new EventBean[]{theEvent}, exprEvaluatorContext);

            EventBean[] rows;
            if (fragmentIsIndexed) {
                rows = (EventBean[]) result;
            } else {
                rows = new EventBean[]{(EventBean) result};
            }

            if (filter == null) {
                return rows;
            }
            return ExprNodeUtilityEvaluate.applyFilterExpression(filter, theEvent, (EventBean[]) result, exprEvaluatorContext);
        } catch (RuntimeException ex) {
            log.error("Unexpected error evaluating property expression for event of type '" +
                    theEvent.getEventType().getName() +
                    "' and property '" +
                    expressionText + "': " + ex.getMessage(), ex);
        }
        return null;
    }

    public EventType getFragmentEventType() {
        return eventType;
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

    public ExprEvaluator getFilter() {
        return filter;
    }

    public String getExpressionText() {
        return expressionText;
    }
}

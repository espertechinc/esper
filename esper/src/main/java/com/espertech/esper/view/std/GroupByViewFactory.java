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
package com.espertech.esper.view.std;

import com.espertech.esper.client.EventType;
import com.espertech.esper.client.annotation.Hint;
import com.espertech.esper.client.annotation.HintEnum;
import com.espertech.esper.core.context.util.AgentInstanceContext;
import com.espertech.esper.core.context.util.AgentInstanceViewFactoryChainContext;
import com.espertech.esper.core.service.StatementContext;
import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.epl.expression.core.ExprNodeUtility;
import com.espertech.esper.view.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Factory for {@link GroupByView} instances.
 */
public class GroupByViewFactory implements ViewFactory, GroupByViewFactoryMarker {
    private static Logger log = LoggerFactory.getLogger(GroupByViewFactory.class);

    /**
     * View parameters.
     */
    protected List<ExprNode> viewParameters;

    /**
     * List of criteria expressions.
     */
    protected ExprNode[] criteriaExpressions;

    private EventType eventType;

    protected boolean isReclaimAged;
    protected double reclaimMaxAge;
    protected double reclaimFrequency;

    public void setViewParameters(ViewFactoryContext viewFactoryContext, List<ExprNode> expressionParameters) throws ViewParameterException {
        this.viewParameters = expressionParameters;

        Hint reclaimGroupAged = HintEnum.RECLAIM_GROUP_AGED.getHint(viewFactoryContext.getStatementContext().getAnnotations());

        if (reclaimGroupAged != null) {
            isReclaimAged = true;
            String hintValueMaxAge = HintEnum.RECLAIM_GROUP_AGED.getHintAssignedValue(reclaimGroupAged);
            if (hintValueMaxAge == null) {
                throw new ViewParameterException("Required hint value for hint '" + HintEnum.RECLAIM_GROUP_AGED + "' has not been provided");
            }
            try {
                reclaimMaxAge = Double.parseDouble(hintValueMaxAge);
            } catch (RuntimeException ex) {
                throw new ViewParameterException("Required hint value for hint '" + HintEnum.RECLAIM_GROUP_AGED + "' value '" + hintValueMaxAge + "' could not be parsed as a double value");
            }

            String hintValueFrequency = HintEnum.RECLAIM_GROUP_FREQ.getHintAssignedValue(reclaimGroupAged);
            if (hintValueFrequency == null) {
                reclaimFrequency = reclaimMaxAge;
            } else {
                try {
                    reclaimFrequency = Double.parseDouble(hintValueFrequency);
                } catch (RuntimeException ex) {
                    throw new ViewParameterException("Required hint value for hint '" + HintEnum.RECLAIM_GROUP_FREQ + "' value '" + hintValueFrequency + "' could not be parsed as a double value");
                }
            }
            if (reclaimMaxAge < 0.100) {
                log.warn("Reclaim max age parameter is less then 100 milliseconds, are your sure?");
            }

            if (log.isDebugEnabled()) {
                log.debug("Using reclaim-aged strategy for group-window age " + reclaimMaxAge + " frequency " + reclaimFrequency);
            }
        }
    }

    public void attach(EventType parentEventType, StatementContext statementContext, ViewFactory optionalParentFactory, List<ViewFactory> parentViewFactories) throws ViewParameterException {
        criteriaExpressions = ViewFactorySupport.validate(getViewName(), parentEventType, statementContext, viewParameters, false);

        if (criteriaExpressions.length == 0) {
            String errorMessage = getViewName() + " view requires a one or more expressions provinding unique values as parameters";
            throw new ViewParameterException(errorMessage);
        }

        this.eventType = parentEventType;
    }

    /**
     * Returns the names of fields to group by
     *
     * @return field names
     */
    public ExprNode[] getCriteriaExpressions() {
        return criteriaExpressions;
    }

    public View makeView(AgentInstanceViewFactoryChainContext agentInstanceViewFactoryContext) {
        if (isReclaimAged) {
            return new GroupByViewReclaimAged(agentInstanceViewFactoryContext, criteriaExpressions, ExprNodeUtility.getEvaluators(criteriaExpressions), reclaimMaxAge, reclaimFrequency);
        }
        return new GroupByViewImpl(agentInstanceViewFactoryContext, criteriaExpressions, ExprNodeUtility.getEvaluators(criteriaExpressions));
    }

    public EventType getEventType() {
        return eventType;
    }

    public boolean canReuse(View view, AgentInstanceContext agentInstanceContext) {
        if (!(view instanceof GroupByView)) {
            return false;
        }

        if (isReclaimAged) {
            return false;
        }

        GroupByView myView = (GroupByView) view;
        if (!ExprNodeUtility.deepEquals(myView.getCriteriaExpressions(), criteriaExpressions)) {
            return false;
        }

        return true;
    }

    public boolean isReclaimAged() {
        return isReclaimAged;
    }

    public double getReclaimMaxAge() {
        return reclaimMaxAge;
    }

    public double getReclaimFrequency() {
        return reclaimFrequency;
    }

    public String getViewName() {
        return "Group-By";
    }
}

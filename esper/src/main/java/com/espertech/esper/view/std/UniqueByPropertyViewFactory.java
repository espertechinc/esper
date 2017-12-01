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
import com.espertech.esper.core.context.util.AgentInstanceContext;
import com.espertech.esper.core.context.util.AgentInstanceViewFactoryChainContext;
import com.espertech.esper.core.service.StatementContext;
import com.espertech.esper.epl.expression.core.ExprNodeUtilityCore;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.epl.util.ExprNodeUtilityRich;
import com.espertech.esper.view.*;

import java.util.List;
import java.util.Set;

/**
 * Factory for {@link UniqueByPropertyView} instances.
 */
public class UniqueByPropertyViewFactory implements DataWindowViewFactoryUniqueCandidate, DataWindowViewFactory {
    public final static String NAME = "Unique-By";

    /**
     * View parameters.
     */
    protected List<ExprNode> viewParameters;

    /**
     * Property name to evaluate unique values.
     */
    protected ExprNode[] criteriaExpressions;
    protected ExprEvaluator[] criteriaExpressionsEvals;
    private EventType eventType;

    public void setViewParameters(ViewFactoryContext viewFactoryContext, List<ExprNode> expressionParameters) throws ViewParameterException {
        this.viewParameters = expressionParameters;
    }

    public void attach(EventType parentEventType, StatementContext statementContext, ViewFactory optionalParentFactory, List<ViewFactory> parentViewFactories) throws ViewParameterException {
        criteriaExpressions = ViewFactorySupport.validate(getViewName(), parentEventType, statementContext, viewParameters, false);

        if (criteriaExpressions.length == 0) {
            String errorMessage = getViewName() + " view requires a one or more expressions providing unique values as parameters";
            throw new ViewParameterException(errorMessage);
        }

        this.eventType = parentEventType;
        this.criteriaExpressionsEvals = ExprNodeUtilityRich.getEvaluatorsMayCompile(criteriaExpressions, statementContext.getEngineImportService(), this.getClass(), false, statementContext.getStatementName());
    }

    public View makeView(AgentInstanceViewFactoryChainContext agentInstanceViewFactoryContext) {
        return new UniqueByPropertyView(this, agentInstanceViewFactoryContext);
    }

    public EventType getEventType() {
        return eventType;
    }

    public boolean canReuse(View view, AgentInstanceContext agentInstanceContext) {
        if (!(view instanceof UniqueByPropertyView)) {
            return false;
        }

        UniqueByPropertyView myView = (UniqueByPropertyView) view;
        if (!ExprNodeUtilityCore.deepEquals(criteriaExpressions, myView.getCriteriaExpressions(), false)) {
            return false;
        }

        return myView.isEmpty();
    }

    public Set<String> getUniquenessCandidatePropertyNames() {
        return ExprNodeUtilityCore.getPropertyNamesIfAllProps(criteriaExpressions);
    }

    public String getViewName() {
        return NAME;
    }

    public ExprNode[] getCriteriaExpressions() {
        return criteriaExpressions;
    }

    public ExprEvaluator[] getCriteriaExpressionsEvals() {
        return criteriaExpressionsEvals;
    }
}

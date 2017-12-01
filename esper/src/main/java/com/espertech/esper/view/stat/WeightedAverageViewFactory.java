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
package com.espertech.esper.view.stat;

import com.espertech.esper.client.EventType;
import com.espertech.esper.core.context.util.AgentInstanceContext;
import com.espertech.esper.core.context.util.AgentInstanceViewFactoryChainContext;
import com.espertech.esper.core.service.StatementContext;
import com.espertech.esper.epl.expression.codegen.ExprNodeCompiler;
import com.espertech.esper.epl.expression.core.ExprNodeUtilityCore;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.util.JavaClassHelper;
import com.espertech.esper.view.*;

import java.util.List;

/**
 * Factory for {@link WeightedAverageView} instances.
 */
public class WeightedAverageViewFactory implements ViewFactory {
    protected final static String NAME = "Weighted-average";

    private List<ExprNode> viewParameters;
    private int streamNumber;

    protected ExprNode fieldNameX;
    protected ExprNode fieldNameWeight;
    protected ExprEvaluator fieldNameXEvaluator;
    protected ExprEvaluator fieldNameWeightEvaluator;

    protected StatViewAdditionalProps additionalProps;

    protected EventType eventType;

    public void setViewParameters(ViewFactoryContext viewFactoryContext, List<ExprNode> expressionParameters) throws ViewParameterException {
        this.viewParameters = expressionParameters;
        this.streamNumber = viewFactoryContext.getStreamNum();
    }

    public void attach(EventType parentEventType, StatementContext statementContext, ViewFactory optionalParentFactory, List<ViewFactory> parentViewFactories) throws ViewParameterException {
        ExprNode[] validated = ViewFactorySupport.validate(getViewName(), parentEventType, statementContext, viewParameters, true);

        if (validated.length < 2) {
            throw new ViewParameterException(getViewParamMessage());
        }
        if ((!JavaClassHelper.isNumeric(validated[0].getForge().getEvaluationType())) || (!JavaClassHelper.isNumeric(validated[1].getForge().getEvaluationType()))) {
            throw new ViewParameterException(getViewParamMessage());
        }

        fieldNameX = validated[0];
        fieldNameWeight = validated[1];
        fieldNameXEvaluator = ExprNodeCompiler.allocateEvaluator(fieldNameX.getForge(), statementContext.getEngineImportService(), WeightedAverageView.class, false, statementContext.getStatementName());
        fieldNameWeightEvaluator = ExprNodeCompiler.allocateEvaluator(fieldNameWeight.getForge(), statementContext.getEngineImportService(), WeightedAverageView.class, false, statementContext.getStatementName());
        additionalProps = StatViewAdditionalProps.make(validated, 2, parentEventType, statementContext.getEngineImportService(), statementContext.getStatementName());
        eventType = WeightedAverageView.createEventType(statementContext, additionalProps, streamNumber);
    }

    public View makeView(AgentInstanceViewFactoryChainContext agentInstanceViewFactoryContext) {
        return new WeightedAverageView(this, agentInstanceViewFactoryContext);
    }

    public EventType getEventType() {
        return eventType;
    }

    public boolean canReuse(View view, AgentInstanceContext agentInstanceContext) {
        if (!(view instanceof WeightedAverageView)) {
            return false;
        }
        if (additionalProps != null) {
            return false;
        }

        WeightedAverageView myView = (WeightedAverageView) view;
        if ((!ExprNodeUtilityCore.deepEquals(fieldNameWeight, myView.getFieldNameWeight(), false)) ||
                (!ExprNodeUtilityCore.deepEquals(fieldNameX, myView.getFieldNameX(), false))) {
            return false;
        }
        return true;
    }

    public String getViewName() {
        return NAME;
    }

    private String getViewParamMessage() {
        return getViewName() + " view requires two expressions returning numeric values as parameters";
    }

    public void setFieldNameX(ExprNode fieldNameX) {
        this.fieldNameX = fieldNameX;
    }

    public void setFieldNameWeight(ExprNode fieldNameWeight) {
        this.fieldNameWeight = fieldNameWeight;
    }

    public void setAdditionalProps(StatViewAdditionalProps additionalProps) {
        this.additionalProps = additionalProps;
    }

    public StatViewAdditionalProps getAdditionalProps() {
        return additionalProps;
    }

    public void setEventType(EventType eventType) {
        this.eventType = eventType;
    }
}

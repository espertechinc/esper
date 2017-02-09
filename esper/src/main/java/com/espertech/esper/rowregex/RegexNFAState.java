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
package com.espertech.esper.rowregex;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;

import java.util.List;

/**
 * Match-recognize NFA states provides this information.
 */
public interface RegexNFAState {
    /**
     * For multiple-quantifiers.
     *
     * @return indicator
     */
    public boolean isMultiple();

    /**
     * Returns the nested node number.
     *
     * @return num
     */
    public String getNodeNumNested();

    /**
     * Returns the absolute node num.
     *
     * @return num
     */
    public int getNodeNumFlat();

    /**
     * Returns the variable name.
     *
     * @return name
     */
    public String getVariableName();

    /**
     * Returns stream number.
     *
     * @return stream num
     */
    public int getStreamNum();

    /**
     * Returns greedy indicator.
     *
     * @return greedy indicator
     */
    public Boolean isGreedy();

    /**
     * Evaluate a match.
     *
     * @param eventsPerStream      variabele values
     * @param exprEvaluatorContext expression evaluation context
     * @return match indicator
     */
    public boolean matches(EventBean[] eventsPerStream, ExprEvaluatorContext exprEvaluatorContext);

    /**
     * Returns the next states.
     *
     * @return states
     */
    public List<RegexNFAState> getNextStates();

    /**
     * Whether or not the match-expression requires multimatch state
     *
     * @return indicator
     */
    public boolean isExprRequiresMultimatchState();
}

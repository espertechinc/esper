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
package com.espertech.esper.epl.expression.core;

/**
 * Represents an stream property identifier in a filter expressiun tree.
 */
public interface ExprIdentNode extends ExprNode, ExprFilterOptimizableNode, ExprStreamRefNode {
    public String getUnresolvedPropertyName();

    public String getFullUnresolvedName();

    public int getStreamId();

    public String getResolvedPropertyNameRoot();

    public String getResolvedPropertyName();

    public String getStreamOrPropertyName();

    public void setStreamOrPropertyName(String streamOrPropertyName);

    public String getResolvedStreamName();

    public ExprIdentNodeEvaluator getExprEvaluatorIdent();
}

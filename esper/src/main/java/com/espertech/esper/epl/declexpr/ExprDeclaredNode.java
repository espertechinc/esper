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
package com.espertech.esper.epl.declexpr;

import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.epl.expression.core.ExprValidationException;
import com.espertech.esper.epl.spec.ExpressionDeclItem;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Expression instance as declared elsewhere.
 * <p>
 * (1) Statement parse: Expression tree from expression body gets deep-copied.
 * (2) Statement create (lifecyle event): Subselect visitor compiles Subselect-list
 * (3) Statement start:
 * a) event types of each stream determined
 * b) subselects filter expressions get validated and subselect started
 * (4) Remaining expressions get validated
 */
public interface ExprDeclaredNode extends ExprNode {
    public List<ExprNode> getChainParameters();

    public ExpressionDeclItem getPrototype();

    public LinkedHashMap<String, Integer> getOuterStreamNames(Map<String, Integer> outerStreamNames) throws ExprValidationException;

    public ExprNode getBody();
}
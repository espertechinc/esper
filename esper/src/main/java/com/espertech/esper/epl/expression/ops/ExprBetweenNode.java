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
package com.espertech.esper.epl.expression.ops;

import com.espertech.esper.epl.expression.core.ExprNode;

/**
 * Represents the between-clause function in an expression tree.
 */
public interface ExprBetweenNode extends ExprNode {
    /**
     * Returns true if the low endpoint is included, false if not
     *
     * @return indicator if endppoint is included
     */
    public boolean isLowEndpointIncluded();

    /**
     * Returns true if the high endpoint is included, false if not
     *
     * @return indicator if endppoint is included
     */
    public boolean isHighEndpointIncluded();

    /**
     * Returns true for inverted range, or false for regular (openn/close/half-open/half-closed) ranges.
     *
     * @return true for not betwene, false for between
     */
    public boolean isNotBetween();
}

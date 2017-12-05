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
package com.espertech.esper.epl.spec;

import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.util.StringValue;

/**
 * Represents a single item in a SELECT-clause, potentially unnamed
 * as no "as" tag may have been supplied in the syntax.
 * <p>
 * Compare to {@link SelectClauseExprCompiledSpec} which carries a determined name.
 */
public class SelectClauseExprRawSpec implements SelectClauseElementRaw {
    private ExprNode selectExpression;
    private String optionalAsName;
    private boolean isEvents;
    private static final long serialVersionUID = 2613265291858800221L;

    /**
     * Ctor.
     *
     * @param selectExpression - the expression node to evaluate for matching events
     * @param optionalAsName   - the name of the item, null if not name supplied
     * @param isEvents         - whether event selected
     */
    public SelectClauseExprRawSpec(ExprNode selectExpression, String optionalAsName, boolean isEvents) {
        this.selectExpression = selectExpression;
        this.optionalAsName = optionalAsName == null ? null : StringValue.removeTicks(optionalAsName);
        this.isEvents = isEvents;
    }

    /**
     * Returns the expression node representing the item in the select clause.
     *
     * @return expression node for item
     */
    public ExprNode getSelectExpression() {
        return selectExpression;
    }

    /**
     * Returns the name of the item in the select clause.
     *
     * @return name of item
     */
    public String getOptionalAsName() {
        return optionalAsName;
    }

    public boolean isEvents() {
        return isEvents;
    }
}

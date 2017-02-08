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

/**
 * Encapsulates the parsed select expressions in a select-clause in an EPL statement.
 */
public class SelectClauseSpecCompiled {
    private final static SelectClauseElementCompiled[] EMPTY = new SelectClauseElementCompiled[0];

    private final boolean isDistinct;
    private SelectClauseElementCompiled[] selectClauseElements;

    /**
     * Ctor.
     *
     * @param isDistinct indicates distinct or not
     */
    public SelectClauseSpecCompiled(boolean isDistinct) {
        selectClauseElements = EMPTY;
        this.isDistinct = isDistinct;
    }

    /**
     * Ctor.
     *
     * @param selectList for a populates list of select expressions
     * @param isDistinct indicates distinct or not
     */
    public SelectClauseSpecCompiled(SelectClauseElementCompiled[] selectList, boolean isDistinct) {
        this.selectClauseElements = selectList;
        this.isDistinct = isDistinct;
    }

    public void setSelectExprList(SelectClauseElementWildcard selectClauseElement) {
        selectClauseElements = new SelectClauseElementWildcard[]{selectClauseElement};
    }

    /**
     * Returns the list of select expressions.
     *
     * @return list of expressions
     */
    public SelectClauseElementCompiled[] getSelectExprList() {
        return selectClauseElements;
    }

    /**
     * Returns true if the select clause contains at least one wildcard.
     *
     * @return true if clause contains wildcard, false if not
     */
    public boolean isUsingWildcard() {
        for (SelectClauseElementCompiled element : selectClauseElements) {
            if (element instanceof SelectClauseElementWildcard) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns indictor whether distinct or not.
     *
     * @return distinct indicator
     */
    public boolean isDistinct() {
        return isDistinct;
    }
}

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
package com.espertech.esper.client.soda;

/**
 * Fire-and-forget (on-demand) insert DML.
 * <p>
 * The insert-into clause holds the named window name and column names.
 * The select-clause list holds the values to be inserted.
 * </p>
 */
public class FireAndForgetInsert implements FireAndForgetClause {
    private static final long serialVersionUID = -3565886245820109541L;

    private boolean useValuesKeyword = true;

    /**
     * Ctor.
     *
     * @param useValuesKeyword whether to use the "values" keyword or whether the syntax is based on select
     */
    public FireAndForgetInsert(boolean useValuesKeyword) {
        this.useValuesKeyword = useValuesKeyword;
    }

    /**
     * Ctor.
     */
    public FireAndForgetInsert() {
    }

    /**
     * Returns indicator whether to use the values keyword.
     *
     * @return indicator
     */
    public boolean isUseValuesKeyword() {
        return useValuesKeyword;
    }

    /**
     * Sets indicator whether to use the values keyword.
     *
     * @param useValuesKeyword indicator
     */
    public void setUseValuesKeyword(boolean useValuesKeyword) {
        this.useValuesKeyword = useValuesKeyword;
    }
}

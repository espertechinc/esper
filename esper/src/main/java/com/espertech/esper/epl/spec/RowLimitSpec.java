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

import java.io.Serializable;

/**
 * Spec for defining a row limit.
 */
public class RowLimitSpec implements Serializable {
    private final Integer numRows;
    private final Integer optionalOffset;

    private final String numRowsVariable;
    private final String optionalOffsetVariable;
    private static final long serialVersionUID = 584331495871950474L;

    /**
     * Ctor.
     *
     * @param numRows                max num rows constant, null if using variable
     * @param optionalOffset         offset or null
     * @param numRowsVariable        max num rows variable, null if using constant
     * @param optionalOffsetVariable offset variable or null
     */
    public RowLimitSpec(Integer numRows, Integer optionalOffset, String numRowsVariable, String optionalOffsetVariable) {
        this.numRows = numRows;
        this.optionalOffset = optionalOffset;
        this.numRowsVariable = numRowsVariable;
        this.optionalOffsetVariable = optionalOffsetVariable;
    }

    /**
     * Returns max num rows constant or null if using variable.
     *
     * @return limit
     */
    public Integer getNumRows() {
        return numRows;
    }

    /**
     * Returns offset constant or null.
     *
     * @return offset
     */
    public Integer getOptionalOffset() {
        return optionalOffset;
    }

    /**
     * Returns max num rows variable or null if using constant.
     *
     * @return limit
     */
    public String getNumRowsVariable() {
        return numRowsVariable;
    }

    /**
     * Returns offset variable or null
     *
     * @return offset variable
     */
    public String getOptionalOffsetVariable() {
        return optionalOffsetVariable;
    }
}

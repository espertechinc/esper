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
package com.espertech.esper.client;

/**
 * Parameter holder for parameterized on-demand queries that are prepared with substitution parameters and that
 * can be executed efficiently multiple times with different actual values for parameters.
 * <p>
 * A pre-compiled query can only be executed when actual values for all
 * substitution parameters are set.
 */
public interface EPOnDemandPreparedQueryParameterized {
    /**
     * Sets the value of the designated parameter using the given object.
     *
     * @param parameterIndex the first parameter is 1, the second is 2, ...
     * @param value          the object containing the input parameter value
     * @throws EPException if the substitution parameter could not be located
     */
    public void setObject(int parameterIndex, Object value) throws EPException;
}


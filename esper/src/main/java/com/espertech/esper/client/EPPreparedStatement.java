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
 * Precompiled statement that is prepared with substitution parameters and that
 * can be created and started efficiently multiple times with different actual values for parameters.
 * <p>
 * When a precompiled statement is prepared via the prepare method on {@link EPAdministrator},
 * it typically has one or more substitution parameters in the statement text,
 * for which the placeholder character is the question mark. This class provides methods to set
 * the actual value for the substitution parameter.
 * <p>
 * A precompiled statement can only be created and started when actual values for all
 * substitution parameters are set.
 */
public interface EPPreparedStatement {
    /**
     * Sets the value of the designated parameter using the given object.
     *
     * @param parameterIndex the first parameter is 1, the second is 2, ...
     * @param value          the object containing the input parameter value
     * @throws EPException if the substitution parameter could not be located
     */
    public void setObject(int parameterIndex, Object value) throws EPException;

    /**
     * Sets the value of the designated parameter using the given object.
     *
     * @param parameterName the name of the parameter
     * @param value         the object containing the input parameter value
     * @throws EPException if the substitution parameter could not be located
     */
    public void setObject(String parameterName, Object value) throws EPException;
}

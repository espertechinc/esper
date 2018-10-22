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
package com.espertech.esper.runtime.client.option;

/**
 * Implement this interface to provide values for substitution parameters.
 */
public interface StatementSubstitutionParameterOption {
    /**
     * Set statement substitution parameters.
     *
     * @param env provides the setObject method and provides information about the statement
     */
    void setStatementParameters(StatementSubstitutionParameterContext env);
}

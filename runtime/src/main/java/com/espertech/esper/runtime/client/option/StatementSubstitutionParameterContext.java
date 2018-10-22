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

import com.espertech.esper.common.client.EPException;

import java.lang.annotation.Annotation;
import java.util.Map;

/**
 * Provides the environment to {@link StatementSubstitutionParameterOption}.
 */
public interface StatementSubstitutionParameterContext {

    /**
     * Returns the deployment id
     *
     * @return deployment id
     */
    String getDeploymentId();

    /**
     * Returns the statement name
     *
     * @return statement name
     */
    String getStatementName();

    /**
     * Returns the statement id
     *
     * @return statement id
     */
    int getStatementId();

    /**
     * Returns the EPL when provided or null when not provided
     *
     * @return epl
     */
    String getEpl();

    /**
     * Returns the annotations
     *
     * @return annotations
     */
    Annotation[] getAnnotations();

    /**
     * Returns the parameter types
     *
     * @return types
     */
    Class[] getSubstitutionParameterTypes();

    /**
     * Returns the parameter names
     *
     * @return names
     */
    Map<String, Integer> getSubstitutionParameterNames();

    /**
     * Sets the value of the designated parameter using the given object.
     *
     * @param parameterIndex the first parameter is 1, the second is 2, ...
     * @param value          the object containing the input parameter value
     * @throws EPException if the substitution parameter could not be set
     */
    public void setObject(int parameterIndex, Object value) throws EPException;

    /**
     * Sets the value of the designated parameter using the given object.
     *
     * @param parameterName the name of the parameter
     * @param value         the object containing the input parameter value
     * @throws EPException if the substitution parameter could not be set
     */
    public void setObject(String parameterName, Object value) throws EPException;
}

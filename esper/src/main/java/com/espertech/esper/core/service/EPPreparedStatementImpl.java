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
package com.espertech.esper.core.service;

import com.espertech.esper.client.EPException;
import com.espertech.esper.client.EPOnDemandPreparedQueryParameterized;
import com.espertech.esper.client.EPPreparedStatement;
import com.espertech.esper.client.soda.EPStatementObjectModel;
import com.espertech.esper.epl.spec.SubstitutionParameterExpressionBase;
import com.espertech.esper.epl.spec.SubstitutionParameterExpressionIndexed;
import com.espertech.esper.epl.spec.SubstitutionParameterExpressionNamed;

import java.io.Serializable;
import java.util.List;

/**
 * Prepared statement implementation that stores the statement object model and
 * a list of substitution parameters, to be mapped into an internal representation upon
 * creation.
 */
public class EPPreparedStatementImpl implements EPPreparedStatement, EPOnDemandPreparedQueryParameterized, Serializable {
    private static final long serialVersionUID = 821297634350548600L;
    private final EPStatementObjectModel model;
    private final List<SubstitutionParameterExpressionBase> subParams;
    private final String optionalEPL;
    private boolean initialized;

    /**
     * Ctor.
     *
     * @param model       is the statement object model
     * @param subParams   is the substitution parameter list
     * @param optionalEPL the EPL provided if any
     */
    public EPPreparedStatementImpl(EPStatementObjectModel model, List<SubstitutionParameterExpressionBase> subParams, String optionalEPL) {
        this.model = model;
        this.subParams = subParams;
        this.optionalEPL = optionalEPL;
    }

    public void setObject(String parameterName, Object value) throws EPException {
        validateNonEmpty();
        if (subParams.get(0) instanceof SubstitutionParameterExpressionIndexed) {
            throw new IllegalArgumentException("Substitution parameters are unnamed, please use setObject(index,...) instead");
        }
        boolean found = false;
        for (SubstitutionParameterExpressionBase subs : subParams) {
            if (((SubstitutionParameterExpressionNamed) subs).getName().equals(parameterName)) {
                found = true;
                subs.setConstant(value);
            }
        }
        if (!found) {
            throw new IllegalArgumentException("Invalid substitution parameter name of '" + parameterName + "' supplied, failed to find the name");
        }
    }

    public void setObject(int parameterIndex, Object value) throws EPException {
        validateNonEmpty();
        if (subParams.get(0) instanceof SubstitutionParameterExpressionNamed) {
            throw new IllegalArgumentException("Substitution parameters are named, please use setObject(name,...) instead");
        }
        if (parameterIndex < 1) {
            throw new IllegalArgumentException("Substitution parameter index starts at 1");
        }
        boolean found = false;
        for (SubstitutionParameterExpressionBase subs : subParams) {
            if (((SubstitutionParameterExpressionIndexed) subs).getIndex() == parameterIndex) {
                found = true;
                subs.setConstant(value);
            }
        }
        if (!found) {
            throw new IllegalArgumentException("Invalid substitution parameter index of " + parameterIndex + " supplied, the maximum for this statement is " + subParams.size());
        }
    }

    /**
     * Returns the statement object model for the prepared statement
     *
     * @return object model
     */
    public EPStatementObjectModel getModel() {
        return model;
    }

    public String getOptionalEPL() {
        return optionalEPL;
    }

    private void validateNonEmpty() {
        if (subParams.size() == 0) {
            throw new IllegalArgumentException("Statement does not have substitution parameters indicated by the '?' character");
        }
    }
}

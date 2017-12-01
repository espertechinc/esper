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
package com.espertech.esper.epl.specmapper;

import com.espertech.esper.client.soda.EPStatementObjectModel;
import com.espertech.esper.epl.spec.SubstitutionParameterExpressionBase;

import java.util.List;

/**
 * Return result for unmap operators unmapping an intermal statement representation to the SODA object model.
 */
public class StatementSpecUnMapResult {
    private final EPStatementObjectModel objectModel;
    private final List<SubstitutionParameterExpressionBase> substitutionParams;

    /**
     * Ctor.
     *
     * @param objectModel        of the statement
     * @param substitutionParams a map of parameter index and parameter
     */
    public StatementSpecUnMapResult(EPStatementObjectModel objectModel, List<SubstitutionParameterExpressionBase> substitutionParams) {
        this.objectModel = objectModel;
        this.substitutionParams = substitutionParams;
    }

    /**
     * Returns the object model.
     *
     * @return object model
     */
    public EPStatementObjectModel getObjectModel() {
        return objectModel;
    }

    /**
     * Returns the substitution paremeters keyed by the parameter's index.
     *
     * @return map of index and parameter
     */
    public List<SubstitutionParameterExpressionBase> getSubstitutionParams() {
        return substitutionParams;
    }
}

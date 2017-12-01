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

import com.espertech.esper.epl.spec.SubstitutionParameterExpressionBase;

import java.util.ArrayList;
import java.util.List;

/**
 * Un-mapping context for mapping from an internal specifications to an SODA object model.
 */
public class StatementSpecUnMapContext {
    private final List<SubstitutionParameterExpressionBase> substitutionParams;

    public StatementSpecUnMapContext() {
        substitutionParams = new ArrayList<SubstitutionParameterExpressionBase>();
    }

    public void add(SubstitutionParameterExpressionBase subsParam) {
        substitutionParams.add(subsParam);
    }

    public List<SubstitutionParameterExpressionBase> getSubstitutionParams() {
        return substitutionParams;
    }

    public void addAll(List<SubstitutionParameterExpressionBase> inner) {
        substitutionParams.addAll(inner);
    }
}

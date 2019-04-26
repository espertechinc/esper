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
package com.espertech.esper.common.client.serde;

import com.espertech.esper.common.internal.compile.stage2.StatementRawInfo;

/**
 * Information about the variable for which to obtain a serde.
 */
public class SerdeProviderAdditionalInfoVariable extends SerdeProviderAdditionalInfo {
    private final String variableName;

    /**
     * Ctor.
     * @param raw statement information
     * @param variableName variable name
     */
    public SerdeProviderAdditionalInfoVariable(StatementRawInfo raw, String variableName) {
        super(raw);
        this.variableName = variableName;
    }

    /**
     * Returns the variable name
     *
     * @return name
     */
    public String getVariableName() {
        return variableName;
    }

    public String toString() {
        return "variable '" + variableName + "'";
    }
}

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
package com.espertech.esper.common.internal.epl.variable.core;

import com.espertech.esper.common.internal.compile.stage3.StmtClassForgeableFactory;
import com.espertech.esper.common.internal.epl.variable.compiletime.VariableMetaData;

import java.util.List;

public class VariableMetadataWithForgables {
    private final VariableMetaData variableMetaData;
    private final List<StmtClassForgeableFactory> forgables;

    public VariableMetadataWithForgables(VariableMetaData variableMetaData, List<StmtClassForgeableFactory> forgables) {
        this.variableMetaData = variableMetaData;
        this.forgables = forgables;
    }

    public VariableMetaData getVariableMetaData() {
        return variableMetaData;
    }

    public List<StmtClassForgeableFactory> getForgables() {
        return forgables;
    }
}

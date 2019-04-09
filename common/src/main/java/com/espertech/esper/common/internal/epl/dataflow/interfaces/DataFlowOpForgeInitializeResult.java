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
package com.espertech.esper.common.internal.epl.dataflow.interfaces;

import com.espertech.esper.common.internal.compile.stage3.StmtForgeMethodResult;
import com.espertech.esper.common.internal.epl.dataflow.util.GraphTypeDesc;

public class DataFlowOpForgeInitializeResult {

    private GraphTypeDesc[] typeDescriptors;
    private StmtForgeMethodResult additionalForgeables;

    public DataFlowOpForgeInitializeResult() {
    }

    public DataFlowOpForgeInitializeResult(GraphTypeDesc[] typeDescriptors) {
        this.typeDescriptors = typeDescriptors;
    }

    public GraphTypeDesc[] getTypeDescriptors() {
        return typeDescriptors;
    }

    public void setTypeDescriptors(GraphTypeDesc[] typeDescriptors) {
        this.typeDescriptors = typeDescriptors;
    }

    public StmtForgeMethodResult getAdditionalForgeables() {
        return additionalForgeables;
    }

    public void setAdditionalForgeables(StmtForgeMethodResult additionalForgeables) {
        this.additionalForgeables = additionalForgeables;
    }
}

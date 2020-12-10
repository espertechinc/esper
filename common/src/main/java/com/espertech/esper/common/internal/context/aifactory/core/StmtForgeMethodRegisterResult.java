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
package com.espertech.esper.common.internal.context.aifactory.core;

import com.espertech.esper.common.internal.fabric.FabricCharge;

public class StmtForgeMethodRegisterResult {

    private final String objectName;
    private final FabricCharge fabricCharge;

    public StmtForgeMethodRegisterResult(String objectName, FabricCharge fabricCharge) {
        this.objectName = objectName;
        this.fabricCharge = fabricCharge;
    }

    public String getObjectName() {
        return objectName;
    }

    public FabricCharge getFabricCharge() {
        return fabricCharge;
    }
}

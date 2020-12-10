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
package com.espertech.esper.common.internal.epl.output.condition;

import com.espertech.esper.common.internal.fabric.FabricCharge;

public class OutputConditionFactoryForgeResult {
    private final OutputConditionFactoryForge forge;
    private final FabricCharge fabricCharge;

    public OutputConditionFactoryForgeResult(OutputConditionFactoryForge forge, FabricCharge fabricCharge) {
        this.forge = forge;
        this.fabricCharge = fabricCharge;
    }

    public OutputConditionFactoryForge getForge() {
        return forge;
    }

    public FabricCharge getFabricCharge() {
        return fabricCharge;
    }
}

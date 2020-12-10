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
package com.espertech.esper.common.internal.fabric;

public class FabricChargeNonHA implements FabricCharge {
    public final static FabricChargeNonHA INSTANCE = new FabricChargeNonHA();

    private FabricChargeNonHA() {
    }

    public void add(FabricCharge fabricCharge) {
        // no action
    }
}

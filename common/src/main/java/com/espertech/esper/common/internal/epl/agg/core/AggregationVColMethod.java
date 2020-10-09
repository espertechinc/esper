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
package com.espertech.esper.common.internal.epl.agg.core;

class AggregationVColMethod {
    private final int vcol;
    private final AggregationForgeFactory forge;

    public AggregationVColMethod(int vcol, AggregationForgeFactory forge) {
        this.vcol = vcol;
        this.forge = forge;
    }

    public int getVcol() {
        return vcol;
    }

    public AggregationForgeFactory getForge() {
        return forge;
    }
}

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
package com.espertech.esper.regressionlib.support.filter;

import java.util.Arrays;

public class SupportFilterPlanPath {
    private SupportFilterPlanTriplet[] triplets;
    private String controlNegate;

    public SupportFilterPlanPath(SupportFilterPlanTriplet... triplets) {
        this.triplets = triplets;
    }

    public SupportFilterPlanPath(String controlNegate, SupportFilterPlanTriplet... triplets) {
        this.triplets = triplets;
        this.controlNegate = controlNegate;
    }

    public SupportFilterPlanTriplet[] getTriplets() {
        return triplets;
    }

    public void setTriplets(SupportFilterPlanTriplet[] triplets) {
        this.triplets = triplets;
    }

    public String getControlNegate() {
        return controlNegate;
    }

    public void setControlNegate(String controlNegate) {
        this.controlNegate = controlNegate;
    }

    public String toString() {
        return "SupportFilterPlanPath{" +
            "triplets=" + Arrays.toString(triplets) +
            ", controlNegate='" + controlNegate + '\'' +
            '}';
    }
}

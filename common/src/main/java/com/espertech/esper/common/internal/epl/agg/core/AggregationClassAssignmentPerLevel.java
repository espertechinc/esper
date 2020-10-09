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

class AggregationClassAssignmentPerLevel {
    private final AggregationClassAssignment[] optionalTop;
    private final AggregationClassAssignment[][] optionalPerLevel;

    public AggregationClassAssignmentPerLevel(AggregationClassAssignment[] optionalTop, AggregationClassAssignment[][] optionalPerLevel) {
        this.optionalTop = optionalTop;
        this.optionalPerLevel = optionalPerLevel;
    }

    public AggregationClassAssignment[] getOptionalTop() {
        return optionalTop;
    }

    public AggregationClassAssignment[][] getOptionalPerLevel() {
        return optionalPerLevel;
    }
}

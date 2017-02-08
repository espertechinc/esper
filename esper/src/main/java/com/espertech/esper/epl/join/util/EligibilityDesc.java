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
package com.espertech.esper.epl.join.util;

public class EligibilityDesc {
    private Eligibility eligibility;
    private Integer streamNum;

    public EligibilityDesc(Eligibility eligibility, Integer streamNum) {
        this.eligibility = eligibility;
        this.streamNum = streamNum;
    }

    public Eligibility getEligibility() {
        return eligibility;
    }

    public Integer getStreamNum() {
        return streamNum;
    }
}

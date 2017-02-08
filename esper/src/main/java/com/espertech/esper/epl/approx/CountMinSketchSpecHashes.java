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
package com.espertech.esper.epl.approx;

public class CountMinSketchSpecHashes {

    private double epsOfTotalCount;
    private double confidence;
    private int seed;

    public CountMinSketchSpecHashes(double epsOfTotalCount, double confidence, int seed) {
        this.epsOfTotalCount = epsOfTotalCount;
        this.confidence = confidence;
        this.seed = seed;
    }

    public double getEpsOfTotalCount() {
        return epsOfTotalCount;
    }

    public double getConfidence() {
        return confidence;
    }

    public int getSeed() {
        return seed;
    }

    public void setEpsOfTotalCount(double epsOfTotalCount) {
        this.epsOfTotalCount = epsOfTotalCount;
    }

    public void setConfidence(double confidence) {
        this.confidence = confidence;
    }

    public void setSeed(int seed) {
        this.seed = seed;
    }
}


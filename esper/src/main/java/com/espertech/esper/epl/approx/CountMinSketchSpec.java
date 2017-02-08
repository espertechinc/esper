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

import com.espertech.esper.client.util.CountMinSketchAgent;

public class CountMinSketchSpec {

    private CountMinSketchSpecHashes hashesSpec;
    private Integer topkSpec;
    private CountMinSketchAgent agent;

    public CountMinSketchSpec(CountMinSketchSpecHashes hashesSpec, Integer topkSpec, CountMinSketchAgent agent) {
        this.hashesSpec = hashesSpec;
        this.topkSpec = topkSpec;
        this.agent = agent;
    }

    public CountMinSketchSpecHashes getHashesSpec() {
        return hashesSpec;
    }

    public Integer getTopkSpec() {
        return topkSpec;
    }

    public void setTopkSpec(Integer topkSpec) {
        this.topkSpec = topkSpec;
    }

    public CountMinSketchAgent getAgent() {
        return agent;
    }

    public void setAgent(CountMinSketchAgent agent) {
        this.agent = agent;
    }
}


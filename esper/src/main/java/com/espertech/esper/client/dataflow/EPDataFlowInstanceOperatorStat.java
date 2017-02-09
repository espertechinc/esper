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
package com.espertech.esper.client.dataflow;

/**
 * Statistics holder for data flow instances.
 */
public class EPDataFlowInstanceOperatorStat {
    private final String operatorName;
    private final String operatorPrettyPrint;
    private final int operatorNumber;
    private final long submittedOverallCount;
    private final long[] submittedPerPortCount;
    private final long timeOverall;
    private final long[] timePerPort;

    /**
     * Ctor.
     *
     * @param operatorName          operator name
     * @param operatorPrettyPrint   operator pretty print
     * @param operatorNumber        operator number
     * @param submittedOverallCount count of submitted events
     * @param submittedPerPortCount count of events submitted per port
     * @param timeOverall           time spent submitting events
     * @param timePerPort           time spent submitting events per port
     */
    public EPDataFlowInstanceOperatorStat(String operatorName, String operatorPrettyPrint, int operatorNumber, long submittedOverallCount, long[] submittedPerPortCount, long timeOverall, long[] timePerPort) {
        this.operatorName = operatorName;
        this.operatorPrettyPrint = operatorPrettyPrint;
        this.operatorNumber = operatorNumber;
        this.submittedOverallCount = submittedOverallCount;
        this.submittedPerPortCount = submittedPerPortCount;
        this.timeOverall = timeOverall;
        this.timePerPort = timePerPort;
    }

    /**
     * Returns operator name.
     *
     * @return op name
     */
    public String getOperatorName() {
        return operatorName;
    }

    /**
     * Returns count of submitted events.
     *
     * @return count
     */
    public long getSubmittedOverallCount() {
        return submittedOverallCount;
    }

    /**
     * Returns count of submitted events per port.
     *
     * @return count per port
     */
    public long[] getSubmittedPerPortCount() {
        return submittedPerPortCount;
    }

    /**
     * Returns operator pretty print
     *
     * @return textual representation of op
     */
    public String getOperatorPrettyPrint() {
        return operatorPrettyPrint;
    }

    /**
     * Returns the operator number.
     *
     * @return op number
     */
    public int getOperatorNumber() {
        return operatorNumber;
    }

    /**
     * Returns total time spent submitting events
     *
     * @return time
     */
    public long getTimeOverall() {
        return timeOverall;
    }

    /**
     * Returns total time spent submitting events per port
     *
     * @return time per port
     */
    public long[] getTimePerPort() {
        return timePerPort;
    }
}

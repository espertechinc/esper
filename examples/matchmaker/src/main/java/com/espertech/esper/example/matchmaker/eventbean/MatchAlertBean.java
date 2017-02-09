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
package com.espertech.esper.example.matchmaker.eventbean;

public class MatchAlertBean {
    private int otherUserId;
    private int selfUserId;

    public MatchAlertBean(int otherUserId, int selfUserId) {
        this.otherUserId = otherUserId;
        this.selfUserId = selfUserId;
    }

    public int getSelfUserId() {
        return selfUserId;
    }

    public int getOtherUserId() {
        return otherUserId;
    }

    public String toString() {
        return "User id of self is " + selfUserId + ", user id of other user is " + otherUserId;
    }
}

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
package com.espertech.esper.common.internal.schedule;

/**
 * This class acts as a buckets for sorting schedule service callbacks that are scheduled to occur at the same
 * time. Each buckets constists of slots that callbacks are assigned to.
 * <p>
 * At the time of timer evaluation, callbacks that become triggerable are ordered using the bucket
 * as the first-level order, and slot as the second-level order.
 * <p>
 * Each statement at statement creation time allocates a buckets, and each timer within the
 * statement allocates a slot. Thus statements that depend on other statements (such as for insert-into),
 * and timers within their statement (such as time window or output rate limit timers) behave
 * deterministically.
 */
public class ScheduleBucket {
    private final int statementId;
    private int lastSlot;

    /**
     * Ctor.
     *
     * @param statementId is the statement id
     */
    public ScheduleBucket(int statementId) {
        this.statementId = statementId;
        lastSlot = 0;
    }

    public long allocateSlot() {
        return toLong(statementId, lastSlot++);
    }

    public long allocateSlot(int slotNumber) {
        return toLong(statementId, slotNumber);
    }

    public static long toLong(int bucket, int slot) {
        return ((long) bucket << 32) | slot & 0xFFFFFFFFL;
    }
}

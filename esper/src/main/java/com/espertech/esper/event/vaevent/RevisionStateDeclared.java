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
package com.espertech.esper.event.vaevent;

import com.espertech.esper.client.EventBean;

/**
 * State for the overlay (non-merge) strategy.
 */
public class RevisionStateDeclared {
    private long revisionNumber;
    private EventBean baseEventUnderlying;
    private RevisionBeanHolder[] holders;
    private RevisionEventBeanDeclared lastEvent;

    /**
     * Ctor.
     *
     * @param baseEventUnderlying base event
     * @param holders             revisions
     * @param lastEvent           prior event
     */
    public RevisionStateDeclared(EventBean baseEventUnderlying, RevisionBeanHolder[] holders, RevisionEventBeanDeclared lastEvent) {
        this.baseEventUnderlying = baseEventUnderlying;
        this.holders = holders;
        this.lastEvent = lastEvent;
    }

    /**
     * Returns revision number.
     *
     * @return version number
     */
    public long getRevisionNumber() {
        return revisionNumber;
    }

    /**
     * Increments version number.
     *
     * @return incremented version number
     */
    public long incRevisionNumber() {
        return ++revisionNumber;
    }

    /**
     * Returns base event.
     *
     * @return base event
     */
    public EventBean getBaseEventUnderlying() {
        return baseEventUnderlying;
    }

    /**
     * Sets base event.
     *
     * @param baseEventUnderlying to set
     */
    public void setBaseEventUnderlying(EventBean baseEventUnderlying) {
        this.baseEventUnderlying = baseEventUnderlying;
    }

    /**
     * Returns versions.
     *
     * @return versions
     */
    public RevisionBeanHolder[] getHolders() {
        return holders;
    }

    /**
     * Sets versions.
     *
     * @param holders versions to set
     */
    public void setHolders(RevisionBeanHolder[] holders) {
        this.holders = holders;
    }

    /**
     * Returns the last event.
     *
     * @return last event
     */
    public RevisionEventBeanDeclared getLastEvent() {
        return lastEvent;
    }

    /**
     * Sets the last event.
     *
     * @param lastEvent to set
     */
    public void setLastEvent(RevisionEventBeanDeclared lastEvent) {
        this.lastEvent = lastEvent;
    }
}

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

/**
 * Strategy for merging updates or additional properties.
 */
public interface UpdateStrategy {
    /**
     * Merge properties.
     *
     * @param isBaseEventType true if the event is a base event type
     * @param revisionState   the current state, to be updated.
     * @param revisionEvent   the new event to merge
     * @param typesDesc       descriptor for event type of the new event to merge
     */
    public void handleUpdate(boolean isBaseEventType,
                             RevisionStateMerge revisionState,
                             RevisionEventBeanMerge revisionEvent,
                             RevisionTypeDesc typesDesc);
}

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

import com.espertech.esper.event.EventPropertyGetterSPI;

/**
 * Property descriptor for use by revision event types to maintain access to revision event properties.
 */
public class RevisionPropertyTypeDesc {
    private final EventPropertyGetterSPI revisionGetter;
    private final RevisionGetterParameters revisionGetterParams;
    private final Object propertyType;  // Can be the {Class|Map|EventType}

    /**
     * Ctor.
     *
     * @param revisionGetter       getter to use
     * @param revisionGetterParams getter parameters
     * @param propertyType         type of the property
     */
    public RevisionPropertyTypeDesc(EventPropertyGetterSPI revisionGetter, RevisionGetterParameters revisionGetterParams, Class propertyType) {
        this.revisionGetter = revisionGetter;
        this.revisionGetterParams = revisionGetterParams;
        this.propertyType = propertyType;
    }

    /**
     * Returns the getter for the property on the revision event type.
     *
     * @return getter
     */
    public EventPropertyGetterSPI getRevisionGetter() {
        return revisionGetter;
    }

    /**
     * Returns parameters for the getter for the property on the revision event type.
     *
     * @return getter parameters
     */
    public RevisionGetterParameters getRevisionGetterParams() {
        return revisionGetterParams;
    }

    /**
     * Returns property type.
     *
     * @return type
     */
    public Object getPropertyType() {
        return propertyType;
    }
}

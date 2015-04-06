/*
 * *************************************************************************************
 *  Copyright (C) 2006-2015 EsperTech, Inc. All rights reserved.                       *
 *  http://www.espertech.com/esper                                                     *
 *  http://www.espertech.com                                                           *
 *  ---------------------------------------------------------------------------------- *
 *  The software in this package is published under the terms of the GPL license       *
 *  a copy of which has been included with this distribution in the license.txt file.  *
 * *************************************************************************************
 */

package com.espertech.esper.support.event;

import com.espertech.esper.client.EventPropertyDescriptor;
import com.espertech.esper.client.EventType;
import com.espertech.esper.client.FragmentEventType;

public enum EventTypeAssertionEnum
{
    NAME(new Extractor() {
        public Object extract(EventPropertyDescriptor desc, EventType eventType) {
            return desc.getPropertyName();
        }
    }),
    TYPE(new Extractor() {
        public Object extract(EventPropertyDescriptor desc, EventType eventType) {
            return desc.getPropertyType();
        }
    }),
    FRAGEMENT_TYPE_NAME(new Extractor() {
        public Object extract(EventPropertyDescriptor desc, EventType eventType) {
            FragmentEventType fragType = eventType.getFragmentType(desc.getPropertyName());
            if (fragType == null) {
                return null;
            }
            return fragType.getFragmentType().getName();
        }
    }),
    FRAGMENT_IS_INDEXED(new Extractor() {
        public Object extract(EventPropertyDescriptor desc, EventType eventType) {
            FragmentEventType fragType = eventType.getFragmentType(desc.getPropertyName());
            if (fragType == null) {
                return null;
            }
            return fragType.isIndexed();
        }
    });

    private Extractor extractor;

    private EventTypeAssertionEnum(Extractor extractor) {
        this.extractor = extractor;
    }

    public Extractor getExtractor() {
        return extractor;
    }

    public interface Extractor {
        public Object extract(EventPropertyDescriptor desc, EventType eventType);
    }

    public static EventTypeAssertionEnum[] getSetWithFragment() {
        return new EventTypeAssertionEnum[] {NAME, TYPE, FRAGEMENT_TYPE_NAME, FRAGMENT_IS_INDEXED};
    }
}
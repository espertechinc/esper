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
package com.espertech.esper.common.internal.supportunit.util;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.epl.streamtype.*;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.common.internal.supportunit.event.SupportEventTypeFactory;

public class SupportStreamTypeSvc1Stream implements StreamTypeService {
    private StreamTypeService impl;

    public SupportStreamTypeSvc1Stream() {
        impl = new StreamTypeServiceImpl(getEventTypes(), getStreamNames(), new boolean[10], false, false);
    }

    public PropertyResolutionDescriptor resolveByPropertyName(String propertyName, boolean obtainFragment) throws DuplicatePropertyException, PropertyNotFoundException {
        return impl.resolveByPropertyName(propertyName, false);
    }

    public PropertyResolutionDescriptor resolveByStreamAndPropName(String streamName, String propertyName, boolean obtainFragment) throws PropertyNotFoundException, StreamNotFoundException {
        return impl.resolveByStreamAndPropName(streamName, propertyName, false);
    }

    public PropertyResolutionDescriptor resolveByStreamAndPropName(String streamAndPropertyName, boolean obtainFragment) throws DuplicatePropertyException, PropertyNotFoundException {
        return impl.resolveByStreamAndPropName(streamAndPropertyName, false);
    }

    public PropertyResolutionDescriptor resolveByPropertyNameExplicitProps(String propertyName, boolean obtainFragment) throws PropertyNotFoundException, DuplicatePropertyException {
        return impl.resolveByPropertyNameExplicitProps(propertyName, false);
    }

    public PropertyResolutionDescriptor resolveByStreamAndPropNameExplicitProps(String streamName, String propertyName, boolean obtainFragment) throws PropertyNotFoundException, StreamNotFoundException {
        return impl.resolveByStreamAndPropNameExplicitProps(streamName, propertyName, false);
    }

    public String[] getStreamNames() {
        return new String[]{"s0"};
    }

    public EventType[] getEventTypes() {
        return new EventType[]{
                SupportEventTypeFactory.createBeanType(SupportBean.class)
        };
    }

    public boolean[] getIStreamOnly() {
        return new boolean[10];
    }

    public int getStreamNumForStreamName(String streamWildcard) {
        return impl.getStreamNumForStreamName(streamWildcard);
    }

    public boolean isOnDemandStreams() {
        return impl.isOnDemandStreams();
    }

    public boolean hasPropertyAgnosticType() {
        return false;
    }

    public boolean hasTableTypes() {
        return false;
    }

    public boolean isStreamZeroUnambigous() {
        return false;
    }

    public boolean isOptionalStreams() {
        return false;
    }
}

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
package com.espertech.esper.epl.util;

import com.espertech.esper.client.ConfigurationInformation;
import com.espertech.esper.client.annotation.EventRepresentation;
import com.espertech.esper.client.util.EventUnderlyingType;
import com.espertech.esper.epl.annotation.AnnotationUtil;
import com.espertech.esper.epl.spec.CreateSchemaDesc;

import java.lang.annotation.Annotation;

public class EventRepresentationUtil {

    public static EventUnderlyingType getRepresentation(Annotation[] annotations, ConfigurationInformation configs, CreateSchemaDesc.AssignedType assignedType) {
        // assigned type has priority
        if (assignedType == CreateSchemaDesc.AssignedType.OBJECTARRAY) {
            return EventUnderlyingType.OBJECTARRAY;
        } else if (assignedType == CreateSchemaDesc.AssignedType.MAP) {
            return EventUnderlyingType.MAP;
        } else if (assignedType == CreateSchemaDesc.AssignedType.AVRO) {
            return EventUnderlyingType.AVRO;
        }
        if (assignedType == CreateSchemaDesc.AssignedType.VARIANT || assignedType != CreateSchemaDesc.AssignedType.NONE) {
            throw new IllegalStateException("Not handled by event representation: " + assignedType);
        }

        // annotation has second priority
        Annotation annotation = AnnotationUtil.findAnnotation(annotations, EventRepresentation.class);
        if (annotation != null) {
            EventRepresentation eventRepresentation = (EventRepresentation) annotation;
            if (eventRepresentation.value() == EventUnderlyingType.AVRO) {
                return EventUnderlyingType.AVRO;
            } else if (eventRepresentation.value() == EventUnderlyingType.OBJECTARRAY) {
                return EventUnderlyingType.OBJECTARRAY;
            } else if (eventRepresentation.value() == EventUnderlyingType.MAP) {
                return EventUnderlyingType.MAP;
            } else {
                throw new IllegalStateException("Unrecognized enum " + eventRepresentation.value());
            }
        }

        // use engine-wide default
        EventUnderlyingType configured = configs.getEngineDefaults().getEventMeta().getDefaultEventRepresentation();
        if (configured == EventUnderlyingType.OBJECTARRAY) {
            return EventUnderlyingType.OBJECTARRAY;
        } else if (configured == EventUnderlyingType.MAP) {
            return EventUnderlyingType.MAP;
        } else if (configured == EventUnderlyingType.AVRO) {
            return EventUnderlyingType.AVRO;
        }
        return EventUnderlyingType.MAP;
    }
}

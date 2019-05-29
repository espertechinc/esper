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
package com.espertech.esper.common.internal.util;

import com.espertech.esper.common.client.annotation.EventRepresentation;
import com.espertech.esper.common.client.configuration.Configuration;
import com.espertech.esper.common.client.util.EventUnderlyingType;
import com.espertech.esper.common.internal.compile.stage1.spec.CreateSchemaDesc;
import com.espertech.esper.common.internal.epl.annotation.AnnotationUtil;

import java.lang.annotation.Annotation;

public class EventRepresentationUtil {

    public static EventUnderlyingType getRepresentation(Annotation[] annotations, Configuration configs, CreateSchemaDesc.AssignedType assignedType) {
        // assigned type has priority
        if (assignedType == CreateSchemaDesc.AssignedType.OBJECTARRAY) {
            return EventUnderlyingType.OBJECTARRAY;
        } else if (assignedType == CreateSchemaDesc.AssignedType.MAP) {
            return EventUnderlyingType.MAP;
        } else if (assignedType == CreateSchemaDesc.AssignedType.AVRO) {
            return EventUnderlyingType.AVRO;
        } else if (assignedType == CreateSchemaDesc.AssignedType.JSON) {
            return EventUnderlyingType.JSON;
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
            } else if (eventRepresentation.value() == EventUnderlyingType.JSON) {
                return EventUnderlyingType.JSON;
            } else if (eventRepresentation.value() == EventUnderlyingType.OBJECTARRAY) {
                return EventUnderlyingType.OBJECTARRAY;
            } else if (eventRepresentation.value() == EventUnderlyingType.MAP) {
                return EventUnderlyingType.MAP;
            } else {
                throw new IllegalStateException("Unrecognized enum " + eventRepresentation.value());
            }
        }

        // use runtime-wide default
        EventUnderlyingType configured = configs.getCommon().getEventMeta().getDefaultEventRepresentation();
        if (configured == EventUnderlyingType.OBJECTARRAY) {
            return EventUnderlyingType.OBJECTARRAY;
        } else if (configured == EventUnderlyingType.MAP) {
            return EventUnderlyingType.MAP;
        } else if (configured == EventUnderlyingType.AVRO) {
            return EventUnderlyingType.AVRO;
        } else if (configured == EventUnderlyingType.JSON) {
            return EventUnderlyingType.JSON;
        }
        return EventUnderlyingType.MAP;
    }
}

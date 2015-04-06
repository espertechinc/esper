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

package com.espertech.esper.util;

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.ConfigurationInformation;
import com.espertech.esper.client.annotation.EventRepresentation;
import com.espertech.esper.epl.annotation.AnnotationUtil;
import com.espertech.esper.epl.spec.CreateSchemaDesc;

import java.lang.annotation.Annotation;

public class EventRepresentationUtil {

    public static boolean isMap(Annotation[] annotations, ConfigurationInformation configs, CreateSchemaDesc.AssignedType assignedType) {
        // assigned type has priority
        if (assignedType == CreateSchemaDesc.AssignedType.OBJECTARRAY) {
            return false;
        }
        if (assignedType == CreateSchemaDesc.AssignedType.MAP) {
            return true;
        }
        if (assignedType == CreateSchemaDesc.AssignedType.VARIANT || assignedType != CreateSchemaDesc.AssignedType.NONE) {
            throw new IllegalStateException("Not handled by event representation: " + assignedType);
        }

        // annotation has second priority
        Annotation annotation = AnnotationUtil.findAnnotation(annotations, EventRepresentation.class);
        if (annotation != null) {
            EventRepresentation eventRepresentation = (EventRepresentation) annotation;
            return !eventRepresentation.array();
        }

        // use engine-wide default
        return configs.getEngineDefaults().getEventMeta().getDefaultEventRepresentation() == Configuration.EventRepresentation.MAP;
    }
}

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
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.annotation.EventRepresentation;
import com.espertech.esper.client.soda.AnnotationPart;
import com.espertech.esper.client.soda.EPStatementObjectModel;
import com.espertech.esper.core.service.EPServiceProviderSPI;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public enum EventRepresentationEnum {
    OBJECTARRAY(Configuration.EventRepresentation.OBJECTARRAY, "@EventRepresentation(array=true)", " objectarray"),
    MAP(Configuration.EventRepresentation.MAP, "@EventRepresentation(array=false)", " map"),
    AVRO(Configuration.EventRepresentation.AVRO, "@EventRepresentation(avro=true)", " objectarray"),
    DEFAULT(Configuration.EventRepresentation.getDefault(), "", "");

    private final String annotationText;
    private final String outputTypeCreateSchemaName;
    private final String outputTypeClassName;

    EventRepresentationEnum(Configuration.EventRepresentation eventRepresentation, String annotationText, String outputTypeCreateSchemaName) {
        this.annotationText = annotationText;
        this.outputTypeCreateSchemaName = outputTypeCreateSchemaName;
        this.outputTypeClassName = eventRepresentation.getUnderlyingClassName();
    }

    public static EventRepresentationEnum getEngineDefault(EPServiceProvider engine) {
        EPServiceProviderSPI spi = (EPServiceProviderSPI) engine;
        Configuration.EventRepresentation configured = spi.getConfigurationInformation().getEngineDefaults().getEventMeta().getDefaultEventRepresentation();
        if (configured == Configuration.EventRepresentation.OBJECTARRAY) {
            return OBJECTARRAY;
        }
        else if (configured == Configuration.EventRepresentation.AVRO) {
            return AVRO;
        }
        return MAP;
    }

    public String getAnnotationText() {
        return annotationText;
    }

    public String getOutputTypeClassName() {
        return outputTypeClassName;
    }

    public String getOutputTypeCreateSchemaName() {
        return outputTypeCreateSchemaName;
    }

    public boolean matchesClass(Class representationType) {
        Set<Class> supers = new HashSet<>();
        JavaClassHelper.getSuper(representationType, supers);
        supers.add(representationType);
        for (Class clazz : supers) {
            if (clazz.getName().equals(outputTypeClassName)) {
                return true;
            }
        }
        return false;
    }

    public boolean isObjectArrayEvent() {
        return this == OBJECTARRAY;
    }

    public String getAnnotationTextForNonMap() {
        if (this == DEFAULT || this == MAP) {
            return "";
        }
        return annotationText;
    }

    public void addAnnotationForNonMap(EPStatementObjectModel model) {
        if (this == DEFAULT || this == MAP) {
            return;
        }
        AnnotationPart part = new AnnotationPart(EventRepresentation.class.getSimpleName());
        if (this == OBJECTARRAY) {
            part.addValue("array", true);
        }
        if (this == AVRO) {
            part.addValue("avro", true);
        }
        model.setAnnotations(Collections.singletonList(part));
    }

    public boolean isAvro() {
        return this == AVRO;
    }
}

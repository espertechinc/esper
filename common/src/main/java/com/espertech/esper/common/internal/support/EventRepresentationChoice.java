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
package com.espertech.esper.common.internal.support;

import com.espertech.esper.common.client.annotation.EventRepresentation;
import com.espertech.esper.common.client.configuration.Configuration;
import com.espertech.esper.common.client.soda.AnnotationPart;
import com.espertech.esper.common.client.soda.EPStatementObjectModel;
import com.espertech.esper.common.client.util.EventUnderlyingType;
import com.espertech.esper.common.internal.util.JavaClassHelper;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public enum EventRepresentationChoice {
    ARRAY(EventUnderlyingType.OBJECTARRAY, "@EventRepresentation('objectarray')", " objectarray"),
    MAP(EventUnderlyingType.MAP, "@EventRepresentation('map')", " map"),
    AVRO(EventUnderlyingType.AVRO, "@EventRepresentation('avro')", " avro"),
    DEFAULT(EventUnderlyingType.getDefault(), "", "");

    private final EventUnderlyingType eventRepresentation;
    private final String annotationText;
    private final String outputTypeCreateSchemaName;
    private final String outputTypeClassName;

    EventRepresentationChoice(EventUnderlyingType eventRepresentation, String annotationText, String outputTypeCreateSchemaName) {
        this.eventRepresentation = eventRepresentation;
        this.annotationText = annotationText;
        this.outputTypeCreateSchemaName = outputTypeCreateSchemaName;
        this.outputTypeClassName = eventRepresentation.getUnderlyingClassName();
    }

    public String getUndName() {
        return eventRepresentation.name();
    }

    public String getAnnotationText() {
        return annotationText;
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
        return this == ARRAY;
    }

    public boolean isMapEvent() {
        return this == DEFAULT || this == MAP;
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
        if (this == ARRAY) {
            part.addValue("objectarray");
        }
        if (this == AVRO) {
            part.addValue("avro");
        }
        model.setAnnotations(Collections.singletonList(part));
    }

    public boolean isAvroEvent() {
        return this == AVRO;
    }

    public static EventRepresentationChoice getEngineDefault(Configuration configuration) {
        EventUnderlyingType configured = configuration.getCommon().getEventMeta().getDefaultEventRepresentation();
        if (configured == EventUnderlyingType.OBJECTARRAY) {
            return ARRAY;
        } else if (configured == EventUnderlyingType.AVRO) {
            return AVRO;
        }
        return MAP;
    }
}

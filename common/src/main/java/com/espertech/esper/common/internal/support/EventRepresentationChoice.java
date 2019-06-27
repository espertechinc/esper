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
    OBJECTARRAY(EventUnderlyingType.OBJECTARRAY, "@EventRepresentation('objectarray')"),
    MAP(EventUnderlyingType.MAP, "@EventRepresentation('map')"),
    AVRO(EventUnderlyingType.AVRO, "@EventRepresentation('avro')"),
    JSON(EventUnderlyingType.JSON, "@EventRepresentation('json')"),
    JSONCLASSPROVIDED(EventUnderlyingType.JSON, "@EventRepresentation('json')"),
    DEFAULT(EventUnderlyingType.getDefault(), "");

    private final EventUnderlyingType eventRepresentation;
    private final String annotationText;
    private final String outputTypeClassName;
    private final Class outputTypeClass;

    EventRepresentationChoice(EventUnderlyingType eventRepresentation, String annotationText) {
        this.eventRepresentation = eventRepresentation;
        this.annotationText = annotationText;
        this.outputTypeClassName = eventRepresentation.getUnderlyingClassName();
        this.outputTypeClass = eventRepresentation.getUnderlyingClass();
    }

    public String getAnnotationText() {
        if (this == JSONCLASSPROVIDED) {
            throw new UnsupportedOperationException("For Json-Provided please use getAnnotationTextWJsonProvided(class)");
        }
        return annotationText;
    }

    public String getAnnotationTextWJsonProvided(Class jsonProvidedClass) {
        if (this == JSONCLASSPROVIDED) {
            return "@JsonSchema(className='" + jsonProvidedClass.getName() + "') " + annotationText;
        }
        return annotationText;
    }

    public boolean matchesClass(Class representationType) {
        Set<Class> supers = new HashSet<>();
        JavaClassHelper.getSuper(representationType, supers);
        supers.add(representationType);
        for (Class clazz : supers) {
            if (clazz.getName().equals(outputTypeClassName) || (outputTypeClass != null && JavaClassHelper.isSubclassOrImplementsInterface(clazz, outputTypeClass))) {
                return true;
            }
        }
        return false;
    }

    public boolean isObjectArrayEvent() {
        return this == OBJECTARRAY;
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
        if (this == OBJECTARRAY) {
            part.addValue("objectarray");
        }
        if (this == AVRO) {
            part.addValue("avro");
        }
        if (this == JSON || this == JSONCLASSPROVIDED) {
            part.addValue("json");
        }
        model.setAnnotations(Collections.singletonList(part));
    }

    public boolean isAvroEvent() {
        return this == AVRO;
    }

    public boolean isAvroOrJsonEvent() {
        return this == AVRO || this == JSON || this == JSONCLASSPROVIDED;
    }

    public static EventRepresentationChoice getEngineDefault(Configuration configuration) {
        EventUnderlyingType configured = configuration.getCommon().getEventMeta().getDefaultEventRepresentation();
        if (configured == EventUnderlyingType.OBJECTARRAY) {
            return OBJECTARRAY;
        } else if (configured == EventUnderlyingType.AVRO) {
            return AVRO;
        }
        return MAP;
    }

    public boolean isJsonEvent() {
        return this == JSON;
    }

    public boolean isJsonProvidedClassEvent() {
        return this == JSONCLASSPROVIDED;
    }

    public String getName() {
        if (this == DEFAULT) {
            return this.eventRepresentation.name();
        }
        return name();
    }
}

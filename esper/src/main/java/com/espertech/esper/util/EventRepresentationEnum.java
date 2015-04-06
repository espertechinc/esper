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
import java.util.Map;

public enum EventRepresentationEnum {
    OBJECTARRAY("@EventRepresentation(array=true)", Object[].class, " objectarray"),
    MAP("@EventRepresentation(array=false)", Map.class, " map"),
    DEFAULT(null, null, "");

    private final String annotationText;
    private final Class outputType;
    private final String outputTypeCreateSchemaName;

    EventRepresentationEnum(String annotationText, Class outputType, String outputTypeCreateSchemaName) {

        if (annotationText == null) {
            this.annotationText = "";
            this.outputType = Configuration.EventRepresentation.getDefault() == Configuration.EventRepresentation.OBJECTARRAY ? Object[].class : Map.class;
        }
        else {
            this.annotationText = annotationText;
            this.outputType = outputType;
        }
        this.outputTypeCreateSchemaName = outputTypeCreateSchemaName;
    }

    public static EventRepresentationEnum getEngineDefault(EPServiceProvider engine) {
        EPServiceProviderSPI spi = (EPServiceProviderSPI) engine;
        if (spi.getConfigurationInformation().getEngineDefaults().getEventMeta().getDefaultEventRepresentation() == Configuration.EventRepresentation.OBJECTARRAY) {
            return OBJECTARRAY;
        }
        return MAP;
    }

    public String getAnnotationText() {
        return annotationText;
    }

    public Class getOutputClass() {
        return outputType;
    }

    public String getOutputTypeCreateSchemaName() {
        return outputTypeCreateSchemaName;
    }

    public boolean matchesClass(Class representationType) {
        return JavaClassHelper.isSubclassOrImplementsInterface(representationType, this.outputType);
    }

    public boolean isObjectArrayEvent() {
        return outputType == Object[].class;
    }

    public void addAnnotation(EPStatementObjectModel model) {
        if (this == DEFAULT) {
            return;
        }
        AnnotationPart part = new AnnotationPart(EventRepresentation.class.getSimpleName());
        part.addValue("array", this == OBJECTARRAY);
        model.setAnnotations(Collections.singletonList(part));
    }
}

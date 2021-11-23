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
package com.espertech.esper.common.internal.xmlxsd.core;

import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.common.internal.event.xml.EventTypeXMLXSDHandler;
import com.espertech.esper.common.internal.event.xml.SchemaModel;
import com.espertech.esper.common.internal.settings.ClasspathImportService;

import javax.xml.namespace.QName;

public class EventTypeXMLXSDHandlerImpl implements EventTypeXMLXSDHandler {
    public QName simpleTypeToQName(short type) {
        return SchemaUtilXerces.simpleTypeToQName(type);
    }

    public EPTypeClass toReturnType(short xsType, String typeName, Integer optionalFractionDigits) {
        return SchemaUtilXerces.toReturnType(xsType, typeName, optionalFractionDigits);
    }

    public SchemaModel loadAndMap(String schemaResource, String schemaText, ClasspathImportService classpathImportService) {
        return XSDSchemaMapper.loadAndMap(schemaResource, schemaText, classpathImportService);
    }
}

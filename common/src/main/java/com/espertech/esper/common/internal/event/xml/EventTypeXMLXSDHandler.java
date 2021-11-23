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
package com.espertech.esper.common.internal.event.xml;

import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.common.internal.settings.ClasspathImportService;

import javax.xml.namespace.QName;

public interface EventTypeXMLXSDHandler {
    String HANDLER_IMPL = "com.espertech.esper.common.internal.xmlxsd.core.EventTypeXMLXSDHandlerImpl";

    QName simpleTypeToQName(short type);
    EPTypeClass toReturnType(short xsType, String typeName, Integer optionalFractionDigits);
    SchemaModel loadAndMap(String schemaResource, String schemaText, ClasspathImportService classpathImportService);
}

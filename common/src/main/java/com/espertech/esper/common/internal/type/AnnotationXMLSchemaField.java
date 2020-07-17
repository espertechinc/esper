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
package com.espertech.esper.common.internal.type;

import com.espertech.esper.common.client.annotation.XMLSchemaField;
import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenSetterBuilder;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;

import java.lang.annotation.Annotation;

public class AnnotationXMLSchemaField implements XMLSchemaField {
    public final static EPTypeClass EPTYPE = new EPTypeClass(AnnotationXMLSchemaField.class);

    private String name;
    private String xpath;
    private String type;
    private String eventTypeName;
    private String castToType;

    public AnnotationXMLSchemaField() {
    }

    public static CodegenExpression toExpression(XMLSchemaField field, CodegenMethod parent, CodegenClassScope scope) {
        return new CodegenSetterBuilder(AnnotationXMLSchemaField.EPTYPE, AnnotationXMLSchemaField.class,
            "field", parent, scope)
            .constantExplicit("name", field.name())
            .constantExplicit("xpath", field.xpath())
            .constantExplicit("type", field.type())
            .constantExplicit("eventTypeName", field.eventTypeName())
            .constantExplicit("castToType", field.castToType())
            .build();
    }

    public String name() {
        return name;
    }

    public String xpath() {
        return xpath;
    }

    public String type() {
        return type;
    }

    public String eventTypeName() {
        return eventTypeName;
    }

    public String castToType() {
        return castToType;
    }

    public Class<? extends Annotation> annotationType() {
        return XMLSchemaField.class;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getXpath() {
        return xpath;
    }

    public void setXpath(String xpath) {
        this.xpath = xpath;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getEventTypeName() {
        return eventTypeName;
    }

    public void setEventTypeName(String eventTypeName) {
        this.eventTypeName = eventTypeName;
    }

    public String getCastToType() {
        return castToType;
    }

    public void setCastToType(String castToType) {
        this.castToType = castToType;
    }
}

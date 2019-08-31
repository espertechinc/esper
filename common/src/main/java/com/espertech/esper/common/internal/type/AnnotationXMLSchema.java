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

import com.espertech.esper.common.client.annotation.XMLSchema;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenSetterBuilder;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;

import java.lang.annotation.Annotation;

public class AnnotationXMLSchema implements XMLSchema {
    private String rootElementName;
    private String schemaResource;
    private String schemaText;
    private boolean xpathPropertyExpr;
    private String defaultNamespace;
    private String rootElementNamespace;
    private boolean eventSenderValidatesRoot;
    private boolean autoFragment;
    private String xpathFunctionResolver;
    private String xpathVariableResolver;
    private boolean xpathResolvePropertiesAbsolute;

    public AnnotationXMLSchema() {
    }

    public static CodegenExpression toExpression(XMLSchema xmlSchema, CodegenMethodScope parent, CodegenClassScope scope) {
        return new CodegenSetterBuilder(AnnotationXMLSchema.class, AnnotationXMLSchema.class, "xmlschema", parent, scope)
            .constant("rootElementName", xmlSchema.rootElementName())
            .constant("schemaResource", xmlSchema.schemaResource())
            .constant("schemaText", xmlSchema.schemaText())
            .constant("schemaText", xmlSchema.schemaText())
            .constant("xpathPropertyExpr", xmlSchema.xpathPropertyExpr())
            .constant("defaultNamespace", xmlSchema.defaultNamespace())
            .constant("eventSenderValidatesRoot", xmlSchema.eventSenderValidatesRoot())
            .constant("autoFragment", xmlSchema.autoFragment())
            .constant("xpathFunctionResolver", xmlSchema.xpathFunctionResolver())
            .constant("xpathVariableResolver", xmlSchema.xpathVariableResolver())
            .constant("rootElementNamespace", xmlSchema.rootElementNamespace())
            .constant("xpathResolvePropertiesAbsolute", xmlSchema.xpathResolvePropertiesAbsolute())
            .build();
    }

    public String rootElementName() {
        return rootElementName;
    }

    public String schemaResource() {
        return schemaResource;
    }

    public String schemaText() {
        return schemaText;
    }

    public boolean xpathPropertyExpr() {
        return xpathPropertyExpr;
    }

    public String defaultNamespace() {
        return defaultNamespace;
    }

    public boolean eventSenderValidatesRoot() {
        return eventSenderValidatesRoot;
    }

    public boolean autoFragment() {
        return autoFragment;
    }

    public String xpathFunctionResolver() {
        return xpathFunctionResolver;
    }

    public String xpathVariableResolver() {
        return xpathVariableResolver;
    }

    public boolean xpathResolvePropertiesAbsolute() {
        return xpathResolvePropertiesAbsolute;
    }

    public String rootElementNamespace() {
        return rootElementName;
    }

    public void setRootElementName(String rootElementName) {
        this.rootElementName = rootElementName;
    }

    public void setSchemaResource(String schemaResource) {
        this.schemaResource = schemaResource;
    }

    public void setXpathPropertyExpr(boolean xpathPropertyExpr) {
        this.xpathPropertyExpr = xpathPropertyExpr;
    }

    public void setDefaultNamespace(String defaultNamespace) {
        this.defaultNamespace = defaultNamespace;
    }

    public void setEventSenderValidatesRoot(boolean eventSenderValidatesRoot) {
        this.eventSenderValidatesRoot = eventSenderValidatesRoot;
    }

    public String getRootElementName() {
        return rootElementName;
    }

    public String getSchemaResource() {
        return schemaResource;
    }

    public boolean isXpathPropertyExpr() {
        return xpathPropertyExpr;
    }

    public String getDefaultNamespace() {
        return defaultNamespace;
    }

    public boolean isEventSenderValidatesRoot() {
        return eventSenderValidatesRoot;
    }

    public boolean isAutoFragment() {
        return autoFragment;
    }

    public void setAutoFragment(boolean autoFragment) {
        this.autoFragment = autoFragment;
    }

    public String getSchemaText() {
        return schemaText;
    }

    public void setSchemaText(String schemaText) {
        this.schemaText = schemaText;
    }

    public String getXpathFunctionResolver() {
        return xpathFunctionResolver;
    }

    public void setXpathFunctionResolver(String xpathFunctionResolver) {
        this.xpathFunctionResolver = xpathFunctionResolver;
    }

    public String getXpathVariableResolver() {
        return xpathVariableResolver;
    }

    public void setXpathVariableResolver(String xpathVariableResolver) {
        this.xpathVariableResolver = xpathVariableResolver;
    }

    public String getRootElementNamespace() {
        return rootElementNamespace;
    }

    public void setRootElementNamespace(String rootElementNamespace) {
        this.rootElementNamespace = rootElementNamespace;
    }

    public boolean isXpathResolvePropertiesAbsolute() {
        return xpathResolvePropertiesAbsolute;
    }

    public void setXpathResolvePropertiesAbsolute(boolean xpathResolvePropertiesAbsolute) {
        this.xpathResolvePropertiesAbsolute = xpathResolvePropertiesAbsolute;
    }

    public Class<? extends Annotation> annotationType() {
        return XMLSchema.class;
    }
}

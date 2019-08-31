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

import com.espertech.esper.common.client.annotation.XMLSchemaNamespacePrefix;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenSetterBuilder;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;

import java.lang.annotation.Annotation;

public class AnnotationXMLSchemaNamespacePrefix implements XMLSchemaNamespacePrefix {
    private String prefix;
    private String namespace;

    public AnnotationXMLSchemaNamespacePrefix() {
    }

    public String prefix() {
        return prefix;
    }

    public String namespace() {
        return namespace;
    }

    public Class<? extends Annotation> annotationType() {
        return XMLSchemaNamespacePrefix.class;
    }

    public static CodegenExpression toExpression(XMLSchemaNamespacePrefix prefix, CodegenMethod parent, CodegenClassScope scope) {
        return new CodegenSetterBuilder(AnnotationXMLSchemaNamespacePrefix.class, AnnotationXMLSchemaNamespacePrefix.class,
            "nsprefix", parent, scope)
            .constant("prefix", prefix.prefix())
            .constant("namespace", prefix.namespace())
            .build();
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }
}

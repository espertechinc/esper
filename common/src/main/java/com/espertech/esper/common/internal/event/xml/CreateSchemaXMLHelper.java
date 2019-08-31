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

import com.espertech.esper.common.client.annotation.XMLSchema;
import com.espertech.esper.common.client.annotation.XMLSchemaField;
import com.espertech.esper.common.client.annotation.XMLSchemaNamespacePrefix;
import com.espertech.esper.common.client.configuration.common.ConfigurationCommonEventTypeXMLDOM;
import com.espertech.esper.common.internal.compile.stage3.StatementBaseInfo;
import com.espertech.esper.common.internal.compile.stage3.StatementCompileTimeServices;
import com.espertech.esper.common.internal.epl.annotation.AnnotationUtil;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;

import javax.xml.namespace.QName;
import javax.xml.xpath.XPathConstants;
import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Locale;

public class CreateSchemaXMLHelper {
    public static ConfigurationCommonEventTypeXMLDOM configure(StatementBaseInfo base, StatementCompileTimeServices services) throws ExprValidationException {
        ConfigurationCommonEventTypeXMLDOM config = new ConfigurationCommonEventTypeXMLDOM();
        Annotation[] annotations = base.getStatementRawInfo().getAnnotations();

        List<Annotation> schemaAnnotations = AnnotationUtil.findAnnotations(annotations, XMLSchema.class);
        if (schemaAnnotations == null || schemaAnnotations.isEmpty()) {
            throw new ExprValidationException("Required annotation @" + XMLSchema.class.getSimpleName() + " could not be found");
        }
        if (schemaAnnotations.size() > 1) {
            throw new ExprValidationException("Found multiple @" + XMLSchema.class.getSimpleName() + " annotations but expected a single annotation");
        }
        XMLSchema schema = (XMLSchema) schemaAnnotations.get(0);
        if (schema.rootElementName().trim().length() == 0) {
            throw new ExprValidationException("Required annotation field 'rootElementName' for annotation @" + XMLSchema.class.getSimpleName() + " could not be found");
        }
        config.setRootElementName(schema.rootElementName().trim());
        config.setSchemaResource(nullIfEmpty(schema.schemaResource()));
        config.setSchemaText(nullIfEmpty(schema.schemaText()));
        config.setXPathPropertyExpr(schema.xpathPropertyExpr());
        config.setDefaultNamespace(schema.defaultNamespace());
        config.setEventSenderValidatesRoot(schema.eventSenderValidatesRoot());
        config.setAutoFragment(schema.autoFragment());
        config.setXPathFunctionResolver(nullIfEmpty(schema.xpathFunctionResolver()));
        config.setXPathVariableResolver(nullIfEmpty(schema.xpathVariableResolver()));
        config.setXPathResolvePropertiesAbsolute(schema.xpathResolvePropertiesAbsolute());
        config.setRootElementNamespace(nullIfEmpty(schema.rootElementNamespace()));

        List<Annotation> prefixes = AnnotationUtil.findAnnotations(annotations, XMLSchemaNamespacePrefix.class);
        for (Annotation prefixAnnotation : prefixes) {
            XMLSchemaNamespacePrefix prefix = (XMLSchemaNamespacePrefix) prefixAnnotation;
            config.addNamespacePrefix(prefix.prefix(), prefix.namespace());
        }

        List<Annotation> fields = AnnotationUtil.findAnnotations(annotations, XMLSchemaField.class);
        for (Annotation fieldAnnotation : fields) {
            XMLSchemaField field = (XMLSchemaField) fieldAnnotation;
            QName qname = getQName(field.type());
            if (field.eventTypeName().trim().length() == 0) {
                String castToType = nullIfEmpty(field.castToType());
                config.addXPathProperty(field.name(), field.xpath(), qname, castToType);
            } else {
                config.addXPathPropertyFragment(field.name(), field.xpath(), qname, field.eventTypeName());
            }
        }

        return config;
    }

    private static String nullIfEmpty(String text) {
        return text == null ? null : (text.trim().length() == 0 ? null : text.trim());
    }

    private static QName getQName(String type) {
        String localPart = type.toUpperCase(Locale.ENGLISH);
        if (localPart.equals(XPathConstants.NODE.getLocalPart())) {
            return XPathConstants.NODE;
        } else if (localPart.equals(XPathConstants.NODESET.getLocalPart())) {
            return XPathConstants.NODESET;
        }
        if (localPart.equals(XPathConstants.STRING.getLocalPart())) {
            return XPathConstants.STRING;
        }
        if (localPart.equals(XPathConstants.NUMBER.getLocalPart())) {
            return XPathConstants.NUMBER;
        }
        if (localPart.equals(XPathConstants.BOOLEAN.getLocalPart())) {
            return XPathConstants.BOOLEAN;
        }
        return QName.valueOf(localPart);
    }
}

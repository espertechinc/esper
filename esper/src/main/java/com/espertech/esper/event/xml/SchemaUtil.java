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
package com.espertech.esper.event.xml;

import com.espertech.esper.client.EPException;
import com.espertech.esper.client.PropertyAccessException;
import com.sun.org.apache.xerces.internal.impl.dv.XSSimpleType;
import org.w3c.dom.*;

import javax.xml.namespace.QName;
import javax.xml.xpath.XPathConstants;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Array;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility class for querying schema information via Xerces implementation classes.
 *
 * @author pablo
 */
public class SchemaUtil {

    private static Map<String, Class> typeMap;

    static {
        typeMap = new HashMap<String, Class>();
        Object[][] types = new Object[][]{
                {"nonPositiveInteger", Integer.class},
                {"nonNegativeInteger", Integer.class},
                {"negativeInteger", Integer.class},
                {"positiveInteger", Integer.class},
                {"long", Long.class},
                {"unsignedLong", Long.class},
                {"int", Integer.class},
                {"unsignedInt", Integer.class},
                {"decimal", Double.class},
                {"integer", Integer.class},
                {"float", Float.class},
                {"double", Double.class},
                {"string", String.class},
                {"short", Short.class},
                {"unsignedShort", Short.class},
                {"byte", Byte.class},
                {"unsignedByte", Byte.class},
                {"boolean", Boolean.class},
                {"dateTime", String.class},
                {"date", String.class},
                {"time", String.class}};
        for (int i = 0; i < types.length; i++) {
            typeMap.put(types[i][0].toString(), (Class) types[i][1]);
        }
    }

    /**
     * Returns the Class-type of the schema item.
     *
     * @param item to to determine type for
     * @return type
     */
    public static Class toReturnType(SchemaItem item) {
        if (item instanceof SchemaItemAttribute) {
            SchemaItemAttribute att = (SchemaItemAttribute) item;
            return SchemaUtil.toReturnType(att.getXsSimpleType(), att.getTypeName(), null);
        } else if (item instanceof SchemaElementSimple) {
            SchemaElementSimple simple = (SchemaElementSimple) item;
            Class returnType = SchemaUtil.toReturnType(simple.getXsSimpleType(), simple.getTypeName(), simple.getFractionDigits());
            if (simple.isArray()) {
                returnType = Array.newInstance(returnType, 0).getClass();
            }
            return returnType;
        } else if (item instanceof SchemaElementComplex) {
            SchemaElementComplex complex = (SchemaElementComplex) item;
            if (complex.getOptionalSimpleType() != null) {
                return SchemaUtil.toReturnType(complex.getOptionalSimpleType(), complex.getOptionalSimpleTypeName(), null);
            }
            if (complex.isArray()) {
                return NodeList.class;
            }
            return Node.class;
        } else {
            throw new PropertyAccessException("Invalid schema return type:" + item);
        }
    }

    /**
     * Returns the type for a give short type and type name.
     *
     * @param xsType                 XSSimplyType type
     * @param typeName               type name in XML standard
     * @param optionalFractionDigits fraction digits if any are defined
     * @return equivalent native type
     */
    public static Class toReturnType(short xsType, String typeName, Integer optionalFractionDigits) {
        if (typeName != null) {
            Class result = typeMap.get(typeName);
            if (result != null) {
                return result;
            }
        }

        switch (xsType) {
            case XSSimpleType.PRIMITIVE_BOOLEAN:
                return Boolean.class;
            case XSSimpleType.PRIMITIVE_STRING:
                return String.class;
            case XSSimpleType.PRIMITIVE_DECIMAL:
                if ((optionalFractionDigits != null) && (optionalFractionDigits > 0)) {
                    return Double.class;
                }
                return Integer.class;
            case XSSimpleType.PRIMITIVE_FLOAT:
                return Float.class;
            case XSSimpleType.PRIMITIVE_DOUBLE:
                return Double.class;
            default:
                return String.class;
        }
    }

    /**
     * Returns the native type based on XPathConstants qname and an optional cast-to type, if provided.
     *
     * @param resultType         qname
     * @param optionalCastToType null or cast-to type
     * @return return type
     */
    public static Class toReturnType(QName resultType, Class optionalCastToType) {
        if (optionalCastToType != null) {
            return optionalCastToType;
        }

        if (resultType.equals(XPathConstants.NODESET))
            return NodeList.class;
        if (resultType.equals(XPathConstants.NODE))
            return Node.class;
        if (resultType.equals(XPathConstants.BOOLEAN))
            return Boolean.class;
        if (resultType.equals(XPathConstants.NUMBER))
            return Double.class;
        if (resultType.equals(XPathConstants.STRING))
            return String.class;

        return String.class;
    }

    /**
     * Returns the XPathConstants type for a given Xerces type definition.
     *
     * @param type is the type
     * @return XPathConstants type
     */
    public static QName simpleTypeToQName(short type) {
        switch (type) {
            case XSSimpleType.PRIMITIVE_BOOLEAN:
                return XPathConstants.BOOLEAN;
            case XSSimpleType.PRIMITIVE_DOUBLE:
                return XPathConstants.NUMBER;
            case XSSimpleType.PRIMITIVE_STRING:
                return XPathConstants.STRING;
            case XSSimpleType.PRIMITIVE_DECIMAL:
                return XPathConstants.NUMBER;
            case XSSimpleType.PRIMITIVE_FLOAT:
                return XPathConstants.NUMBER;
            case XSSimpleType.PRIMITIVE_DATETIME:
                return XPathConstants.STRING;
            case XSSimpleType.PRIMITIVE_DATE:
                return XPathConstants.STRING;
            case XSSimpleType.PRIMITIVE_TIME:
                return XPathConstants.STRING;
            default:
                throw new EPException("Unexpected schema simple type encountered '" + type + "'");
        }
    }

    /**
     * Returns the root element for a given schema given a root element name and namespace.
     *
     * @param schema      is the schema to interrogate
     * @param namespace   is the namespace of the root element
     * @param elementName is the name of the root element
     * @return declaration of root element
     */
    public static SchemaElementComplex findRootElement(SchemaModel schema, String namespace, String elementName) {
        if ((namespace != null) && namespace.length() != 0) {
            for (SchemaElementComplex complexElement : schema.getComponents()) {
                if ((complexElement.getNamespace().equals(namespace)) && (complexElement.getName().equals(elementName))) {
                    return complexElement;
                }
            }
        } else {
            for (SchemaElementComplex complexElement : schema.getComponents()) {
                if (complexElement.getName().equals(elementName)) {
                    return complexElement;
                }
            }
        }

        if (elementName.startsWith("//")) {
            elementName = elementName.substring(2);
            for (SchemaElementComplex complexElement : schema.getComponents()) {
                SchemaElementComplex match = recursiveDeepMatch(complexElement, namespace, elementName);
                if (match != null) {
                    return match;
                }
            }
        }

        String text = "Could not find root element declaration in schema for element name '" + elementName + '\'';
        if (namespace != null) {
            text = text + " in namespace '" + namespace + '\'';
        }
        throw new EPException(text);
    }

    private static SchemaElementComplex recursiveDeepMatch(SchemaElementComplex parent, String namespace, String elementName) {
        if ((namespace != null) && namespace.length() != 0) {
            for (SchemaElementComplex complexElement : parent.getChildren()) {
                if ((complexElement.getNamespace().equals(namespace)) && (complexElement.getName().equals(elementName))) {
                    return complexElement;
                }
            }
        } else {
            for (SchemaElementComplex complexElement : parent.getChildren()) {
                if (complexElement.getName().equals(elementName)) {
                    return complexElement;
                }
            }
        }

        for (SchemaElementComplex complexElement : parent.getChildren()) {
            SchemaElementComplex found = recursiveDeepMatch(complexElement, namespace, elementName);
            if (found != null) {
                return found;
            }
        }

        return null;
    }


    /**
     * Finds an apropiate definition for the given property, starting at the * given definition.
     * First look if the property es an attribute. If not, look at simple and then child element
     * definitions.
     *
     * @param def      the definition to start looking
     * @param property the property to look for
     * @return schema element or null if not found
     */
    public static SchemaItem findPropertyMapping(SchemaElementComplex def, String property) {

        for (SchemaItemAttribute attribute : def.getAttributes()) {
            if (attribute.getName().equals(property)) {
                return attribute;
            }
        }

        for (SchemaElementSimple simple : def.getSimpleElements()) {
            if (simple.getName().equals(property)) {
                return simple;
            }
        }

        for (SchemaElementComplex complex : def.getChildren()) {
            if (complex.getName().equals(property)) {
                return complex;
            }
        }

        //property not found in schema
        return null;
    }

    /**
     * Serialize the given node.
     *
     * @param doc node to serialize
     * @return serialized node string
     */
    public static String serialize(Node doc) {
        StringWriter writer = new StringWriter();
        try {
            serializeNode(doc, "", writer);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        writer.flush();
        return writer.toString();
    }

    private static void serializeNode(Node node, String
            indentLevel, StringWriter writer) throws IOException {
        switch (node.getNodeType()) {
            case Node.DOCUMENT_NODE:
                Document doc = (Document) node;
                writer.write("<?xml version=\"");
                writer.write(doc.getXmlVersion());
                writer.write("\" encoding=\"UTF-8\" standalone=\"");
                if (doc.getXmlStandalone()) {
                    writer.write("yes");
                } else {
                    writer.write("no");
                }
                writer.write("\"?>\n");

                NodeList nodes = node.getChildNodes();
                if (nodes != null) {
                    for (int i = 0; i < nodes.getLength(); i++) {
                        serializeNode(nodes.item(i), "", writer);
                    }
                }
                break;
            case Node.ELEMENT_NODE:
                String name = node.getNodeName();
                writer.write(indentLevel + "<" + name);
                NamedNodeMap attributes = node.getAttributes();
                for (int i = 0; i < attributes.getLength(); i++) {
                    Node current = attributes.item(i);
                    writer.write(" " + current.getNodeName() + "=\"");
                    print(current.getNodeValue(), writer);
                    writer.write("\"");
                }
                writer.write(">");

                NodeList children = node.getChildNodes();
                if (children != null) {
                    if ((children.item(0) != null) && (children.item(0).getNodeType() == Node.ELEMENT_NODE)) {
                        writer.write("\n");
                    }

                    for (int i = 0; i < children.getLength(); i++) {
                        serializeNode(children.item(i), indentLevel, writer);
                    }

                    if ((children.item(0) != null)
                            && (children.item(children.getLength() - 1).getNodeType() == Node.ELEMENT_NODE)) {
                        writer.write(indentLevel);
                    }
                }

                writer.write("</" + name + ">\n");
                break;
            case Node.TEXT_NODE:
                print(node.getNodeValue(), writer);
                break;
            case Node.CDATA_SECTION_NODE:
                writer.write("CDATA");
                print(node.getNodeValue(), writer);
                writer.write("");
                break;
            case Node.COMMENT_NODE:
                writer.write(indentLevel + "<!-- " + node.getNodeValue() + " -->\n");
                break;
            case Node.PROCESSING_INSTRUCTION_NODE:
                writer.write("<?" + node.getNodeName() + " " + node.getNodeValue() + "?>\n");
                break;
            case Node.ENTITY_REFERENCE_NODE:
                writer.write("&" + node.getNodeName() + ";");
                break;
            case Node.DOCUMENT_TYPE_NODE:
                DocumentType docType = (DocumentType) node;
                String publicId = docType.getPublicId();
                String systemId = docType.getSystemId();
                String internalSubset = docType.getInternalSubset();
                writer.write("<!DOCTYPE " + docType.getName());
                if (publicId != null) {
                    writer.write(" PUBLIC \"" + publicId + "\" ");
                } else {
                    writer.write(" SYSTEM ");
                }
                writer.write("\"" + systemId + "\"");
                if (internalSubset != null) {
                    writer.write(" [" + internalSubset + "]");
                }
                writer.write(">\n");
                break;
            default:
                break;
        }
    }

    private static void print(String s, Writer writer) throws IOException {
        if (s == null) {
            return;
        }
        for (int i = 0, len = s.length(); i < len; i++) {
            char c = s.charAt(i);
            switch (c) {
                case '<':
                    writer.write("&lt;");
                    break;
                case '>':
                    writer.write("&gt;");
                    break;
                case '&':
                    writer.write("&amp;");
                    break;
                case '\r':
                    writer.write("&#xD;");
                    break;
                default:
                    writer.write(c);
            }
        }
    }

}

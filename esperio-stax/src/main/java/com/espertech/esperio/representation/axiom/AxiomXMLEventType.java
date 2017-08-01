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
package com.espertech.esperio.representation.axiom;

import com.espertech.esper.client.*;
import com.espertech.esper.epl.generated.EsperEPL2GrammarParser;
import com.espertech.esper.event.*;
import com.espertech.esper.event.property.PropertyParser;
import com.espertech.esper.event.xml.SimpleXMLPropertyParser;
import org.apache.axiom.om.OMNode;
import org.apache.axiom.om.xpath.AXIOMXPath;
import org.jaxen.JaxenException;

import javax.xml.namespace.QName;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import java.util.*;

/**
 * Apache Axiom event type provides event metadata for Axiom OMDocument events.
 * <p>
 * Optimistic try to resolve the property string into an appropiate xPath, and
 * use it as getter. Mapped and Indexed properties supported. Because no type
 * information is given, all property are resolved to String. No namespace
 * support. Cannot access to xml attributes, only elements content.
 * <p>
 * See {@link AxiomEventRepresentation} for more details.
 */
public class AxiomXMLEventType implements EventTypeSPI {
    private EventTypeMetadata metadata;
    private int eventTypeId;
    private String defaultNamespacePrefix;
    private ConfigurationEventTypeAxiom config;
    private AxiomXPathNamespaceContext namespaceContext;
    private Map<String, TypedEventPropertyGetter> propertyGetterCache;
    private Map<String, EventPropertyDescriptor> propertyDescriptorsMap;
    private EventPropertyDescriptor[] propertyDescriptors;

    public AxiomXMLEventType(EventTypeMetadata metadata, int eventTypeId, ConfigurationEventTypeAxiom configurationEventTypeAxiom) {
        this.metadata = metadata;
        this.eventTypeId = eventTypeId;
        this.config = configurationEventTypeAxiom;
        this.propertyGetterCache = new HashMap<String, TypedEventPropertyGetter>();

        // Set up a namespace context for XPath expressions
        namespaceContext = new AxiomXPathNamespaceContext();
        for (Map.Entry<String, String> entry : configurationEventTypeAxiom.getNamespacePrefixes().entrySet()) {
            namespaceContext.addPrefix(entry.getKey(), entry.getValue());
        }

        // add namespaces
        if (configurationEventTypeAxiom.getDefaultNamespace() != null) {
            String defaultNamespace = configurationEventTypeAxiom.getDefaultNamespace();
            namespaceContext.setDefaultNamespace(defaultNamespace);

            // determine a default namespace prefix to use to construct XPath
            // expressions from pure property names
            defaultNamespacePrefix = null;
            for (Map.Entry<String, String> entry : configurationEventTypeAxiom.getNamespacePrefixes().entrySet()) {
                if (entry.getValue().equals(defaultNamespace)) {
                    defaultNamespacePrefix = entry.getKey();
                    break;
                }
            }
        }

        // determine XPath properties that are predefined
        propertyDescriptorsMap = new HashMap<String, EventPropertyDescriptor>();
        List<EventPropertyDescriptor> descriptors = new ArrayList<EventPropertyDescriptor>();
        String xpathExpression = null;
        try {
            for (ConfigurationEventTypeAxiom.XPathPropertyDesc property : config.getXPathProperties().values()) {
                TypedEventPropertyGetter getter = resolvePropertyGetter(property.getName(), property.getXpath(), property.getType(), property.getOptionalCastToType());
                propertyGetterCache.put(property.getName(), getter);
                EventPropertyDescriptor desc = new EventPropertyDescriptor(property.getName(), getter.getResultClass(), null, false, false, false, false, false);
                propertyDescriptorsMap.put(property.getName(), desc);
                descriptors.add(desc);
            }
        } catch (XPathExpressionException ex) {
            throw new EPException("XPath expression could not be compiled for expression '" + xpathExpression + '\'', ex);
        }
        propertyDescriptors = descriptors.toArray(new EventPropertyDescriptor[descriptors.size()]);
    }

    public int getEventTypeId() {
        return eventTypeId;
    }

    public Class getPropertyType(String property) {
        TypedEventPropertyGetter getter = propertyGetterCache.get(property);
        if (getter != null)
            return getter.getResultClass();
        return String.class;    // all other types are assumed to exist and be of type String
    }

    public Class getUnderlyingType() {
        return OMNode.class;
    }

    public EventPropertyGetterSPI getGetterSPI(String property) {
        EventPropertyGetterSPI getter = propertyGetterCache.get(property);
        if (getter != null)
            return getter;
        try {
            return resolveDynamicProperty(property);
        } catch (XPathExpressionException e) {
            return null;
        }
    }

    public EventPropertyGetterSPI getGetter(String property) {
        return getGetterSPI(property);
    }

    public String[] getPropertyNames() {
        Set<String> properties = propertyGetterCache.keySet();
        return properties.toArray(new String[properties.size()]);
    }

    public boolean isProperty(String property) {
        return getGetter(property) != null;
    }

    public EventType[] getSuperTypes() {
        return null;
    }

    public Iterator<EventType> getDeepSuperTypes() {
        return null;
    }

    public String getStartTimestampPropertyName() {
        return null;
    }

    public String getEndTimestampPropertyName() {
        return null;
    }

    /**
     * Returns the configuration for the name.
     *
     * @return configuration details underlying the type
     */
    public ConfigurationEventTypeAxiom getConfig() {
        return config;
    }

    public EventTypeMetadata getMetadata() {
        return metadata;
    }

    private TypedEventPropertyGetter resolveDynamicProperty(String property) throws XPathExpressionException {
        // not defined, come up with an XPath
        EsperEPL2GrammarParser.StartEventPropertyRuleContext ast = PropertyParser.parse(property);
        String xPathExpr = SimpleXMLPropertyParser.walk(ast, property, config.getRootElementName(), defaultNamespacePrefix, config.isResolvePropertiesAbsolute());
        return resolvePropertyGetter(property, xPathExpr, XPathConstants.STRING, null);
    }

    private TypedEventPropertyGetter resolvePropertyGetter(String propertyName, String xPathExpr, QName type, Class optionalCastToType) throws XPathExpressionException {
        AXIOMXPath axXPath;
        try {
            axXPath = new AXIOMXPath(xPathExpr);
        } catch (JaxenException e) {
            throw new EPException("Error constructing XPath expression from property name '" + propertyName + '\'', e);
        }

        axXPath.setNamespaceContext(namespaceContext);
        return new AxiomXPathPropertyGetter(propertyName, axXPath, type, optionalCastToType);
    }

    public String getName() {
        return metadata.getPublicName();
    }

    public FragmentEventType getFragmentType(String propertyExpression) {
        return null;  // Does not allow fragments
    }

    public EventPropertyDescriptor[] getPropertyDescriptors() {
        return propertyDescriptors;
    }

    public EventPropertyDescriptor getPropertyDescriptor(String propertyName) {
        return propertyDescriptorsMap.get(propertyName);
    }

    public EventPropertyWriter getWriter(String propertyName) {
        return null;
    }

    public EventPropertyDescriptor[] getWriteableProperties() {
        return new EventPropertyDescriptor[0];
    }

    public EventBeanCopyMethod getCopyMethod(String[] properties) {
        return null;
    }

    public EventPropertyDescriptor getWritableProperty(String propertyName) {
        return null;
    }

    public EventBeanWriter getWriter(String[] properties) {
        return null;
    }

    public EventBeanReader getReader() {
        return null;
    }

    public EventPropertyGetterMapped getGetterMapped(String mappedPropertyName) {
        return null;
    }

    public EventPropertyGetterMappedSPI getGetterMappedSPI(String propertyName) {
        return null;
    }

    public EventPropertyGetterIndexed getGetterIndexed(String indexedPropertyName) {
        return null;
    }

    public EventPropertyGetterIndexedSPI getGetterIndexedSPI(String propertyName) {
        return null;
    }

    public boolean equalsCompareType(EventType eventType) {
        return this == eventType;
    }
}

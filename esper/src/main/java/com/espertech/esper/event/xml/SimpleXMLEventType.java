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

import com.espertech.esper.client.ConfigurationEventTypeXMLDOM;
import com.espertech.esper.client.EPException;
import com.espertech.esper.client.FragmentEventType;
import com.espertech.esper.epl.generated.EsperEPL2GrammarParser;
import com.espertech.esper.event.EventAdapterService;
import com.espertech.esper.event.EventPropertyGetterSPI;
import com.espertech.esper.event.EventTypeMetadata;
import com.espertech.esper.event.property.Property;
import com.espertech.esper.event.property.PropertyParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.namespace.QName;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Optimistic try to resolve the property string into an appropiate xPath,
 * and use it as getter.
 * Mapped and Indexed properties supported.
 * Because no type information is given, all property are resolved to String.
 * No namespace support.
 * Cannot access to xml attributes, only elements content.
 * <p>
 * If an xsd is present, then use {@link com.espertech.esper.event.xml.SchemaXMLEventType SchemaXMLEventType }
 *
 * @author pablo
 */
public class SimpleXMLEventType extends BaseXMLEventType {

    private static final Logger log = LoggerFactory.getLogger(SimpleXMLEventType.class);
    private final Map<String, EventPropertyGetterSPI> propertyGetterCache;
    private String defaultNamespacePrefix;
    private final boolean isResolvePropertiesAbsolute;

    /**
     * Ctor.
     *
     * @param configurationEventTypeXMLDOM configures the event type
     * @param eventTypeMetadata            event type metadata
     * @param eventAdapterService          for type looking and registration
     * @param eventTypeId                  type id
     */
    public SimpleXMLEventType(EventTypeMetadata eventTypeMetadata, int eventTypeId, ConfigurationEventTypeXMLDOM configurationEventTypeXMLDOM, EventAdapterService eventAdapterService) {
        super(eventTypeMetadata, eventTypeId, configurationEventTypeXMLDOM, eventAdapterService);
        isResolvePropertiesAbsolute = configurationEventTypeXMLDOM.isXPathResolvePropertiesAbsolute();
        propertyGetterCache = new HashMap<String, EventPropertyGetterSPI>();

        // Set of namespace context for XPath expressions
        XPathNamespaceContext xPathNamespaceContext = new XPathNamespaceContext();
        for (Map.Entry<String, String> entry : configurationEventTypeXMLDOM.getNamespacePrefixes().entrySet()) {
            xPathNamespaceContext.addPrefix(entry.getKey(), entry.getValue());
        }
        if (configurationEventTypeXMLDOM.getDefaultNamespace() != null) {
            String defaultNamespace = configurationEventTypeXMLDOM.getDefaultNamespace();
            xPathNamespaceContext.setDefaultNamespace(defaultNamespace);

            // determine a default namespace prefix to use to construct XPath expressions from pure property names
            defaultNamespacePrefix = null;
            for (Map.Entry<String, String> entry : configurationEventTypeXMLDOM.getNamespacePrefixes().entrySet()) {
                if (entry.getValue().equals(defaultNamespace)) {
                    defaultNamespacePrefix = entry.getKey();
                    break;
                }
            }
        }
        super.setNamespaceContext(xPathNamespaceContext);
        super.initialize(configurationEventTypeXMLDOM.getXPathProperties().values(), Collections.EMPTY_LIST);
    }

    protected Class doResolvePropertyType(String propertyExpression) {
        EsperEPL2GrammarParser.StartEventPropertyRuleContext ast = PropertyParser.parse(propertyExpression);
        if (PropertyParser.isPropertyDynamic(ast)) {
            return org.w3c.dom.Node.class;
        } else {
            return String.class;
        }
    }

    protected EventPropertyGetterSPI doResolvePropertyGetter(String propertyExpression) {
        EventPropertyGetterSPI getter = propertyGetterCache.get(propertyExpression);
        if (getter != null) {
            return getter;
        }

        if (!this.getConfigurationEventTypeXMLDOM().isXPathPropertyExpr()) {
            Property prop = PropertyParser.parseAndWalkLaxToSimple(propertyExpression);
            getter = prop.getGetterDOM();
            if (!prop.isDynamic()) {
                getter = new DOMConvertingGetter((DOMPropertyGetter) getter, String.class);
            }
        } else {
            XPathExpression xPathExpression;
            String xPathExpr;
            boolean isDynamic;
            try {
                EsperEPL2GrammarParser.StartEventPropertyRuleContext ast = PropertyParser.parse(propertyExpression);
                isDynamic = PropertyParser.isPropertyDynamic(ast);

                xPathExpr = SimpleXMLPropertyParser.walk(ast, propertyExpression, getRootElementName(), defaultNamespacePrefix, isResolvePropertiesAbsolute);
                XPath xpath = getXPathFactory().newXPath();
                xpath.setNamespaceContext(namespaceContext);
                if (log.isInfoEnabled()) {
                    log.info("Compiling XPath expression for property '" + propertyExpression + "' as '" + xPathExpr + "'");
                }
                xPathExpression = xpath.compile(xPathExpr);
            } catch (XPathExpressionException e) {
                throw new EPException("Error constructing XPath expression from property name '" + propertyExpression + '\'', e);
            }

            QName xPathReturnType;
            if (isDynamic) {
                xPathReturnType = XPathConstants.NODE;
            } else {
                xPathReturnType = XPathConstants.STRING;
            }
            getter = new XPathPropertyGetter(propertyExpression, xPathExpr, xPathExpression, xPathReturnType, null, null);
        }

        // no fragment factory, fragments not allowed
        propertyGetterCache.put(propertyExpression, getter);
        return getter;
    }

    protected FragmentEventType doResolveFragmentType(String property) {
        return null;  // Since we have no type information, the fragments are not allowed unless explicitly configured via XPath getter
    }
}

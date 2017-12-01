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
import com.espertech.esper.client.EventPropertyDescriptor;
import com.espertech.esper.client.EventType;
import com.espertech.esper.client.FragmentEventType;
import com.espertech.esper.event.EventAdapterService;
import com.espertech.esper.event.EventPropertyGetterSPI;
import com.espertech.esper.event.EventTypeMetadata;
import com.espertech.esper.event.ExplicitPropertyDescriptor;
import com.espertech.esper.event.property.IndexedProperty;
import com.espertech.esper.event.property.Property;
import com.espertech.esper.event.property.PropertyParser;
import com.espertech.esper.util.StringValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * EventType for xml events that have a Schema.
 * Mapped and Indexed properties are supported.
 * All property types resolved via the declared xsd types.
 * Can access attributes.
 * Validates the property string at construction time.
 *
 * @author pablo
 */
public class SchemaXMLEventType extends BaseXMLEventType {
    private static final Logger log = LoggerFactory.getLogger(SchemaXMLEventType.class);

    private final SchemaModel schemaModel;
    private final SchemaElementComplex schemaModelRoot;
    private final String rootElementNamespace;
    private final Map<String, EventPropertyGetterSPI> propertyGetterCache;
    private final boolean isPropertyExpressionXPath;

    /**
     * Ctor.
     *
     * @param config              - configuration for type
     * @param eventTypeMetadata   - event type metadata
     * @param schemaModel         - the schema representation
     * @param eventAdapterService - type lookup and registration
     * @param eventTypeId         type id
     */
    public SchemaXMLEventType(EventTypeMetadata eventTypeMetadata, int eventTypeId, ConfigurationEventTypeXMLDOM config, SchemaModel schemaModel, EventAdapterService eventAdapterService) {
        super(eventTypeMetadata, eventTypeId, config, eventAdapterService);

        this.propertyGetterCache = new HashMap<String, EventPropertyGetterSPI>();
        this.schemaModel = schemaModel;
        this.rootElementNamespace = config.getRootElementNamespace();
        this.schemaModelRoot = SchemaUtil.findRootElement(schemaModel, rootElementNamespace, this.getRootElementName());
        this.isPropertyExpressionXPath = config.isXPathPropertyExpr();

        // Set of namespace context for XPath expressions
        XPathNamespaceContext ctx = new XPathNamespaceContext();
        if (config.getDefaultNamespace() != null) {
            ctx.setDefaultNamespace(config.getDefaultNamespace());
        }
        for (Map.Entry<String, String> entry : config.getNamespacePrefixes().entrySet()) {
            ctx.addPrefix(entry.getKey(), entry.getValue());
        }
        super.setNamespaceContext(ctx);

        // add properties for the root element
        List<ExplicitPropertyDescriptor> additionalSchemaProps = new ArrayList<ExplicitPropertyDescriptor>();

        // Add a property for each complex child element
        for (SchemaElementComplex complex : schemaModelRoot.getChildren()) {
            String propertyName = complex.getName();
            Class returnType = Node.class;
            Class propertyComponentType = null;

            if (complex.getOptionalSimpleType() != null) {
                returnType = SchemaUtil.toReturnType(complex);
            }
            if (complex.isArray()) {
                returnType = Node[].class;      // We use Node[] for arrays and NodeList for XPath-Expressions returning Nodeset
                propertyComponentType = Node.class;
            }

            boolean isFragment = false;
            if (this.getConfigurationEventTypeXMLDOM().isAutoFragment() && (!this.getConfigurationEventTypeXMLDOM().isXPathPropertyExpr())) {
                isFragment = canFragment(complex);
            }

            EventPropertyGetterSPI getter = doResolvePropertyGetter(propertyName, true);
            EventPropertyDescriptor desc = new EventPropertyDescriptor(propertyName, returnType, propertyComponentType, false, false, complex.isArray(), false, isFragment);
            ExplicitPropertyDescriptor explicit = new ExplicitPropertyDescriptor(desc, getter, false, null);
            additionalSchemaProps.add(explicit);
        }

        // Add a property for each simple child element
        for (SchemaElementSimple simple : schemaModelRoot.getSimpleElements()) {
            String propertyName = simple.getName();
            Class returnType = SchemaUtil.toReturnType(simple);
            EventPropertyGetterSPI getter = doResolvePropertyGetter(propertyName, true);
            EventPropertyDescriptor desc = new EventPropertyDescriptor(propertyName, returnType, null, false, false, simple.isArray(), false, false);
            ExplicitPropertyDescriptor explicit = new ExplicitPropertyDescriptor(desc, getter, false, null);
            additionalSchemaProps.add(explicit);
        }

        // Add a property for each attribute
        for (SchemaItemAttribute attribute : schemaModelRoot.getAttributes()) {
            String propertyName = attribute.getName();
            Class returnType = SchemaUtil.toReturnType(attribute);
            EventPropertyGetterSPI getter = doResolvePropertyGetter(propertyName, true);
            EventPropertyDescriptor desc = new EventPropertyDescriptor(propertyName, returnType, null, false, false, false, false, false);
            ExplicitPropertyDescriptor explicit = new ExplicitPropertyDescriptor(desc, getter, false, null);
            additionalSchemaProps.add(explicit);
        }

        // Finally add XPath properties as that may depend on the rootElementNamespace
        super.initialize(config.getXPathProperties().values(), additionalSchemaProps);
    }

    public SchemaModel getSchemaModel() {
        return schemaModel;
    }

    protected FragmentEventType doResolveFragmentType(String property) {
        if ((!this.getConfigurationEventTypeXMLDOM().isAutoFragment()) || (this.getConfigurationEventTypeXMLDOM().isXPathPropertyExpr())) {
            return null;
        }

        Property prop = PropertyParser.parseAndWalkLaxToSimple(property);

        SchemaItem item = prop.getPropertyTypeSchema(schemaModelRoot, this.getEventAdapterService());
        if ((item == null) || (!canFragment(item))) {
            return null;
        }
        SchemaElementComplex complex = (SchemaElementComplex) item;

        // build name of event type
        String[] atomicProps = prop.toPropertyArray();
        String delimiterDot = ".";
        StringBuilder eventTypeNameBuilder = new StringBuilder(this.getName());
        for (String atomic : atomicProps) {
            eventTypeNameBuilder.append(delimiterDot);
            eventTypeNameBuilder.append(atomic);
        }
        String eventTypeName = eventTypeNameBuilder.toString();

        // check if the type exists, use the existing type if found
        EventType existingType = this.getEventAdapterService().getExistsTypeByName(eventTypeName);
        if (existingType != null) {
            return new FragmentEventType(existingType, complex.isArray(), false);
        }

        // add a new type
        ConfigurationEventTypeXMLDOM xmlDom = new ConfigurationEventTypeXMLDOM();
        xmlDom.setRootElementName("//" + complex.getName());    // such the reload of the type can resolve it
        xmlDom.setRootElementNamespace(complex.getNamespace());
        xmlDom.setAutoFragment(this.getConfigurationEventTypeXMLDOM().isAutoFragment());
        xmlDom.setEventSenderValidatesRoot(this.getConfigurationEventTypeXMLDOM().isEventSenderValidatesRoot());
        xmlDom.setXPathPropertyExpr(this.getConfigurationEventTypeXMLDOM().isXPathPropertyExpr());
        xmlDom.setXPathResolvePropertiesAbsolute(this.getConfigurationEventTypeXMLDOM().isXPathResolvePropertiesAbsolute());
        xmlDom.setSchemaResource(this.getConfigurationEventTypeXMLDOM().getSchemaResource());
        xmlDom.setSchemaText(this.getConfigurationEventTypeXMLDOM().getSchemaText());
        xmlDom.setXPathFunctionResolver(this.getConfigurationEventTypeXMLDOM().getXPathFunctionResolver());
        xmlDom.setXPathVariableResolver(this.getConfigurationEventTypeXMLDOM().getXPathVariableResolver());
        xmlDom.setDefaultNamespace(this.getConfigurationEventTypeXMLDOM().getDefaultNamespace());
        xmlDom.addNamespacePrefixes(this.getConfigurationEventTypeXMLDOM().getNamespacePrefixes());

        EventType newType;
        try {
            newType = this.getEventAdapterService().addXMLDOMType(eventTypeName, xmlDom, schemaModel, true);
        } catch (Exception ex) {
            log.error("Failed to add dynamic event type for fragment of XML schema for property '" + property + "' :" + ex.getMessage(), ex);
            return null;
        }
        return new FragmentEventType(newType, complex.isArray(), false);
    }

    protected Class doResolvePropertyType(String propertyExpression) {
        return doResolvePropertyType(propertyExpression, false);
    }

    private Class doResolvePropertyType(String propertyExpression, boolean allowSimpleProperties) {

        // see if this is an indexed property
        int index = StringValue.unescapedIndexOfDot(propertyExpression);
        if ((!allowSimpleProperties) && (index == -1)) {
            // parse, can be an indexed property
            Property property = PropertyParser.parseAndWalkLaxToSimple(propertyExpression);
            if (!property.isDynamic()) {
                if (!(property instanceof IndexedProperty)) {
                    return null;
                }
                IndexedProperty indexedProp = (IndexedProperty) property;
                EventPropertyDescriptor descriptor = propertyDescriptorMap.get(indexedProp.getPropertyNameAtomic());
                if (descriptor == null) {
                    return null;
                }
                return descriptor.getPropertyType();
            }
        }

        Property prop = PropertyParser.parseAndWalkLaxToSimple(propertyExpression);
        if (prop.isDynamic()) {
            return Node.class;
        }

        SchemaItem item = prop.getPropertyTypeSchema(schemaModelRoot, this.getEventAdapterService());
        if (item == null) {
            return null;
        }

        return SchemaUtil.toReturnType(item);
    }

    protected EventPropertyGetterSPI doResolvePropertyGetter(String property) {
        return doResolvePropertyGetter(property, false);
    }

    private EventPropertyGetterSPI doResolvePropertyGetter(String propertyExpression, boolean allowSimpleProperties) {
        EventPropertyGetterSPI getter = propertyGetterCache.get(propertyExpression);
        if (getter != null) {
            return getter;
        }

        if (!allowSimpleProperties) {
            // see if this is an indexed property
            int index = StringValue.unescapedIndexOfDot(propertyExpression);
            if (index == -1) {
                // parse, can be an indexed property
                Property property = PropertyParser.parseAndWalkLaxToSimple(propertyExpression);
                if (!property.isDynamic()) {
                    if (!(property instanceof IndexedProperty)) {
                        return null;
                    }
                    IndexedProperty indexedProp = (IndexedProperty) property;
                    getter = this.propertyGetters.get(indexedProp.getPropertyNameAtomic());
                    if (null == getter) {
                        return null;
                    }
                    EventPropertyDescriptor descriptor = this.propertyDescriptorMap.get(indexedProp.getPropertyNameAtomic());
                    if (descriptor == null) {
                        return null;
                    }
                    if (!descriptor.isIndexed()) {
                        return null;
                    }
                    if (descriptor.getPropertyType() == NodeList.class) {
                        FragmentFactory fragmentFactory = new FragmentFactoryDOMGetter(this.getEventAdapterService(), this, indexedProp.getPropertyNameAtomic());
                        return new XPathPropertyArrayItemGetter(getter, indexedProp.getIndex(), fragmentFactory);
                    }
                }
            }
        }

        if (!isPropertyExpressionXPath) {
            Property prop = PropertyParser.parseAndWalkLaxToSimple(propertyExpression);
            boolean isDynamic = prop.isDynamic();

            if (!isDynamic) {
                SchemaItem item = prop.getPropertyTypeSchema(schemaModelRoot, this.getEventAdapterService());
                if (item == null) {
                    return null;
                }

                getter = prop.getGetterDOM(schemaModelRoot, this.getEventAdapterService(), this, propertyExpression);
                if (getter == null) {
                    return null;
                }

                Class returnType = SchemaUtil.toReturnType(item);
                if ((returnType != Node.class) && (returnType != NodeList.class)) {
                    if (!returnType.isArray()) {
                        getter = new DOMConvertingGetter((DOMPropertyGetter) getter, returnType);
                    } else {
                        getter = new DOMConvertingArrayGetter((DOMPropertyGetter) getter, returnType.getComponentType());
                    }
                }
            } else {
                return prop.getGetterDOM();
            }
        } else {
            boolean allowFragments = !this.getConfigurationEventTypeXMLDOM().isXPathPropertyExpr();
            getter = SchemaXMLPropertyParser.getXPathResolution(propertyExpression, getXPathFactory(), getRootElementName(), rootElementNamespace, schemaModel, this.getEventAdapterService(), this, allowFragments, this.getConfigurationEventTypeXMLDOM().getDefaultNamespace());
        }

        propertyGetterCache.put(propertyExpression, getter);
        return getter;
    }

    private boolean canFragment(SchemaItem item) {
        if (!(item instanceof SchemaElementComplex)) {
            return false;
        }

        SchemaElementComplex complex = (SchemaElementComplex) item;
        if (complex.getOptionalSimpleType() != null) {
            return false;    // no transposing if the complex type also has a simple value else that is hidden
        }

        return true;
    }
}

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

import com.espertech.esper.event.EventTypeMetadata;
import com.espertech.esper.plugin.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Plug-in event representation for Apache AXIOM.
 * <p>
 * Allows direct querying of org.apache.axiom.OMDocument and org.apache.axiom.om.OMNode objects via
 * properties that are translated into XPath.
 */
public class AxiomEventRepresentation implements PlugInEventRepresentation {
    private Map<String, AxiomXMLEventType> types = new HashMap<String, AxiomXMLEventType>();

    public void init(PlugInEventRepresentationContext eventRepresentationContext) {
    }

    public boolean acceptsType(PlugInEventTypeHandlerContext acceptTypeContext) {
        Object initValue = acceptTypeContext.getTypeInitializer();
        if (initValue instanceof String) {
            String xml = (String) initValue;
            return xml.contains("xml-axiom");
        }
        return initValue instanceof ConfigurationEventTypeAxiom;
    }

    public PlugInEventTypeHandler getTypeHandler(PlugInEventTypeHandlerContext eventTypeContext) {
        ConfigurationEventTypeAxiom config;

        Object initValue = eventTypeContext.getTypeInitializer();
        if (initValue instanceof String) {
            String xml = (String) initValue;
            config = AxiomConfigurationParserXML.parse(xml);
        } else {
            config = (ConfigurationEventTypeAxiom) eventTypeContext.getTypeInitializer();
        }

        EventTypeMetadata metadata = EventTypeMetadata.createXMLType(config.getRootElementName(), true, false);
        AxiomXMLEventType eventType = new AxiomXMLEventType(metadata, eventTypeContext.getEventTypeId(), config);
        types.put(config.getRootElementName(), eventType);  // keep a handle on the types created to allow dynamic event reflection via bean factory
        return new AxiomEventTypeHandler(eventType);
    }

    public boolean acceptsEventBeanResolution(PlugInEventBeanReflectorContext context) {
        return true;
    }

    public PlugInEventBeanFactory getEventBeanFactory(PlugInEventBeanReflectorContext uri) {
        return new AxionEventBeanFactory(types);
    }
}

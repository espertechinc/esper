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
package com.espertech.esper.event.bean;

import com.espertech.esper.client.ConfigurationEventTypeLegacy;

/**
 * Factory for creates a builder/introspector for determining event property descriptors
 * based on a given class.
 */
public class PropertyListBuilderFactory {
    /**
     * Creates an implementation for a builer considering the accessor style and
     * code generation flags for a given class.
     *
     * @param optionalLegacyClassConfigs configures how event property listy is build
     * @return builder/introspector implementation
     */
    public static PropertyListBuilder createBuilder(ConfigurationEventTypeLegacy optionalLegacyClassConfigs) {
        if (optionalLegacyClassConfigs == null) {
            return new PropertyListBuilderJavaBean(null);
        }
        if (optionalLegacyClassConfigs.getAccessorStyle() == ConfigurationEventTypeLegacy.AccessorStyle.JAVABEAN) {
            return new PropertyListBuilderJavaBean(optionalLegacyClassConfigs);
        }
        if (optionalLegacyClassConfigs.getAccessorStyle() == ConfigurationEventTypeLegacy.AccessorStyle.EXPLICIT) {
            return new PropertyListBuilderExplicit(optionalLegacyClassConfigs);
        }
        if (optionalLegacyClassConfigs.getAccessorStyle() == ConfigurationEventTypeLegacy.AccessorStyle.PUBLIC) {
            return new PropertyListBuilderPublic(optionalLegacyClassConfigs);
        }
        throw new IllegalArgumentException("Cannot match accessor style to property list builder");
    }
}

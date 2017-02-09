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

import com.espertech.esper.client.Configuration;

/**
 * Interface for a factory for obtaining {@link BeanEventType} instances.
 */
public interface BeanEventTypeFactory {
    /**
     * Returns the bean event type for a given class assigning the given name.
     *
     * @param name                  is the name
     * @param clazz                 is the class for which to generate an event type
     * @param isConfigured          if the class is a configuration value, false if discovered
     * @param isPreconfigured       if configured before use
     * @param isPreconfiguredStatic if from static engine config
     * @return is the event type for the class
     */
    public BeanEventType createBeanType(String name, Class clazz, boolean isPreconfiguredStatic, boolean isPreconfigured, boolean isConfigured);

    /**
     * Returns the bean event type for a given class assigning the given name.
     *
     * @param clazz is the class for which to generate an event type
     * @return is the event type for the class
     */
    public BeanEventType createBeanTypeDefaultName(Class clazz);

    /**
     * Returns the default property resolution style.
     *
     * @return property resolution style
     */
    public Configuration.PropertyResolutionStyle getDefaultPropertyResolutionStyle();

    public BeanEventType[] getCachedTypes();
}

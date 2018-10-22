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
package com.espertech.esper.common.client.configuration.runtime;

import com.espertech.esper.common.client.hook.condition.ConditionHandlerFactory;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Configuration object for defining condition handling behavior.
 */
public class ConfigurationRuntimeConditionHandling implements Serializable {
    private static final long serialVersionUID = -708367341332718634L;
    private List<String> handlerFactories;

    /**
     * Returns the list of condition handler factory class names,
     * see {@link ConditionHandlerFactory}
     *
     * @return list of fully-qualified class names
     */
    public List<String> getHandlerFactories() {
        return handlerFactories;
    }

    /**
     * Add an condition handler factory class name.
     * <p>
     * Provide a fully-qualified class name of the implementation
     * of the {@link ConditionHandlerFactory}
     * interface.
     *
     * @param className class name of condition handler factory
     */
    public void addClass(String className) {
        if (handlerFactories == null) {
            handlerFactories = new ArrayList<String>();
        }
        handlerFactories.add(className);
    }

    /**
     * Add a list of condition handler class names.
     *
     * @param classNames to add
     */
    public void addClasses(List<String> classNames) {
        if (handlerFactories == null) {
            handlerFactories = new ArrayList<String>();
        }
        handlerFactories.addAll(classNames);
    }

    /**
     * Add an condition handler factory class.
     * <p>
     * The class provided should implement the
     * {@link ConditionHandlerFactory}
     * interface.
     *
     * @param clazz class of implementation
     */
    public void addClass(Class clazz) {
        addClass(clazz.getName());
    }
}

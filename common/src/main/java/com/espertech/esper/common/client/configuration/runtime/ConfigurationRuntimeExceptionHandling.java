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

import com.espertech.esper.common.client.hook.exception.ExceptionHandlerFactory;
import com.espertech.esper.common.client.util.UndeployRethrowPolicy;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Configuration object for defining exception handling behavior.
 */
public class ConfigurationRuntimeExceptionHandling implements Serializable {
    private static final long serialVersionUID = -708367341332718634L;
    private List<String> handlerFactories;
    private UndeployRethrowPolicy undeployRethrowPolicy = UndeployRethrowPolicy.WARN;

    /**
     * Returns the list of exception handler factory class names,
     * see {@link ExceptionHandlerFactory}
     *
     * @return list of fully-qualified class names
     */
    public List<String> getHandlerFactories() {
        return handlerFactories;
    }

    /**
     * Add an exception handler factory class name.
     * <p>
     * Provide a fully-qualified class name of the implementation
     * of the {@link ExceptionHandlerFactory}
     * interface.
     *
     * @param exceptionHandlerFactoryClassName class name of exception handler factory
     */
    public void addClass(String exceptionHandlerFactoryClassName) {
        if (handlerFactories == null) {
            handlerFactories = new ArrayList<String>();
        }
        handlerFactories.add(exceptionHandlerFactoryClassName);
    }

    /**
     * Add a list of exception handler class names.
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
     * Add an exception handler factory class.
     * <p>
     * The class provided should implement the
     * {@link ExceptionHandlerFactory}
     * interface.
     *
     * @param exceptionHandlerFactoryClass class of implementation
     */
    public void addClass(Class exceptionHandlerFactoryClass) {
        addClass(exceptionHandlerFactoryClass.getName());
    }

    /**
     * Returns the policy to instruct the runtime whether a module un-deploy rethrows runtime exceptions that are encountered
     * during the undeploy. By default we are logging exceptions.
     *
     * @return indicator
     */
    public UndeployRethrowPolicy getUndeployRethrowPolicy() {
        return undeployRethrowPolicy;
    }

    /**
     * Sets the policy to instruct the runtime whether a module un-deploy rethrows runtime exceptions that are encountered
     * during the undeploy for any statement that is undeployed. By default we are logging exceptions.
     *
     * @param undeployRethrowPolicy indicator
     */
    public void setUndeployRethrowPolicy(UndeployRethrowPolicy undeployRethrowPolicy) {
        this.undeployRethrowPolicy = undeployRethrowPolicy;
    }

}

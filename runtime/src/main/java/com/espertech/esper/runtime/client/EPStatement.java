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
package com.espertech.esper.runtime.client;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.util.StatementProperty;

import java.lang.annotation.Annotation;

/**
 * The statement is the means to attach callbacks to receive statement results (push, observer)
 * and to object current results using pull.
 */
public interface EPStatement extends EPListenable, EPIterable {
    /**
     * Returns the type of events the statement pushes to listeners or returns for iterator.
     *
     * @return event type of events the iterator returns and that listeners receive
     */
    EventType getEventType();

    /**
     * Returns statement annotations.
     * <p>
     * See the annotation {@link com.espertech.esper.common.client.annotation} package for
     * available annotations. Application can define their own annotations.
     * </p>
     *
     * @return annotations or a zero-length array if no annotaions have been specified.
     */
    Annotation[] getAnnotations();

    /**
     * Returns the statement name.
     *
     * @return statement name
     */
    String getName();

    /**
     * Returns the deployment id.
     *
     * @return deployment id
     */
    String getDeploymentId();

    /**
     * Returns a statement property value.
     *
     * @param field statement property value
     * @return property or null if not set
     */
    Object getProperty(StatementProperty field);

    /**
     * Returns true if the statement has been undeployed.
     *
     * @return true for undeployed statements, false for deployed statements.
     */
    boolean isDestroyed();

    /**
     * Attaches a subscriber to receive statement results,
     * or removes a previously set subscriber (by providing a null value).
     * <p>
     * Note: Requires the allow-subscriber compiler options.
     * </p>
     * Only a single subscriber may be set for a statement. If this method is invoked twice
     * any previously-set subscriber is no longer used.
     *
     * @param subscriber to attach, or null to remove the previously set subscriber
     * @throws EPSubscriberException if the subscriber does not provide the methods
     *                               needed to receive statement results
     */
    void setSubscriber(Object subscriber) throws EPSubscriberException;

    /**
     * Attaches a subscriber to receive statement results by calling the method with the provided method name,
     * or removes a previously set subscriber (by providing a null value).
     * <p>
     * Note: Requires the allow-subscriber compiler options.
     * </p>
     * Only a single subscriber may be set for a statement. If this method is invoked twice
     * any previously-set subscriber is no longer used.
     *
     * @param subscriber to attach, or null to remove the previously set subscriber
     * @param methodName the name of the method to invoke, or null for the "update" method
     * @throws EPSubscriberException if the subscriber does not provide the methods
     *                               needed to receive statement results
     */
    void setSubscriber(Object subscriber, String methodName) throws EPSubscriberException;

    /**
     * Returns the current subscriber instance that receives statement results.
     *
     * @return subscriber object, or null to indicate that no subscriber is attached
     */
    Object getSubscriber();

    /**
     * Returns the application defined user data object associated with the statement at compile time, or null if none
     * was supplied at time of statement compilation.
     * <p>
     * The <em>user object</em> is a single, unnamed field that is stored with every statement.
     * Applications may put arbitrary objects in this field or a null value.
     * <p>
     * User objects are passed at time of statement compilation via options.
     *
     * @return user object or null if none defined
     */
    Object getUserObjectCompileTime();

    /**
     * Returns the application defined user data object associated with the statement at deployment time, or null if none
     * was supplied at time of deployment.
     * <p>
     * The <em>user object</em> is a single, unnamed field that is stored with every statement.
     * Applications may put arbitrary objects in this field or a null value.
     * <p>
     * User objects are passed at time of deployment via options.
     *
     * @return user object or null if none defined
     */
    Object getUserObjectRuntime();
}

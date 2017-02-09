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
package com.espertech.esper.client;

/**
 * Factory for {@link EventBean} instances given an underlying event object.
 * <p>
 * Not transferable between engine instances.
 */
public interface EventBeanFactory {

    /**
     * Wraps the underlying event object.
     *
     * @param underlying event to wrap
     * @return event bean
     */
    public EventBean wrap(Object underlying);

    /**
     * Returns the type of the underlying class expected for successful wrapping.
     *
     * @return underlying type expected
     */
    public Class getUnderlyingType();
}

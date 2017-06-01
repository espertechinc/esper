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

import com.espertech.esper.client.PropertyAccessException;
import com.espertech.esper.event.EventPropertyGetterSPI;

/**
 * Shortcut-evaluator for use with POJO-backed events only.
 */
public interface BeanEventPropertyGetter extends EventPropertyGetterSPI {
    /**
     * Returns the property as an object.
     *
     * @param object to evaluate
     * @return property of object
     * @throws PropertyAccessException if access failed
     */
    public Object getBeanProp(Object object) throws PropertyAccessException;

    /**
     * Returns true if the dynamic property exists.
     *
     * @param object to evaluate
     * @return indicator if property exists
     */
    public boolean isBeanExistsProperty(Object object);

    public Class getBeanPropType();

    public Class getTargetType();
}

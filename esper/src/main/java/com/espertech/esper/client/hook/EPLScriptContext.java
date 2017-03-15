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
package com.espertech.esper.client.hook;

/**
 * Available when using JSR-223 scripts or MVEL, for access of script attributes.
 */
public interface EPLScriptContext {

    /**
     * Returns event and event type services
     * @return event type and event services
     */
    public EventBeanService getEventBeanService();

    /**
     * Set a script attributed.
     *
     * @param attribute name to use
     * @param value     value to set
     */
    public void setScriptAttribute(String attribute, Object value);

    /**
     * Return a script attribute value.
     *
     * @param attribute name to retrieve value for
     * @return attribute value or null if undefined
     */
    public Object getScriptAttribute(String attribute);
}

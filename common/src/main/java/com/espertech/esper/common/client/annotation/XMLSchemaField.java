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
package com.espertech.esper.common.client.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Annotation for use with XML to set a given event property name to use XPath.
 * The name, xpath and type are required.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface XMLSchemaField {
    /**
     * Property name
     * @return name
     */
    String name();

    /**
     * XPath expression
     * @return xpath
     */
    String xpath();

    /**
     * javax.xml.xpath.XPathConstants type as a string, i.e. "string" or "nodeset" and others
     * @return type
     */
    String type();

    /**
     * For use when event properties themselves has an xml event type
     * @return type name
     */
    String eventTypeName() default "";

    /**
     * For casting the xpath evaluation result to a given type
     * @return type to cast to
     */
    String castToType() default "";
}

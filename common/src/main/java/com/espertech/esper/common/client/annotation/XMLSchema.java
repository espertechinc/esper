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

import com.espertech.esper.common.client.EventSender;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Annotation for use with XML schemas. Only the root element name is required.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface XMLSchema {
    /**
     * The root element name (required).
     * @return root element name
     */
    String rootElementName();

    /**
     * The schema resource URL
     * @return url
     */
    String schemaResource() default "";

    /**
     * The schema text
     * @return schema
     */
    String schemaText() default "";

    /**
     * Set to false (the default) to indicate that property expressions are evaluated by the DOM-walker
     * implementation (the default), or set to true to indicate that property expressions are rewritten into XPath expressions.
     * @return xpath property use
     */
    boolean xpathPropertyExpr() default false;

    /**
     * When set to true (the default), indicates that when properties are compiled to XPath expressions that the
     * compilation should generate an absolute XPath expression such as "/getQuote/request" for the
     * simple request property, or "/getQuote/request/symbol" for a "request.symbol" nested property,
     * wherein the root element node is "getQuote".
     * <p>
     * When set to false, indicates that when properties are compiled to XPath expressions that the
     * compilation should generate a deep XPath expression such as "//symbol" for the
     * simple symbol property, or "//request/symbol" for a "request.symbol" nested property.
     *
     * @return xpath resolve properties absolute flag
     */
    boolean xpathResolvePropertiesAbsolute() default true;

    /**
     * The default namespace
     * @return default namespace
     */
    String defaultNamespace() default "";

    /**
     * The root element namespace
     * @return root element namespace
     */
    String rootElementNamespace() default "";

    /**
     * Set to true (the default) to indicate that an {@link EventSender} returned for this event type validates
     * the root document element name against the one configured (the default), or false to not validate the root document
     * element name as configured.
     * @return flag
     */
    boolean eventSenderValidatesRoot() default true;

    /**
     * Set to true (the default) to look up or create event types representing fragments of an XML document
     * automatically upon request for fragment event type information; Or false when only explicit
     * properties may return fragments.
     * @return flag
     */
    boolean autoFragment() default true;

    /**
     * Sets the class name of the XPath function resolver to be assigned to the XPath factory instance
     * upon type initialization.
     * @return class name
     */
    String xpathFunctionResolver() default "";

    /**
     * Sets the class name of the XPath variable resolver to be assigned to the XPath factory instance
     * upon type initialization.
     * @return class name
     */
    String xpathVariableResolver() default "";
}

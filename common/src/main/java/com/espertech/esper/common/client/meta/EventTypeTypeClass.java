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
package com.espertech.esper.common.client.meta;

/**
 * Metatype.
 */
public enum EventTypeTypeClass {
    /**
     * A type that represents the information made available via insert-into.
     */
    STREAM(),

    /**
     * A revision event type.
     */
    REVISION(),

    /**
     * A pattern-derived stream event type.
     */
    PATTERNDERIVED(),

    /**
     * A match-recognized-derived stream event type.
     */
    MATCHRECOGDERIVED(),

    /**
     * A variant stream event type.
     */
    VARIANT(),

    /**
     * An application-defined event type such as JavaBean or legacy Java, XML or Map.
     */
    APPLICATION(),

    /**
     * An application-defined event type such as JavaBean or legacy Java, XML or Map.
     */
    STATEMENTOUT(),

    /**
     * An derived-value-view-defined event type.
     */
    VIEWDERIVED(),

    /**
     * An enum-method derived event type.
     */
    ENUMDERIVED(),

    /**
     * A create-context for context properties event type.
     */
    CONTEXTPROPDERIVED(),

    /**
     * A bean-derived event type.
     */
    BEAN_INCIDENTAL(),

    /**
     * An UDF-method derived event type.
     */
    UDFDERIVED(),

    /**
     * A subquery-method derived event type.
     */
    SUBQDERIVED(),

    /**
     * A DB-access derived event type.
     */
    DBDERIVED(),

    /**
     * A Dataflow derived event type.
     */
    DATAFLOWDERIVED(),

    /**
     * A From-clause-method derived event type.
     */
    METHODPOLLDERIVED(),

    /**
     * A type representing a named window.
     */
    NAMED_WINDOW(),

    /**
     * A type representing a table.
     */
    TABLE_PUBLIC(),

    /**
     * A type for internal use with tables
     */
    TABLE_INTERNAL(),

    /**
     * An event type for exclude-plan evaluation.
     */
    EXCLUDEPLANHINTDERIVED(),

}

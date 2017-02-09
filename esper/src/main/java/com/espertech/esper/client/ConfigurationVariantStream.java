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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Configures a variant stream.
 */
public class ConfigurationVariantStream implements Serializable {
    private List<String> variantTypeNames;
    private TypeVariance typeVariance;
    private static final long serialVersionUID = 3147963036149252974L;

    /**
     * Ctor.
     */
    public ConfigurationVariantStream() {
        variantTypeNames = new ArrayList<String>();
        typeVariance = TypeVariance.PREDEFINED;
    }

    /**
     * Returns the type variance setting specifying whether the variant stream accepts event of
     * only the predefined types or any type.
     *
     * @return type variance setting
     */
    public TypeVariance getTypeVariance() {
        return typeVariance;
    }

    /**
     * Sets the type variance setting specifying whether the variant stream accepts event of
     * only the predefined types or any type.
     *
     * @param typeVariance type variance setting
     */
    public void setTypeVariance(TypeVariance typeVariance) {
        this.typeVariance = typeVariance;
    }

    /**
     * Returns the names of event types that a predefined for the variant stream.
     *
     * @return predefined types in the variant stream
     */
    public List<String> getVariantTypeNames() {
        return variantTypeNames;
    }

    /**
     * Adds names of an event types that is one of the predefined event typs allowed for the variant stream.
     *
     * @param eventTypeName name of the event type to allow in the variant stream
     */
    public void addEventTypeName(String eventTypeName) {
        variantTypeNames.add(eventTypeName);
    }

    /**
     * Enumeration specifying whether only the predefine types or any type of event is accepted by the variant stream.
     */
    public enum TypeVariance {
        /**
         * Allow only the predefined types to be inserted into the stream.
         */
        PREDEFINED,

        /**
         * Allow any types to be inserted into the stream.
         */
        ANY
    }
}

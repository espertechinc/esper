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
package com.espertech.esper.client.soda;

import java.io.Serializable;
import java.util.List;

/**
 * Represents type information for data flow operators.
 */
public class DataFlowOperatorOutputType implements Serializable {

    private static final long serialVersionUID = 4590721496447077706L;
    private boolean isWildcard;
    private String typeOrClassname;
    private List<DataFlowOperatorOutputType> typeParameters;

    /**
     * Ctor.
     */
    public DataFlowOperatorOutputType() {
    }

    /**
     * Ctor.
     *
     * @param wildcard        true for wildcard type
     * @param typeOrClassname type name
     * @param typeParameters  optional additional type parameters
     */
    public DataFlowOperatorOutputType(boolean wildcard, String typeOrClassname, List<DataFlowOperatorOutputType> typeParameters) {
        isWildcard = wildcard;
        this.typeOrClassname = typeOrClassname;
        this.typeParameters = typeParameters;
    }

    /**
     * Returns true for wildcard type.
     *
     * @return wildcard type indicator
     */
    public boolean isWildcard() {
        return isWildcard;
    }

    /**
     * Sets to true for wildcard type.
     *
     * @param wildcard wildcard type indicator
     */
    public void setWildcard(boolean wildcard) {
        isWildcard = wildcard;
    }

    /**
     * Returns the type name or class name.
     *
     * @return name
     */
    public String getTypeOrClassname() {
        return typeOrClassname;
    }

    /**
     * Sets the type name or class name.
     *
     * @param typeOrClassname name
     */
    public void setTypeOrClassname(String typeOrClassname) {
        this.typeOrClassname = typeOrClassname;
    }

    /**
     * Returns optional additional type parameters
     *
     * @return type params
     */
    public List<DataFlowOperatorOutputType> getTypeParameters() {
        return typeParameters;
    }

    /**
     * Sets optional additional type parameters
     *
     * @param typeParameters type params
     */
    public void setTypeParameters(List<DataFlowOperatorOutputType> typeParameters) {
        this.typeParameters = typeParameters;
    }
}

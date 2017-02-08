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
package com.espertech.esper.epl.spec;

import java.io.Serializable;
import java.util.List;

public class GraphOperatorOutputItemType implements Serializable {
    private static final long serialVersionUID = 9021595021369141198L;
    private final boolean isWildcard;
    private final String typeOrClassname;
    private final List<GraphOperatorOutputItemType> typeParameters;

    public GraphOperatorOutputItemType(boolean wildcard, String typeOrClassname, List<GraphOperatorOutputItemType> typeParameters) {
        isWildcard = wildcard;
        this.typeOrClassname = typeOrClassname;
        this.typeParameters = typeParameters;
    }

    public boolean isWildcard() {
        return isWildcard;
    }

    public String getTypeOrClassname() {
        return typeOrClassname;
    }

    public List<GraphOperatorOutputItemType> getTypeParameters() {
        return typeParameters;
    }
}

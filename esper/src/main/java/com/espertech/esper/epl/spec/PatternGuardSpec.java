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

import com.espertech.esper.epl.expression.core.ExprNode;

import java.util.List;

/**
 * Specification for a pattern guard object consists of a namespace, name and guard object parameters.
 */
public final class PatternGuardSpec extends ObjectSpec {
    private static final long serialVersionUID = -3744696950315586560L;

    /**
     * Constructor.
     *
     * @param namespace        if the namespace the object is in
     * @param objectName       is the name of the object
     * @param objectParameters is a list of values representing the object parameters
     */
    public PatternGuardSpec(String namespace, String objectName, List<ExprNode> objectParameters) {
        super(namespace, objectName, objectParameters);
    }
}

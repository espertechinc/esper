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
package com.espertech.esperio.representation.axiom;

import com.espertech.esper.event.xml.XPathNamespaceContext;
import org.jaxen.NamespaceContext;

/**
 * XPathContext for Jaxen compatibility.
 * <p>
 * See {@link AxiomEventRepresentation} for more details.
 */
public class AxiomXPathNamespaceContext extends XPathNamespaceContext implements NamespaceContext {
    public String translateNamespacePrefixToUri(String prefix) {
        return this.getNamespaceURI(prefix);
    }
}



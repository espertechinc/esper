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
package com.espertech.esper.supportregression.xml;

import javax.xml.namespace.QName;
import javax.xml.xpath.XPathVariableResolver;

public class SupportXPathVariableResolver implements XPathVariableResolver {
    public Object resolveVariable(QName variableName) {
        return "value";
    }
}

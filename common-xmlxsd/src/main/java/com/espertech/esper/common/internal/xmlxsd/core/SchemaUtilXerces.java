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
package com.espertech.esper.common.internal.xmlxsd.core;

import com.espertech.esper.common.client.EPException;
import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.common.client.type.EPTypePremade;
import com.espertech.esper.common.internal.event.xml.SchemaUtil;
import org.apache.xerces.impl.dv.XSSimpleType;

import javax.xml.namespace.QName;
import javax.xml.xpath.XPathConstants;

public class SchemaUtilXerces {
    /**
     * Returns the XPathConstants type for a given Xerces type definition.
     *
     * @param type is the type
     * @return XPathConstants type
     */
    protected static QName simpleTypeToQName(short type) {
        switch (type) {
            case XSSimpleType.PRIMITIVE_BOOLEAN:
                return XPathConstants.BOOLEAN;
            case XSSimpleType.PRIMITIVE_DOUBLE:
                return XPathConstants.NUMBER;
            case XSSimpleType.PRIMITIVE_STRING:
                return XPathConstants.STRING;
            case XSSimpleType.PRIMITIVE_DECIMAL:
                return XPathConstants.NUMBER;
            case XSSimpleType.PRIMITIVE_FLOAT:
                return XPathConstants.NUMBER;
            case XSSimpleType.PRIMITIVE_DATETIME:
                return XPathConstants.STRING;
            case XSSimpleType.PRIMITIVE_DATE:
                return XPathConstants.STRING;
            case XSSimpleType.PRIMITIVE_TIME:
                return XPathConstants.STRING;
            default:
                throw new EPException("Unexpected schema simple type encountered '" + type + "'");
        }
    }

    /**
     * Returns the type for a give short type and type name.
     *
     * @param xsType                 XSSimplyType type
     * @param typeName               type name in XML standard
     * @param optionalFractionDigits fraction digits if any are defined
     * @return equivalent native type
     */
    public static EPTypeClass toReturnType(short xsType, String typeName, Integer optionalFractionDigits) {
        if (typeName != null) {
            EPTypeClass result = SchemaUtil.TYPE_MAP.get(typeName);
            if (result != null) {
                return result;
            }
        }

        switch (xsType) {
            case XSSimpleType.PRIMITIVE_BOOLEAN:
                return EPTypePremade.BOOLEANBOXED.getEPType();
            case XSSimpleType.PRIMITIVE_STRING:
                return EPTypePremade.STRING.getEPType();
            case XSSimpleType.PRIMITIVE_DECIMAL:
                if ((optionalFractionDigits != null) && (optionalFractionDigits > 0)) {
                    return EPTypePremade.DOUBLEBOXED.getEPType();
                }
                return EPTypePremade.INTEGERBOXED.getEPType();
            case XSSimpleType.PRIMITIVE_FLOAT:
                return EPTypePremade.FLOATBOXED.getEPType();
            case XSSimpleType.PRIMITIVE_DOUBLE:
                return EPTypePremade.DOUBLEBOXED.getEPType();
            default:
                return EPTypePremade.STRING.getEPType();
        }
    }
}

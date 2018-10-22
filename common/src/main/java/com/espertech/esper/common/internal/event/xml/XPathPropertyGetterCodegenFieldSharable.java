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
package com.espertech.esper.common.internal.event.xml;

import com.espertech.esper.common.client.EPException;
import com.espertech.esper.common.client.EventPropertyGetter;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenFieldSharable;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.context.module.EPStatementInitServices;
import com.espertech.esper.common.internal.event.core.EventTypeUtility;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.constant;
import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.staticMethod;

public class XPathPropertyGetterCodegenFieldSharable implements CodegenFieldSharable {
    private final BaseXMLEventType baseXMLEventType;
    private final XPathPropertyGetter xPathPropertyGetter;

    public XPathPropertyGetterCodegenFieldSharable(BaseXMLEventType baseXMLEventType, XPathPropertyGetter xPathPropertyGetter) {
        this.baseXMLEventType = baseXMLEventType;
        this.xPathPropertyGetter = xPathPropertyGetter;
    }

    public Class type() {
        return XPathPropertyGetter.class;
    }

    public CodegenExpression initCtorScoped() {
        return staticMethod(XPathPropertyGetterCodegenFieldSharable.class, "resolveXPathPropertyGetter",
                EventTypeUtility.resolveTypeCodegen(baseXMLEventType, EPStatementInitServices.REF),
                constant(xPathPropertyGetter.getProperty()));
    }

    public static XPathPropertyGetter resolveXPathPropertyGetter(EventType eventType, String propertyName) {
        if (!(eventType instanceof BaseXMLEventType)) {
            throw new EPException("Failed to obtain xpath property getter, expected an xml event type but received type '" + eventType.getMetadata().getName() + "'");
        }
        BaseXMLEventType type = (BaseXMLEventType) eventType;
        EventPropertyGetter getter = type.getGetter(propertyName);
        if (!(getter instanceof XPathPropertyGetter)) {
            throw new EPException("Failed to obtain xpath property getter for property '" + propertyName + "', expected " + XPathPropertyGetter.class.getSimpleName() + " but received " + getter);
        }
        return (XPathPropertyGetter) getter;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        XPathPropertyGetterCodegenFieldSharable that = (XPathPropertyGetterCodegenFieldSharable) o;

        if (!baseXMLEventType.equals(that.baseXMLEventType)) return false;
        return xPathPropertyGetter.equals(that.xPathPropertyGetter);
    }

    public int hashCode() {
        int result = baseXMLEventType.hashCode();
        result = 31 * result + xPathPropertyGetter.hashCode();
        return result;
    }
}

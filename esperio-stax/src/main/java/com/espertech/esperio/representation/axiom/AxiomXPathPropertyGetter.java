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

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.PropertyAccessException;
import com.espertech.esper.codegen.base.CodegenClassScope;
import com.espertech.esper.codegen.base.CodegenMethodScope;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.util.SimpleTypeParser;
import com.espertech.esper.util.SimpleTypeParserFactory;
import org.apache.axiom.om.OMNode;
import org.apache.axiom.om.xpath.AXIOMXPath;
import org.jaxen.JaxenException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.namespace.QName;
import javax.xml.xpath.XPathConstants;

/**
 * Implementation of a property getter for the Axiom XML data model.
 * <p>
 * See {@link AxiomEventRepresentation} for more details.
 */
public class AxiomXPathPropertyGetter implements TypedEventPropertyGetter {
    private static final Logger log = LoggerFactory.getLogger(AxiomXPathPropertyGetter.class);
    private final AXIOMXPath expression;
    private final String property;
    private final QName resultType;
    private final SimpleTypeParser simpleTypeParser;
    private final Class optionalCastToType;

    /**
     * Ctor.
     *
     * @param propertyName       is the name of the event property for which this getter gets values
     * @param resultType         is the resulting type
     * @param xPath              the Axiom xpath expression
     * @param optionalCastToType null if no cast, or the type to cast to
     */
    public AxiomXPathPropertyGetter(String propertyName, AXIOMXPath xPath, QName resultType, Class optionalCastToType) {
        this.expression = xPath;
        this.property = propertyName;
        this.resultType = resultType;
        if (optionalCastToType != null) {
            simpleTypeParser = SimpleTypeParserFactory.getParser(optionalCastToType);
        } else {
            simpleTypeParser = null;
        }
        this.optionalCastToType = optionalCastToType;
    }

    public Object get(EventBean eventBean) throws PropertyAccessException {
        Object und = eventBean.getUnderlying();
        if (und == null) {
            throw new PropertyAccessException(
                    "Unexpected null underlying event encountered, expecting org.w3c.dom.Node instance as underlying");
        }
        if (!(und instanceof OMNode)) {
            throw new PropertyAccessException(
                    "Unexpected underlying event of type '"
                            + und.getClass()
                            + "' encountered, expecting org.w3c.dom.Node as underlying");
        }
        try {
            // if there is no parser, return xpath expression type
            if (optionalCastToType == null) {
                if (resultType.equals(XPathConstants.BOOLEAN)) {
                    return expression.booleanValueOf(und);
                } else if (resultType.equals(XPathConstants.NUMBER)) {
                    Number n = expression.numberValueOf(und);
                    return n.doubleValue();
                } else {
                    String result = expression.stringValueOf(und);
                    return result;
                }
            }

            // obtain result as string and parse
            String result = expression.stringValueOf(und);
            if (result == null) {
                return null;
            }

            try {
                return simpleTypeParser.parse(result.toString());
            } catch (RuntimeException ex) {
                log.warn("Error parsing XPath property named '" + property + "' expression result '" + result + " as type " + optionalCastToType.getName());
                return null;
            }
        } catch (JaxenException e) {
            throw new PropertyAccessException("Error getting property '" + property + "' : " + e.getMessage(), e);
        }
    }

    public Class getResultClass() {
        if (resultType.equals(XPathConstants.BOOLEAN)) {
            return Boolean.class;
        }
        if (resultType.equals(XPathConstants.NUMBER)) {
            return Double.class;
        }
        if (resultType.equals(XPathConstants.STRING)) {
            return String.class;
        }

        return String.class;
    }

    public boolean isExistsProperty(EventBean eventBean) {
        return true; // Property always exists as the property is not dynamic
    }

    public Object getFragment(EventBean eventBean) throws PropertyAccessException {
        return null;
    }

    public CodegenExpression eventBeanGetCodegen(CodegenExpression beanExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        throw getUnsupported();
    }

    public CodegenExpression eventBeanExistsCodegen(CodegenExpression beanExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        throw getUnsupported();
    }

    public CodegenExpression eventBeanFragmentCodegen(CodegenExpression beanExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        throw getUnsupported();
    }

    public CodegenExpression underlyingGetCodegen(CodegenExpression underlyingExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        throw getUnsupported();
    }

    public CodegenExpression underlyingExistsCodegen(CodegenExpression underlyingExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        throw getUnsupported();
    }

    public CodegenExpression underlyingFragmentCodegen(CodegenExpression underlyingExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        throw getUnsupported();
    }

    private UnsupportedOperationException getUnsupported() {
        return new UnsupportedOperationException("Codegeneration not supported with Axiom event type");
    }
}

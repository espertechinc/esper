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
package com.espertech.esper.event.xml;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventPropertyGetter;
import com.espertech.esper.client.PropertyAccessException;
import com.espertech.esper.util.JavaClassHelper;
import com.espertech.esper.util.SimpleTypeParser;
import com.espertech.esper.util.SimpleTypeParserFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.namespace.QName;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import java.lang.reflect.Array;

/**
 * Getter for properties of DOM xml events.
 *
 * @author pablo
 */
public class XPathPropertyGetter implements EventPropertyGetter {
    private static final Logger log = LoggerFactory.getLogger(XPathPropertyGetter.class);
    private final XPathExpression expression;
    private final String expressionText;
    private final String property;
    private final QName resultType;
    private final SimpleTypeParser simpleTypeParser;
    private final Class optionalCastToType;
    private final boolean isCastToArray;
    private final FragmentFactory fragmentFactory;

    /**
     * Ctor.
     *
     * @param propertyName       is the name of the event property for which this getter gets values
     * @param expressionText     is the property expression itself
     * @param xPathExpression    is a compile XPath expression
     * @param resultType         is the resulting type
     * @param optionalCastToType if non-null then the return value of the xpath expression is cast to this value
     * @param fragmentFactory    for creating fragments, or null in none to be created
     */
    public XPathPropertyGetter(String propertyName, String expressionText, XPathExpression xPathExpression, QName resultType, Class optionalCastToType, FragmentFactory fragmentFactory) {
        this.expression = xPathExpression;
        this.expressionText = expressionText;
        this.property = propertyName;
        this.resultType = resultType;
        this.fragmentFactory = fragmentFactory;

        if ((optionalCastToType != null) && (optionalCastToType.isArray())) {
            isCastToArray = true;
            if (!resultType.equals(XPathConstants.NODESET)) {
                throw new IllegalArgumentException("Array cast-to types require XPathConstants.NODESET as the XPath result type");
            }
            optionalCastToType = optionalCastToType.getComponentType();
        } else {
            isCastToArray = false;
        }

        if (optionalCastToType != null) {
            simpleTypeParser = SimpleTypeParserFactory.getParser(optionalCastToType);
        } else {
            simpleTypeParser = null;
        }
        if (optionalCastToType == Node.class) {
            this.optionalCastToType = null;
        } else {
            this.optionalCastToType = optionalCastToType;
        }
    }

    public Object get(EventBean eventBean) throws PropertyAccessException {
        Object und = eventBean.getUnderlying();
        if (und == null) {
            throw new PropertyAccessException("Unexpected null underlying event encountered, expecting org.w3c.dom.Node instance as underlying");
        }
        if (!(und instanceof Node)) {
            throw new PropertyAccessException("Unexpected underlying event of type '" + und.getClass() + "' encountered, expecting org.w3c.dom.Node as underlying");
        }
        try {

            if (log.isDebugEnabled()) {
                log.debug("Running XPath '" + expressionText + "' for property '" + property + "' against Node XML :" + SchemaUtil.serialize((Node) und));
            }

            // if there is no parser, return xpath expression type
            if (optionalCastToType == null) {
                return expression.evaluate(und, resultType);
            }

            // obtain result
            Object result = expression.evaluate(und, resultType);
            if (result == null) {
                return null;
            }

            if (isCastToArray) {
                return castToArray(result);
            }

            // string results get parsed
            if (result instanceof String) {
                try {
                    return simpleTypeParser.parse(result.toString());
                } catch (RuntimeException ex) {
                    log.warn("Error parsing XPath property named '" + property + "' expression result '" + result + " as type " + optionalCastToType.getName());
                    return null;
                }
            }

            // coercion
            if (result instanceof Double) {
                try {
                    return JavaClassHelper.coerceBoxed((Number) result, optionalCastToType);
                } catch (RuntimeException ex) {
                    log.warn("Error coercing XPath property named '" + property + "' expression result '" + result + " as type " + optionalCastToType.getName());
                    return null;
                }
            }

            // check boolean type
            if (result instanceof Boolean) {
                if (optionalCastToType != Boolean.class) {
                    log.warn("Error coercing XPath property named '" + property + "' expression result '" + result + " as type " + optionalCastToType.getName());
                    return null;
                }
                return result;
            }

            log.warn("Error processing XPath property named '" + property + "' expression result '" + result + ", not a known type");
            return null;
        } catch (XPathExpressionException e) {
            throw new PropertyAccessException("Error getting property " + property, e);
        }
    }

    public boolean isExistsProperty(EventBean eventBean) {
        return true; // Property exists as the property is not dynamic (unchecked)
    }

    public Object getFragment(EventBean eventBean) {
        if (fragmentFactory == null) {
            return null;
        }

        Object und = eventBean.getUnderlying();
        if (und == null) {
            throw new PropertyAccessException("Unexpected null underlying event encountered, expecting org.w3c.dom.Node instance as underlying");
        }
        if (!(und instanceof Node)) {
            throw new PropertyAccessException("Unexpected underlying event of type '" + und.getClass() + "' encountered, expecting org.w3c.dom.Node as underlying");
        }

        try {
            if (log.isDebugEnabled()) {
                log.debug("Running XPath '" + expressionText + "' for property '" + property + "' against Node XML :" + SchemaUtil.serialize((Node) und));
            }

            Object result = expression.evaluate(und, resultType);

            if (result instanceof Node) {
                return fragmentFactory.getEvent((Node) result);
            }

            if (result instanceof NodeList) {
                NodeList nodeList = (NodeList) result;
                EventBean[] events = new EventBean[nodeList.getLength()];
                for (int i = 0; i < events.length; i++) {
                    events[i] = fragmentFactory.getEvent(nodeList.item(i));
                }
                return events;
            }

            log.warn("Error processing XPath property named '" + property + "' expression result is not of type Node or Nodeset");
            return null;
        } catch (XPathExpressionException e) {
            throw new PropertyAccessException("Error getting property " + property, e);
        }
    }

    private Object castToArray(Object result) {
        if (!(result instanceof NodeList)) {
            return null;
        }

        NodeList nodeList = (NodeList) result;
        Object array = Array.newInstance(optionalCastToType, nodeList.getLength());

        for (int i = 0; i < nodeList.getLength(); i++) {
            Object arrayItem = null;
            try {
                Node item = nodeList.item(i);
                String textContent;
                if ((item.getNodeType() == Node.ATTRIBUTE_NODE) || (item.getNodeType() == Node.ELEMENT_NODE)) {
                    textContent = nodeList.item(i).getTextContent();
                } else {
                    continue;
                }

                arrayItem = simpleTypeParser.parse(textContent);
            } catch (Exception ex) {
                if (log.isInfoEnabled()) {
                    log.info("Parse error for text content " + nodeList.item(i).getTextContent() + " for expression " + expression);
                }
            }
            Array.set(array, i, arrayItem);
        }

        return array;
    }
}

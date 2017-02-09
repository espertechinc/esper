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
import com.espertech.esper.util.SimpleTypeParser;
import com.espertech.esper.util.SimpleTypeParserFactory;
import org.w3c.dom.Node;

import java.lang.reflect.Array;

/**
 * Getter for converting a Node child nodes into an array.
 */
public class DOMConvertingArrayGetter implements EventPropertyGetter {
    private final DOMPropertyGetter getter;
    private final Class componentType;
    private final SimpleTypeParser parser;

    /**
     * Ctor.
     *
     * @param domPropertyGetter getter
     * @param returnType        component type
     */
    public DOMConvertingArrayGetter(DOMPropertyGetter domPropertyGetter, Class returnType) {
        this.getter = domPropertyGetter;
        this.componentType = returnType;
        this.parser = SimpleTypeParserFactory.getParser(returnType);
    }

    public Object get(EventBean obj) throws PropertyAccessException {
        // The underlying is expected to be a map
        if (!(obj.getUnderlying() instanceof Node)) {
            throw new PropertyAccessException("Mismatched property getter to event bean type, " +
                    "the underlying data object is not of type Node");
        }

        Node node = (Node) obj.getUnderlying();

        Node[] result = getter.getValueAsNodeArray(node);
        if (result == null) {
            return null;
        }

        Object array = Array.newInstance(componentType, result.length);
        for (int i = 0; i < result.length; i++) {
            String text = result[i].getTextContent();
            if ((text == null) || (text.length() == 0)) {
                continue;
            }

            Object parseResult = parser.parse(text);
            Array.set(array, i, parseResult);
        }

        return array;
    }

    public boolean isExistsProperty(EventBean eventBean) {
        return true;
    }

    public Object getFragment(EventBean eventBean) throws PropertyAccessException {
        return null;
    }
}

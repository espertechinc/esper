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
import com.espertech.esper.client.PropertyAccessException;
import com.espertech.esper.codegen.core.CodegenContext;
import com.espertech.esper.codegen.core.CodegenMember;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.event.EventPropertyGetterSPI;
import com.espertech.esper.util.SimpleTypeParser;
import com.espertech.esper.util.SimpleTypeParserFactory;
import org.w3c.dom.Node;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;

/**
 * Getter for parsing node content to a desired type.
 */
public class DOMConvertingGetter implements EventPropertyGetterSPI {
    private final DOMPropertyGetter getter;
    private final SimpleTypeParser parser;
    private CodegenMember codegenParser;

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     * @param node node
     * @param parser parser
     * @return value
     * @throws PropertyAccessException exception
     */
    public static Object getParseTextValue(Node node, SimpleTypeParser parser) throws PropertyAccessException {
        if (node == null) {
            return null;
        }
        String text = node.getTextContent();
        if (text == null) {
            return null;
        }
        return parser.parse(text);
    }

    /**
     * Ctor.
     *  @param domPropertyGetter  getter
     * @param returnType         desired result type
     */
    public DOMConvertingGetter(DOMPropertyGetter domPropertyGetter, Class returnType) {
        this.getter = domPropertyGetter;
        this.parser = SimpleTypeParserFactory.getParser(returnType);
    }

    public Object get(EventBean obj) throws PropertyAccessException {
        // The underlying is expected to be a map
        if (!(obj.getUnderlying() instanceof Node)) {
            throw new PropertyAccessException("Mismatched property getter to event bean type, " +
                    "the underlying data object is not of type Node");
        }
        Node node = (Node) obj.getUnderlying();
        Node result = getter.getValueAsNode(node);
        return getParseTextValue(result, parser);
    }

    public boolean isExistsProperty(EventBean eventBean) {
        return true;
    }

    public Object getFragment(EventBean eventBean) throws PropertyAccessException {
        return null;
    }

    public CodegenExpression codegenEventBeanGet(CodegenExpression beanExpression, CodegenContext context) {
        return codegenUnderlyingGet(castUnderlying(Node.class, beanExpression), context);
    }

    public CodegenExpression codegenEventBeanExists(CodegenExpression beanExpression, CodegenContext context) {
        return codegenUnderlyingExists(castUnderlying(Node.class, beanExpression), context);
    }

    public CodegenExpression codegenEventBeanFragment(CodegenExpression beanExpression, CodegenContext context) {
        return constantNull();
    }

    public CodegenExpression codegenUnderlyingGet(CodegenExpression underlyingExpression, CodegenContext context) {
        generateParserMember(context);
        CodegenExpression inner = getter.codegenUnderlyingGet(underlyingExpression, context);
        return staticMethod(this.getClass(), "getParseTextValue", inner, ref(codegenParser.getMemberName()));
    }

    public CodegenExpression codegenUnderlyingExists(CodegenExpression underlyingExpression, CodegenContext context) {
        return constantTrue();
    }

    public CodegenExpression codegenUnderlyingFragment(CodegenExpression underlyingExpression, CodegenContext context) {
        return constantNull();
    }

    private void generateParserMember(CodegenContext context) {
        if (codegenParser == null) {
            codegenParser = context.makeMember(SimpleTypeParser.class, parser);
        }
        context.addMember(codegenParser);
    }
}

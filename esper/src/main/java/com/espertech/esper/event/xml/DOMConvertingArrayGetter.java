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
import com.espertech.esper.codegen.base.CodegenClassScope;
import com.espertech.esper.codegen.base.CodegenMember;
import com.espertech.esper.codegen.base.CodegenMethodScope;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.codegen.base.CodegenMethodNode;
import com.espertech.esper.event.EventPropertyGetterSPI;
import com.espertech.esper.util.SimpleTypeParser;
import com.espertech.esper.util.SimpleTypeParserFactory;
import org.w3c.dom.Node;

import java.lang.reflect.Array;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;

/**
 * Getter for converting a Node child nodes into an array.
 */
public class DOMConvertingArrayGetter implements EventPropertyGetterSPI {
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
        return getDOMArrayFromNodes(result, componentType, parser);
    }

    private CodegenMethodNode getCodegen(CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        CodegenMember mComponentType = codegenClassScope.makeAddMember(Class.class, componentType);
        CodegenMember mParser = codegenClassScope.makeAddMember(SimpleTypeParser.class, parser);
        return codegenMethodScope.makeChild(Object.class, this.getClass(), codegenClassScope).addParam(Node.class, "node").getBlock()
                .declareVar(Node[].class, "result", getter.getValueAsNodeArrayCodegen(ref("node"), codegenMethodScope, codegenClassScope))
                .ifRefNullReturnNull("result")
                .methodReturn(staticMethod(this.getClass(), "getDOMArrayFromNodes", ref("result"), member(mComponentType.getMemberId()), member(mParser.getMemberId())));
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     * @param result nodes
     * @param componentType type
     * @param parser parser
     * @return result
     */
    public static Object getDOMArrayFromNodes(Node[] result, Class componentType, SimpleTypeParser parser) {
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

    public CodegenExpression eventBeanGetCodegen(CodegenExpression beanExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return underlyingGetCodegen(castUnderlying(Node.class, beanExpression), codegenMethodScope, codegenClassScope);
    }

    public CodegenExpression eventBeanExistsCodegen(CodegenExpression beanExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return constantTrue();
    }

    public CodegenExpression eventBeanFragmentCodegen(CodegenExpression beanExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return constantNull();
    }

    public CodegenExpression underlyingGetCodegen(CodegenExpression underlyingExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return localMethod(getCodegen(codegenMethodScope, codegenClassScope), underlyingExpression);
    }

    public CodegenExpression underlyingExistsCodegen(CodegenExpression underlyingExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return constantTrue();
    }

    public CodegenExpression underlyingFragmentCodegen(CodegenExpression underlyingExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return constantNull();
    }
}

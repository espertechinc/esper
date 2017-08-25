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
import com.espertech.esper.codegen.base.*;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.event.EventPropertyGetterSPI;
import org.w3c.dom.Node;

import java.util.List;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;

/**
 * Getter for nested properties in a DOM tree.
 */
public class DOMNestedPropertyGetter implements EventPropertyGetterSPI, DOMPropertyGetter {
    private final DOMPropertyGetter[] domGetterChain;
    private final FragmentFactory fragmentFactory;

    /**
     * Ctor.
     *
     * @param getterChain     is the chain of getters to retrieve each nested property
     * @param fragmentFactory for creating fragments
     */
    public DOMNestedPropertyGetter(List<EventPropertyGetter> getterChain, FragmentFactory fragmentFactory) {
        this.domGetterChain = new DOMPropertyGetter[getterChain.size()];
        this.fragmentFactory = fragmentFactory;

        int count = 0;
        for (EventPropertyGetter getter : getterChain) {
            domGetterChain[count++] = (DOMPropertyGetter) getter;
        }
    }

    public Object getValueAsFragment(Node node) {
        Node result = getValueAsNode(node);
        if (result == null) {
            return null;
        }
        return fragmentFactory.getEvent(result);
    }

    private CodegenMethodNode getValueAsFragmentCodegen(CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        CodegenMember mType = codegenClassScope.makeAddMember(FragmentFactory.class, fragmentFactory);
        return codegenMethodScope.makeChild(Object.class, this.getClass(), codegenClassScope).addParam(Node.class, "node").getBlock()
                .declareVar(Node.class, "result", getValueAsNodeCodegen(ref("node"), codegenMethodScope, codegenClassScope))
                .ifRefNullReturnNull("result")
                .methodReturn(exprDotMethod(member(mType.getMemberId()), "getEvent", ref("result")));
    }

    public Node[] getValueAsNodeArray(Node node) {
        for (int i = 0; i < domGetterChain.length - 1; i++) {
            node = domGetterChain[i].getValueAsNode(node);
            if (node == null) {
                return null;
            }
        }
        return domGetterChain[domGetterChain.length - 1].getValueAsNodeArray(node);
    }

    private CodegenMethodNode getValueAsNodeArrayCodegen(CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        CodegenBlock block = codegenMethodScope.makeChild(Node[].class, this.getClass(), codegenClassScope).addParam(Node.class, "node").getBlock();
        for (int i = 0; i < domGetterChain.length - 1; i++) {
            block.assignRef("node", domGetterChain[i].getValueAsNodeCodegen(ref("node"), codegenMethodScope, codegenClassScope));
            block.ifRefNullReturnNull("node");
        }
        return block.methodReturn(domGetterChain[domGetterChain.length - 1].getValueAsNodeArrayCodegen(ref("node"), codegenMethodScope, codegenClassScope));
    }

    public Node getValueAsNode(Node node) {
        for (int i = 0; i < domGetterChain.length; i++) {
            node = domGetterChain[i].getValueAsNode(node);
            if (node == null) {
                return null;
            }
        }
        return node;
    }

    private CodegenMethodNode getValueAsNodeCodegen(CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        CodegenBlock block = codegenMethodScope.makeChild(Node.class, this.getClass(), codegenClassScope).addParam(Node.class, "node").getBlock();
        for (int i = 0; i < domGetterChain.length; i++) {
            block.assignRef("node", domGetterChain[i].getValueAsNodeCodegen(ref("node"), codegenMethodScope, codegenClassScope));
            block.ifRefNullReturnNull("node");
        }
        return block.methodReturn(ref("node"));
    }

    public Object get(EventBean obj) throws PropertyAccessException {
        // The underlying is expected to be a map
        if (!(obj.getUnderlying() instanceof Node)) {
            throw new PropertyAccessException("Mismatched property getter to event bean type, " +
                    "the underlying data object is not of type Node");
        }

        Node node = (Node) obj.getUnderlying();
        return getValueAsNode(node);
    }

    public boolean isExistsProperty(EventBean obj) {
        // The underlying is expected to be a map
        if (!(obj.getUnderlying() instanceof Node)) {
            throw new PropertyAccessException("Mismatched property getter to event bean type, " +
                    "the underlying data object is not of type Node");
        }
        return isExistsProperty((Node) obj.getUnderlying());
    }

    private boolean isExistsProperty(Node value) {
        for (int i = 0; i < domGetterChain.length; i++) {
            value = domGetterChain[i].getValueAsNode(value);
            if (value == null) {
                return false;
            }
        }
        return true;
    }

    private CodegenMethodNode isExistsPropertyCodegen(CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        CodegenBlock block = codegenMethodScope.makeChild(boolean.class, this.getClass(), codegenClassScope).addParam(Node.class, "value").getBlock();
        for (int i = 0; i < domGetterChain.length; i++) {
            block.assignRef("value", domGetterChain[i].getValueAsNodeCodegen(ref("value"), codegenMethodScope, codegenClassScope));
            block.ifRefNullReturnFalse("value");
        }
        return block.methodReturn(constantTrue());
    }

    public Object getFragment(EventBean obj) throws PropertyAccessException {
        // The underlying is expected to be a map
        if (!(obj.getUnderlying() instanceof Node)) {
            throw new PropertyAccessException("Mismatched property getter to event bean type, " +
                    "the underlying data object is not of type Node");
        }

        Node value = (Node) obj.getUnderlying();

        for (int i = 0; i < domGetterChain.length - 1; i++) {
            value = domGetterChain[i].getValueAsNode(value);

            if (value == null) {
                return null;
            }
        }

        return domGetterChain[domGetterChain.length - 1].getValueAsFragment(value);
    }

    private CodegenMethodNode getFragmentCodegen(CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        CodegenBlock block = codegenMethodScope.makeChild(Object.class, this.getClass(), codegenClassScope).addParam(Node.class, "value").getBlock();
        for (int i = 0; i < domGetterChain.length - 1; i++) {
            block.assignRef("value", domGetterChain[i].getValueAsNodeCodegen(ref("value"), codegenMethodScope, codegenClassScope));
            block.ifRefNullReturnNull("value");
        }
        return block.methodReturn(domGetterChain[domGetterChain.length - 1].underlyingFragmentCodegen(ref("value"), codegenMethodScope, codegenClassScope));
    }

    public CodegenExpression eventBeanGetCodegen(CodegenExpression beanExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return underlyingGetCodegen(castUnderlying(Node.class, beanExpression), codegenMethodScope, codegenClassScope);
    }

    public CodegenExpression eventBeanExistsCodegen(CodegenExpression beanExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return underlyingExistsCodegen(castUnderlying(Node.class, beanExpression), codegenMethodScope, codegenClassScope);
    }

    public CodegenExpression eventBeanFragmentCodegen(CodegenExpression beanExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return underlyingFragmentCodegen(castUnderlying(Node.class, beanExpression), codegenMethodScope, codegenClassScope);
    }

    public CodegenExpression underlyingGetCodegen(CodegenExpression underlyingExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return localMethod(getValueAsNodeCodegen(codegenMethodScope, codegenClassScope), underlyingExpression);
    }

    public CodegenExpression underlyingExistsCodegen(CodegenExpression underlyingExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return localMethod(isExistsPropertyCodegen(codegenMethodScope, codegenClassScope), underlyingExpression);
    }

    public CodegenExpression underlyingFragmentCodegen(CodegenExpression underlyingExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return localMethod(getFragmentCodegen(codegenMethodScope, codegenClassScope), underlyingExpression);
    }

    public CodegenExpression getValueAsNodeCodegen(CodegenExpression value, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return localMethod(getValueAsNodeCodegen(codegenMethodScope, codegenClassScope), value);
    }

    public CodegenExpression getValueAsNodeArrayCodegen(CodegenExpression value, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return localMethod(getValueAsNodeArrayCodegen(codegenMethodScope, codegenClassScope), value);
    }

    public CodegenExpression getValueAsFragmentCodegen(CodegenExpression value, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return localMethod(getValueAsFragmentCodegen(codegenMethodScope, codegenClassScope), value);
    }
}

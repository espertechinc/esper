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

import com.espertech.esper.codegen.base.CodegenClassScope;
import com.espertech.esper.codegen.base.CodegenMethodScope;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.event.EventPropertyGetterSPI;
import org.w3c.dom.Node;

/**
 * Shortcut-getter for DOM underlying objects.
 */
public interface DOMPropertyGetter extends EventPropertyGetterSPI {
    /**
     * Returns a property value as a node.
     *
     * @param node to evaluate
     * @return value node
     */
    public Node getValueAsNode(Node node);

    /**
     * Returns a property value that is indexed as a node array.
     *
     * @param node to evaluate
     * @return nodes
     */
    public Node[] getValueAsNodeArray(Node node);

    /**
     * Returns a property value as a fragment.
     *
     * @param node to evaluate
     * @return fragment
     */
    public Object getValueAsFragment(Node node);

    public CodegenExpression getValueAsNodeCodegen(CodegenExpression value, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope);
    public CodegenExpression getValueAsNodeArrayCodegen(CodegenExpression value, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope);
    public CodegenExpression getValueAsFragmentCodegen(CodegenExpression value, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope);
}

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
package com.espertech.esper.common.internal.filterspec;

import com.espertech.esper.common.client.EventPropertyGetter;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbolWEventType;
import com.espertech.esper.common.internal.context.module.EPStatementInitServices;
import com.espertech.esper.common.internal.epl.expression.core.ExprFilterSpecLookupableForge;
import com.espertech.esper.common.internal.epl.index.advanced.index.quadtree.AdvancedIndexConfigContextPartitionQuadTree;
import com.espertech.esper.common.internal.event.core.EventPropertyGetterSPI;
import com.espertech.esper.common.internal.event.core.EventTypeUtility;

import java.util.function.Function;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class FilterSpecLookupableAdvancedIndexForge extends ExprFilterSpecLookupableForge {
    private final AdvancedIndexConfigContextPartitionQuadTree quadTreeConfig;
    private final EventPropertyGetterSPI x;
    private final EventPropertyGetterSPI y;
    private final EventPropertyGetterSPI width;
    private final EventPropertyGetterSPI height;
    private final String indexType;

    public FilterSpecLookupableAdvancedIndexForge(String expression, EventPropertyGetterSPI getter, Class returnType, AdvancedIndexConfigContextPartitionQuadTree quadTreeConfig, EventPropertyGetterSPI x, EventPropertyGetterSPI y, EventPropertyGetterSPI width, EventPropertyGetterSPI height, String indexType) {
        super(expression, getter, returnType, true, null);
        this.quadTreeConfig = quadTreeConfig;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.indexType = indexType;
    }

    @Override
    public CodegenMethod makeCodegen(CodegenMethodScope parent, SAIFFInitializeSymbolWEventType symbols, CodegenClassScope classScope) {
        CodegenMethod method = parent.makeChild(FilterSpecLookupableAdvancedIndex.class, FilterSpecLookupableAdvancedIndexForge.class, classScope);
        Function<EventPropertyGetterSPI, CodegenExpression> toEval = getter -> EventTypeUtility.codegenGetterWCoerce(getter, Double.class, null, method, this.getClass(), classScope);
        method.getBlock()
                .declareVar(FilterSpecLookupableAdvancedIndex.class, "lookupable", newInstance(FilterSpecLookupableAdvancedIndex.class,
                        constant(expression), constantNull(), enumValue(returnType, "class")))
                .exprDotMethod(ref("lookupable"), "setQuadTreeConfig", quadTreeConfig.make())
                .exprDotMethod(ref("lookupable"), "setX", toEval.apply(x))
                .exprDotMethod(ref("lookupable"), "setY", toEval.apply(y))
                .exprDotMethod(ref("lookupable"), "setWidth", toEval.apply(width))
                .exprDotMethod(ref("lookupable"), "setHeight", toEval.apply(height))
                .exprDotMethod(ref("lookupable"), "setIndexType", constant(indexType))
                .expression(exprDotMethodChain(symbols.getAddInitSvc(method)).add(EPStatementInitServices.GETFILTERSHAREDLOOKUPABLEREGISTERY).add("registerLookupable", symbols.getAddEventType(method), ref("lookupable")))
                .methodReturn(ref("lookupable"));
        return method;
    }

    public EventPropertyGetter getX() {
        return x;
    }

    public EventPropertyGetter getY() {
        return y;
    }

    public EventPropertyGetter getWidth() {
        return width;
    }

    public EventPropertyGetter getHeight() {
        return height;
    }

    public AdvancedIndexConfigContextPartitionQuadTree getQuadTreeConfig() {
        return quadTreeConfig;
    }

    public String getIndexType() {
        return indexType;
    }
}



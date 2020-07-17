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
package com.espertech.esper.common.internal.view.access;

import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.epl.expression.codegen.CodegenLegoRichConstant;

import java.util.Collections;
import java.util.SortedSet;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

/**
 * Coordinates between view factories and requested resource (by expressions) the
 * availability of view resources to expressions.
 */
public class ViewResourceDelegateDesc {
    public final static EPTypeClass EPTYPE = new EPTypeClass(ViewResourceDelegateDesc.class);
    public final static EPTypeClass EPTYPEARRAY = new EPTypeClass(ViewResourceDelegateDesc[].class);
    public final static ViewResourceDelegateDesc[] SINGLE_ELEMENT_ARRAY = new ViewResourceDelegateDesc[] {new ViewResourceDelegateDesc(false, Collections.emptySortedSet())};

    private final boolean hasPrevious;
    private final SortedSet<Integer> priorRequests;

    public ViewResourceDelegateDesc(boolean hasPrevious, SortedSet<Integer> priorRequests) {
        this.hasPrevious = hasPrevious;
        this.priorRequests = priorRequests;
    }

    public static CodegenExpression toExpression(ViewResourceDelegateDesc[] descs) {
        if (descs.length == 1 && !descs[0].hasPrevious && descs[0].priorRequests.isEmpty()) {
            return publicConstValue(ViewResourceDelegateDesc.class, "SINGLE_ELEMENT_ARRAY");
        }
        CodegenExpression[] values = new CodegenExpression[descs.length];
        for (int i = 0; i < descs.length; i++) {
            values[i] = descs[i].toExpression();
        }
        return newArrayWithInit(ViewResourceDelegateDesc.EPTYPE, values);
    }

    public boolean isHasPrevious() {
        return hasPrevious;
    }

    public SortedSet<Integer> getPriorRequests() {
        return priorRequests;
    }

    public CodegenExpression toExpression() {
        return newInstance(EPTYPE, constant(hasPrevious), CodegenLegoRichConstant.toExpression(priorRequests));
    }

    public static boolean hasPrior(ViewResourceDelegateDesc[] delegates) {
        for (ViewResourceDelegateDesc delegate : delegates) {
            if (!delegate.priorRequests.isEmpty()) {
                return true;
            }
        }
        return false;
    }
}

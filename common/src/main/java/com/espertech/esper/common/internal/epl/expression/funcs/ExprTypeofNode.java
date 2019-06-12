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
package com.espertech.esper.common.internal.epl.expression.funcs;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.FragmentEventType;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.epl.expression.core.*;
import com.espertech.esper.common.internal.event.core.EventPropertyGetterSPI;
import com.espertech.esper.common.internal.event.core.EventPropertyValueGetterForge;
import com.espertech.esper.common.internal.event.core.EventTypeSPI;
import com.espertech.esper.common.internal.serde.compiletime.resolve.DataInputOutputSerdeForge;

import java.io.StringWriter;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

/**
 * Represents the TYPEOF(a) function is an expression tree.
 */
public class ExprTypeofNode extends ExprNodeBase implements ExprFilterOptimizableNode {
    private transient ExprTypeofNodeForge forge;
    private transient ExprValidationContext exprValidationContext;

    /**
     * Ctor.
     */
    public ExprTypeofNode() {
    }

    public ExprEvaluator getExprEvaluator() {
        checkValidated(forge);
        return forge.getExprEvaluator();
    }

    public ExprForge getForge() {
        return forge;
    }

    public ExprNode validate(ExprValidationContext validationContext) throws ExprValidationException {
        this.exprValidationContext = validationContext;
        if (this.getChildNodes().length != 1) {
            throw new ExprValidationException("Typeof node must have 1 child expression node supplying the expression to test");
        }

        if (this.getChildNodes()[0] instanceof ExprStreamUnderlyingNode) {
            ExprStreamUnderlyingNode stream = (ExprStreamUnderlyingNode) getChildNodes()[0];
            forge = new ExprTypeofNodeForgeStreamEvent(this, stream.getStreamId());
            return null;
        }

        if (this.getChildNodes()[0] instanceof ExprIdentNode) {
            ExprIdentNode ident = (ExprIdentNode) getChildNodes()[0];
            int streamNum = validationContext.getStreamTypeService().getStreamNumForStreamName(ident.getFullUnresolvedName());
            if (streamNum != -1) {
                forge = new ExprTypeofNodeForgeStreamEvent(this, streamNum);
                return null;
            }

            EventType eventType = validationContext.getStreamTypeService().getEventTypes()[ident.getStreamId()];
            FragmentEventType fragmentEventType = eventType.getFragmentType(ident.getResolvedPropertyName());
            if (fragmentEventType != null) {
                EventPropertyGetterSPI getter = ((EventTypeSPI) eventType).getGetterSPI(ident.getResolvedPropertyName());
                forge = new ExprTypeofNodeForgeFragmentType(this, ident.getStreamId(), getter, fragmentEventType.getFragmentType().getName());
                return null;
            }
        }

        forge = new ExprTypeofNodeForgeInnerEval(this);
        return null;
    }

    public boolean isConstantResult() {
        return false;
    }

    public Class getType() {
        return String.class;
    }

    public boolean getFilterLookupEligible() {
        return true;
    }

    public ExprFilterSpecLookupableForge getFilterLookupable() {
        EventPropertyValueGetterForge eventPropertyForge = new EventPropertyValueGetterForge() {
            public CodegenExpression eventBeanGetCodegen(CodegenExpression beanExpression, CodegenMethodScope parent, CodegenClassScope classScope) {
                CodegenMethod method = parent.makeChild(String.class, this.getClass(), classScope).addParam(EventBean.class, "bean");
                method.getBlock().methodReturn(exprDotMethodChain(ref("bean")).add("getEventType").add("getName"));
                return localMethod(method, beanExpression);
            }
        };
        DataInputOutputSerdeForge serde = exprValidationContext.getSerdeResolver().serdeForFilter(String.class, exprValidationContext.getStatementRawInfo());
        return new ExprFilterSpecLookupableForge(ExprNodeUtilityPrint.toExpressionStringMinPrecedenceSafe(this), eventPropertyForge, String.class, true, serde);
    }

    public void toPrecedenceFreeEPL(StringWriter writer) {
        writer.append("typeof(");
        this.getChildNodes()[0].toEPL(writer, ExprPrecedenceEnum.MINIMUM);
        writer.append(')');
    }

    public ExprPrecedenceEnum getPrecedence() {
        return ExprPrecedenceEnum.UNARY;
    }

    public boolean equalsNode(ExprNode node, boolean ignoreStreamPrefix) {
        return node instanceof ExprTypeofNode;
    }
}

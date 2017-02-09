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
package com.espertech.esper.epl.enummethod.eval;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Iterator;

public class EnumEvalSequenceEqual extends EnumEvalBase implements EnumEval {

    private static final Logger log = LoggerFactory.getLogger(EnumEvalSequenceEqual.class);

    public EnumEvalSequenceEqual(ExprEvaluator innerExpression, int streamCountIncoming) {
        super(innerExpression, streamCountIncoming);
    }

    public Object evaluateEnumMethod(EventBean[] eventsLambda, Collection target, boolean isNewData, ExprEvaluatorContext context) {
        Object otherObj = this.getInnerExpression().evaluate(eventsLambda, isNewData, context);

        if (otherObj == null) {
            return false;
        }
        if (!(otherObj instanceof Collection)) {
            if (otherObj.getClass().isArray()) {
                if (target.size() != Array.getLength(otherObj)) {
                    return false;
                }

                if (target.isEmpty()) {
                    return true;
                }

                Iterator oneit = target.iterator();
                for (int i = 0; i < target.size(); i++) {
                    Object first = oneit.next();
                    Object second = Array.get(otherObj, i);

                    if (first == null) {
                        if (second != null) {
                            return false;
                        }
                        continue;
                    }
                    if (second == null) {
                        return false;
                    }

                    if (!first.equals(second)) {
                        return false;
                    }
                }

                return true;
            } else {
                log.warn("Enumeration method 'sequenceEqual' expected a Collection-type return value from its parameter but received '" + otherObj.getClass() + "'");
                return false;
            }
        }

        Collection other = (Collection) otherObj;
        if (target.size() != other.size()) {
            return false;
        }

        if (target.isEmpty()) {
            return true;
        }

        Iterator oneit = target.iterator();
        Iterator twoit = other.iterator();
        for (int i = 0; i < target.size(); i++) {
            Object first = oneit.next();
            Object second = twoit.next();

            if (first == null) {
                if (second != null) {
                    return false;
                }
                continue;
            }
            if (second == null) {
                return false;
            }

            if (!first.equals(second)) {
                return false;
            }
        }

        return true;
    }
}

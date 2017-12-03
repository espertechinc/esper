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
package com.espertech.esper.pattern.guard;

import com.espertech.esper.client.EPException;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.pattern.MatchedEventConvertor;
import com.espertech.esper.filterspec.MatchedEventMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Guard implementation that keeps a timer instance and quits when the timer expired,
 * and also keeps a count of the number of matches so far, checking both count and timer,
 * letting all {@link MatchedEventMap} instances pass until then.
 */
public class ExpressionGuard implements Guard {
    private final static Logger log = LoggerFactory.getLogger(ExpressionGuard.class);

    private final Quitable quitable;
    private final MatchedEventConvertor convertor;
    private final ExprEvaluator expression;

    public ExpressionGuard(MatchedEventConvertor convertor, ExprEvaluator expression, Quitable quitable) {
        this.quitable = quitable;
        this.convertor = convertor;
        this.expression = expression;
    }

    public void startGuard() {
    }

    public boolean inspect(MatchedEventMap matchEvent) {
        EventBean[] eventsPerStream = convertor.convert(matchEvent);

        try {
            Object result = expression.evaluate(eventsPerStream, true, quitable.getContext().getAgentInstanceContext());
            if (result == null) {
                return false;
            }

            if (result.equals(Boolean.TRUE)) {
                return true;
            }

            quitable.guardQuit();
            return false;
        } catch (RuntimeException ex) {
            String message = "Failed to evaluate expression for pattern-guard for statement '" + quitable.getContext().getPatternContext().getStatementName() + "'";
            if (ex.getMessage() != null) {
                message += ": " + ex.getMessage();
            }
            log.error(message, ex);
            throw new EPException(message);
        }
    }

    public void stopGuard() {
    }

    public void accept(EventGuardVisitor visitor) {
        visitor.visitGuard(0);
    }
}

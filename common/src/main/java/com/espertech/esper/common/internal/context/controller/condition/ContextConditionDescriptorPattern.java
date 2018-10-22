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
package com.espertech.esper.common.internal.context.controller.condition;

import com.espertech.esper.common.internal.epl.pattern.and.EvalAndFactoryNode;
import com.espertech.esper.common.internal.epl.pattern.core.EvalFactoryNodeVisitor;
import com.espertech.esper.common.internal.epl.pattern.core.EvalRootFactoryNode;
import com.espertech.esper.common.internal.epl.pattern.core.PatternContext;
import com.espertech.esper.common.internal.epl.pattern.every.EvalEveryFactoryNode;
import com.espertech.esper.common.internal.epl.pattern.everydistinct.EvalEveryDistinctFactoryNode;
import com.espertech.esper.common.internal.epl.pattern.filter.EvalFilterFactoryNode;
import com.espertech.esper.common.internal.epl.pattern.followedby.EvalFollowedByFactoryNode;
import com.espertech.esper.common.internal.epl.pattern.guard.EvalGuardFactoryNode;
import com.espertech.esper.common.internal.epl.pattern.matchuntil.EvalMatchUntilFactoryNode;
import com.espertech.esper.common.internal.epl.pattern.not.EvalNotFactoryNode;
import com.espertech.esper.common.internal.epl.pattern.observer.EvalObserverFactoryNode;
import com.espertech.esper.common.internal.epl.pattern.or.EvalOrFactoryNode;
import com.espertech.esper.common.internal.filterspec.FilterSpecActivatable;

import java.util.List;

public class ContextConditionDescriptorPattern implements ContextConditionDescriptor {
    private EvalRootFactoryNode pattern;
    private PatternContext patternContext;
    private boolean inclusive;
    private boolean immediate;
    private String[] taggedEvents;
    private String[] arrayEvents;

    public EvalRootFactoryNode getPattern() {
        return pattern;
    }

    public void setPattern(EvalRootFactoryNode pattern) {
        this.pattern = pattern;
    }

    public PatternContext getPatternContext() {
        return patternContext;
    }

    public void setPatternContext(PatternContext patternContext) {
        this.patternContext = patternContext;
    }

    public boolean isInclusive() {
        return inclusive;
    }

    public void setInclusive(boolean inclusive) {
        this.inclusive = inclusive;
    }

    public String[] getTaggedEvents() {
        return taggedEvents;
    }

    public void setTaggedEvents(String[] taggedEvents) {
        this.taggedEvents = taggedEvents;
    }

    public String[] getArrayEvents() {
        return arrayEvents;
    }

    public void setArrayEvents(String[] arrayEvents) {
        this.arrayEvents = arrayEvents;
    }

    public boolean isImmediate() {
        return immediate;
    }

    public void setImmediate(boolean immediate) {
        this.immediate = immediate;
    }

    public void addFilterSpecActivatable(List<FilterSpecActivatable> activatables) {
        EvalFactoryNodeVisitor visitor = new EvalFactoryNodeVisitor() {
            public void visit(EvalRootFactoryNode root) {
            }

            public void visit(EvalOrFactoryNode or) {
            }

            public void visit(EvalNotFactoryNode not) {
            }

            public void visit(EvalEveryDistinctFactoryNode everyDistinct) {
            }

            public void visit(EvalMatchUntilFactoryNode matchUntil) {
            }

            public void visit(EvalFollowedByFactoryNode followedBy) {
            }

            public void visit(EvalAndFactoryNode and) {
            }

            public void visit(EvalObserverFactoryNode observer) {
            }

            public void visit(EvalEveryFactoryNode every) {
            }

            public void visit(EvalGuardFactoryNode guard) {
            }

            public void visit(EvalFilterFactoryNode filter) {
                activatables.add(filter.getFilterSpec());
            }
        };
        pattern.accept(visitor);
    }
}

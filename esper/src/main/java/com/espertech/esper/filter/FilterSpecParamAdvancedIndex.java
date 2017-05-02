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
package com.espertech.esper.filter;

import com.espertech.esper.core.context.util.AgentInstanceContext;
import com.espertech.esper.pattern.MatchedEventMap;
import com.espertech.esper.spatial.quadtree.core.XYPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class FilterSpecParamAdvancedIndex extends FilterSpecParam {
    private static final Logger log = LoggerFactory.getLogger(FilterSpecParamAdvancedIndex.class);

    private FilterSpecParamFilterForEvalDouble xEval;
    private FilterSpecParamFilterForEvalDouble yEval;

    public FilterSpecParamAdvancedIndex(FilterSpecLookupable lookupable, FilterOperator filterOperator, FilterSpecParamFilterForEvalDouble xEval, FilterSpecParamFilterForEvalDouble yEval) {
        super(lookupable, filterOperator);
        this.xEval = xEval;
        this.yEval = yEval;
    }

    public Object getFilterValue(MatchedEventMap matchedEvents, AgentInstanceContext agentInstanceContext) {
        Double x = xEval.getFilterValueDouble(matchedEvents, agentInstanceContext);
        Double y = yEval.getFilterValueDouble(matchedEvents, agentInstanceContext);
        return new XYPoint(x, y);
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof FilterSpecParamAdvancedIndex)) {
            return false;
        }

        FilterSpecParamAdvancedIndex other = (FilterSpecParamAdvancedIndex) obj;
        if (!super.equals(other)) {
            return false;
        }
        return this.xEval.equals(other.xEval) &&
                (this.yEval.equals(other.yEval));
    }

    public int hashCode() {
        return super.hashCode();
    }
}

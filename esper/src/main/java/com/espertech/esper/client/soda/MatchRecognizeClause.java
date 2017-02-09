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
package com.espertech.esper.client.soda;

import java.io.Serializable;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * Match-recognize clause.
 */
public class MatchRecognizeClause implements Serializable {

    private static final long serialVersionUID = -344174798312697242L;

    private List<Expression> partitionExpressions = new ArrayList<Expression>();
    private List<SelectClauseExpression> measures = new ArrayList<SelectClauseExpression>();
    private boolean all;
    private MatchRecognizeSkipClause skipClause = MatchRecognizeSkipClause.PAST_LAST_ROW;
    private MatchRecognizeRegEx pattern;
    private MatchRecognizeIntervalClause intervalClause;
    private List<MatchRecognizeDefine> defines = new ArrayList<MatchRecognizeDefine>();

    /**
     * Ctor.
     */
    public MatchRecognizeClause() {
    }

    /**
     * Renders the clause in textual representation.
     *
     * @param writer to output to
     */
    public void toEPL(StringWriter writer) {
        writer.write(" match_recognize (");

        if (partitionExpressions.size() > 0) {
            String delimiter = "";
            writer.write(" partition by ");
            for (Expression part : partitionExpressions) {
                writer.write(delimiter);
                part.toEPL(writer, ExpressionPrecedenceEnum.MINIMUM);
                delimiter = ", ";
            }
        }

        String delimiter = "";
        writer.write(" measures ");
        for (SelectClauseExpression part : measures) {
            writer.write(delimiter);
            part.toEPLElement(writer);
            delimiter = ", ";
        }

        if (all) {
            writer.write(" all matches");
        }

        if (skipClause != MatchRecognizeSkipClause.PAST_LAST_ROW) {
            writer.write(" after match skip " + skipClause.getText());
        }

        writer.write(" pattern (");
        pattern.writeEPL(writer);
        writer.write(")");

        if ((intervalClause != null) && (intervalClause.getExpression() != null)) {
            writer.write(" interval ");
            intervalClause.getExpression().toEPL(writer, ExpressionPrecedenceEnum.MINIMUM);
            if (intervalClause.isOrTerminated()) {
                writer.write(" or terminated");
            }
        }

        delimiter = "";
        if (!defines.isEmpty()) {
            writer.write(" define ");
            for (MatchRecognizeDefine def : defines) {
                writer.write(delimiter);
                writer.write(def.getName());
                writer.write(" as ");
                def.getExpression().toEPL(writer, ExpressionPrecedenceEnum.MINIMUM);
                delimiter = ", ";
            }
        }

        writer.write(")");
    }

    /**
     * Get partition expressions.
     *
     * @return partition expressions
     */
    public List<Expression> getPartitionExpressions() {
        return partitionExpressions;
    }

    /**
     * Set partition expressions.
     *
     * @param partitionExpressions partition expressions
     */
    public void setPartitionExpressions(List<Expression> partitionExpressions) {
        this.partitionExpressions = partitionExpressions;
    }

    /**
     * Returns measures.
     *
     * @return measures
     */
    public List<SelectClauseExpression> getMeasures() {
        return measures;
    }

    /**
     * Sets measures.
     *
     * @param measures to set
     */
    public void setMeasures(List<SelectClauseExpression> measures) {
        this.measures = measures;
    }

    /**
     * Indicator whether all matches.
     *
     * @return all matches
     */
    public boolean isAll() {
        return all;
    }

    /**
     * Sets indicator whether all matches.
     *
     * @param all all matches
     */
    public void setAll(boolean all) {
        this.all = all;
    }

    /**
     * Returns skip-clause.
     *
     * @return skip-clause
     */
    public MatchRecognizeSkipClause getSkipClause() {
        return skipClause;
    }

    /**
     * Sets the skip-clause.
     *
     * @param skipClause to set
     */
    public void setSkipClause(MatchRecognizeSkipClause skipClause) {
        this.skipClause = skipClause;
    }

    /**
     * Returns the defines-clause
     *
     * @return defines-clause
     */
    public List<MatchRecognizeDefine> getDefines() {
        return defines;
    }

    /**
     * Sets the defines-clause
     *
     * @param defines to set
     */
    public void setDefines(List<MatchRecognizeDefine> defines) {
        this.defines = defines;
    }

    /**
     * Returns the interval clause.
     *
     * @return interval clause
     */
    public MatchRecognizeIntervalClause getIntervalClause() {
        return intervalClause;
    }

    /**
     * Sets the interval clause.
     *
     * @param intervalClause interval clause
     */
    public void setIntervalClause(MatchRecognizeIntervalClause intervalClause) {
        this.intervalClause = intervalClause;
    }

    /**
     * Returns regex-pattern.
     *
     * @return pattern
     */
    public MatchRecognizeRegEx getPattern() {
        return pattern;
    }

    /**
     * Sets regex-pattern.
     *
     * @param pattern to set
     */
    public void setPattern(MatchRecognizeRegEx pattern) {
        this.pattern = pattern;
    }
}

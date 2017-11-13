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

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * A clause to insert, update or delete to/from a named window based on a triggering event arriving and correlated to the named window events to be updated.
 */
public class OnMergeClause extends OnClause {
    private static final long serialVersionUID = 0L;

    private String windowName;
    private String optionalAsName;
    private List<OnMergeMatchItem> matchItems;
    private OnMergeMatchedInsertAction insertNoMatch;

    /**
     * Ctor.
     */
    public OnMergeClause() {
        matchItems = new ArrayList<OnMergeMatchItem>();
    }

    /**
     * Creates an on-update clause.
     *
     * @param windowName     is the named window name
     * @param optionalAsName is the optional as-provided name
     * @param matchItems     is the matched and non-matched action items
     * @return on-update clause without assignments
     */
    public static OnMergeClause create(String windowName, String optionalAsName, List<OnMergeMatchItem> matchItems) {
        return new OnMergeClause(windowName, optionalAsName, matchItems);
    }

    /**
     * Ctor.
     *
     * @param windowName     is the named window name
     * @param optionalAsName is the as-provided name of the named window
     * @param matchItems     is the matched and non-matched action items
     */
    public OnMergeClause(String windowName, String optionalAsName, List<OnMergeMatchItem> matchItems) {
        this.windowName = windowName;
        this.optionalAsName = optionalAsName;
        this.matchItems = matchItems;
    }

    /**
     * Renders the clause in textual representation.
     *
     * @param writer              to output to
     * @param optionalWhereClause where clause if present, or null
     * @param formatter           for newline-whitespace formatting
     */
    public void toEPL(StringWriter writer, Expression optionalWhereClause, EPStatementFormatter formatter) {
        formatter.beginMerge(writer);
        writer.write("merge ");
        writer.write(windowName);

        if (optionalAsName != null) {
            writer.write(" as ");
            writer.write(optionalAsName);
        }

        if (insertNoMatch != null) {
            writer.append(" ");
            insertNoMatch.toEPL(writer);
        } else {
            if (optionalWhereClause != null) {
                formatter.beginMergeWhere(writer);
                writer.write("where ");
                optionalWhereClause.toEPL(writer, ExpressionPrecedenceEnum.MINIMUM);
            }

            for (OnMergeMatchItem item : matchItems) {
                item.toEPL(writer, formatter);
            }
        }
    }

    /**
     * Returns the name of the named window to update.
     *
     * @return named window name
     */
    public String getWindowName() {
        return windowName;
    }

    /**
     * Sets the name of the named window.
     *
     * @param windowName window name
     */
    public void setWindowName(String windowName) {
        this.windowName = windowName;
    }

    /**
     * Returns the as-provided name for the named window.
     *
     * @return name or null
     */
    public String getOptionalAsName() {
        return optionalAsName;
    }

    /**
     * Sets the as-provided for the named window.
     *
     * @param optionalAsName name to set for window
     */
    public void setOptionalAsName(String optionalAsName) {
        this.optionalAsName = optionalAsName;
    }

    /**
     * Add a new action to the list of actions.
     *
     * @param action to add
     * @return clause
     */
    public OnMergeClause addAction(OnMergeMatchItem action) {
        matchItems.add(action);
        return this;
    }

    /**
     * Returns all actions.
     *
     * @return actions
     */
    public List<OnMergeMatchItem> getMatchItems() {
        return matchItems;
    }

    /**
     * Sets all actions.
     *
     * @param matchItems to set
     */
    public void setMatchItems(List<OnMergeMatchItem> matchItems) {
        this.matchItems = matchItems;
    }

    /**
     * Sets an optional insert to executed without a match-clause. If set indicates there is no match-clause.
     * @param insertNoMatch insert
     */
    public void setInsertNoMatch(OnMergeMatchedInsertAction insertNoMatch) {
        this.insertNoMatch = insertNoMatch;
    }

    /**
     * Reutrns an optional insert to executed without a match-clause. If set indicates there is no match-clause.
     * @return insert
     */
    public OnMergeMatchedInsertAction getInsertNoMatch() {
        return insertNoMatch;
    }
}
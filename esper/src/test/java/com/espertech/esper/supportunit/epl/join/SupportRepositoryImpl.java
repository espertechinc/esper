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
package com.espertech.esper.supportunit.epl.join;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.epl.join.rep.Cursor;
import com.espertech.esper.epl.join.rep.Repository;
import com.espertech.esper.epl.join.rep.SingleCursorIterator;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class SupportRepositoryImpl implements Repository {
    private List<Cursor> cursorList = new LinkedList<Cursor>();
    private List<Set<EventBean>> lookupResultsList = new LinkedList<Set<EventBean>>();
    private List<Integer> resultStreamList = new LinkedList<Integer>();

    public Iterator<Cursor> getCursors(int lookupStream) {
        return new SingleCursorIterator(new Cursor(SupportJoinResultNodeFactory.makeEvent(), 0, null));
    }

    public void addResult(Cursor cursor, Set<EventBean> lookupResults, int resultStream) {
        cursorList.add(cursor);
        lookupResultsList.add(lookupResults);
        resultStreamList.add(resultStream);
    }

    public List<Cursor> getCursorList() {
        return cursorList;
    }

    public List<Set<EventBean>> getLookupResultsList() {
        return lookupResultsList;
    }

    public List<Integer> getResultStreamList() {
        return resultStreamList;
    }
}

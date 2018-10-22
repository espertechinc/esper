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
package com.espertech.esper.regressionlib.support.bookexample;

import java.io.Serializable;

public class OrderBean implements Serializable {
    private OrderWithItems orderdetail;
    private BookDesc[] books;
    private GameDesc[] games;

    public OrderBean(OrderWithItems order, BookDesc[] books, GameDesc[] games) {
        this.books = books;
        this.games = games;
        this.orderdetail = order;
    }

    public BookDesc[] getBooks() {
        return books;
    }

    public OrderWithItems getOrderdetail() {
        return orderdetail;
    }

    public GameDesc[] getGames() {
        return games;
    }
}

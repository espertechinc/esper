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
package com.espertech.esper.supportregression.bean.bookexample;

public class OrderBeanFactory {
    public static OrderBean makeEventOne() {
        Order order = new Order("PO200901",
                new OrderItem[]{
                        new OrderItem("A001", "10020", 10, 11.95),
                        new OrderItem("A002", "10021", 25, 7.50),
                        new OrderItem("A003", "10020", 30, 10),
                });
        return new OrderBean(order, getBookDesc(), new GameDesc[0]);
    }

    public static OrderBean makeEventTwo() {
        Order order = new Order("PO200902",
                new OrderItem[]{new OrderItem("B001", "10022", 5, 99.50)});

        return new OrderBean(order, getBookDesc(), new GameDesc[0]);
    }

    public static OrderBean makeEventThree() {
        Order order = new Order("PO200903",
                new OrderItem[]{
                        new OrderItem("C001", "10025", 52, 99.50),
                        new OrderItem("C001", "10024", 51, 41.50),
                        new OrderItem("C001", "10021", 50, 30.50)
                });

        return new OrderBean(order, getBookDesc(),
                new GameDesc[]{new GameDesc("GA01", "Castlevania", "Eidos",
                        new Review[]{
                                new Review(100, "best game ever"),
                                new Review(101, "good platformer")
                        })
                });
    }

    public static OrderBean makeEventFour() {
        Order order = new Order("PO200904",
                new OrderItem[0]);
        return new OrderBean(order, new BookDesc[]{
                new BookDesc("10031", "Foundation 2", "Isaac Asimov", 15.00d,
                        new Review[]{
                                new Review(201, "great book")
                        }),
                new BookDesc("10032", "Red Planet", "Robert A Heinlein", 13.00d, new Review[0]),
        }, new GameDesc[0]);
    }

    private static BookDesc[] getBookDesc() {
        return new BookDesc[]{
                new BookDesc("10020", "Enders Game", "Orson Scott Card", 24.00d,
                        new Review[]{
                                new Review(1, "best book ever"),
                                new Review(2, "good science fiction")
                        }),
                new BookDesc("10021", "Foundation 1", "Isaac Asimov", 35.00d,
                        new Review[]{
                                new Review(10, "great book")
                        }),
                new BookDesc("10022", "Stranger in a Strange Land", "Robert A Heinlein", 27.00d, new Review[0])
        };
    }
}

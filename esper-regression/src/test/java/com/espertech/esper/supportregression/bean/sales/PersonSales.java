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
package com.espertech.esper.supportregression.bean.sales;

import java.util.ArrayList;
import java.util.List;

public class PersonSales {
    private List<Person> persons;
    private List<Sale> sales;

    public PersonSales(List<Person> persons, List<Sale> sales) {
        this.persons = persons;
        this.sales = sales;
    }

    public List<Person> getPersons() {
        return persons;
    }

    public List<Sale> getSales() {
        return sales;
    }

    public static PersonSales make() {
        List<Person> persons = new ArrayList<Person>();
        persons.add(new Person("Jim", 19));
        persons.add(new Person("Henry", 20));
        persons.add(new Person("Peter", 50));
        persons.add(new Person("Boris", 42));

        List<Sale> sales = new ArrayList<Sale>();
        sales.add(new Sale(persons.get(0), persons.get(1), 1000));
        sales.add(new Sale(persons.get(2), persons.get(3), 5000));

        return new PersonSales(persons, sales);
    }
}

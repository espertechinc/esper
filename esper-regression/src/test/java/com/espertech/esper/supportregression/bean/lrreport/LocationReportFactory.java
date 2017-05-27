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
package com.espertech.esper.supportregression.bean.lrreport;

import java.util.ArrayList;
import java.util.List;

public class LocationReportFactory {

    public static LocationReport makeSmall() {

        List<Item> items = new ArrayList<Item>();
        items.add(new Item("P00002", new Location(40, 40), "P", null));
        items.add(new Item("L00001", new Location(42, 41), "L", "P00002"));    // This is luggage #L00001 beloning to P00002
        items.add(new Item("P00020", new Location(0, 0), "P", null));
        return new LocationReport(items);
    }

    public static LocationReport makeLarge() {

        List<Item> items = new ArrayList<Item>();
        items.add(new Item("P00002", new Location(40, 40), "P", null));
        items.add(new Item("L00001", new Location(42, 41), "L", "P00002"));    // This is luggage #L00001 beloning to P00002
        items.add(new Item("L00002", new Location(43, 43), "L", "P00002"));
        items.add(new Item("P00001", new Location(10, 10), "P", null));
        items.add(new Item("L00000", new Location(99, 97), "L", "P00001"));
        items.add(new Item("P00004", new Location(20, 20), "P", null));
        items.add(new Item("P00002", new Location(40, 40), "P", null));
        items.add(new Item("L00003", new Location(29, 26), "L", "P00004"));
        items.add(new Item("E00011", new Location(90, 95), "P", null));
        items.add(new Item("A00010", new Location(104, 101), "L", "E00011"));
        items.add(new Item("A00011", new Location(96, 100), "L", "E00011"));
        items.add(new Item("E00010", new Location(90, 95), "P", null));
        items.add(new Item("L00009", new Location(102, 101), "L", "E00010"));
        items.add(new Item("P00005", new Location(30, 30), "P", null));
        items.add(new Item("L00004", new Location(26, 27), "L", "P00005"));
        items.add(new Item("L00005", new Location(30, 28), "L", "P00005"));
        items.add(new Item("P00007", new Location(90, 95), "P", null));
        items.add(new Item("L00006", new Location(96, 100), "L", "P00007"));
        items.add(new Item("P00008", new Location(100, 100), "P", null));
        items.add(new Item("L00007", new Location(10, 12), "L", "P00008"));
        items.add(new Item("L00008", new Location(10, 12), "L", "P00008"));
        return new LocationReport(items);
    }

    /**
     * Return all luggages separated from the owner.
     *
     * @param lr event
     * @return list
     */
    public static List<Item> findSeparatedLuggage(LocationReport lr) {
        // loop over all luggages
        // find the location of the owner of the luggage
        // compute distance luggage to owner
        // if distance > 10 add original-owner

        List<Item> result = new ArrayList<Item>();
        for (Item item : lr.getItems()) {
            if (item.getType().equals("L")) {
                String belongTo = item.getAssetIdPassenger();

                Item owner = null;
                for (Item ownerItem : lr.getItems()) {
                    if (ownerItem.getType().equals("P")) {
                        if (ownerItem.getAssetId().equals(belongTo)) {
                            owner = ownerItem;
                        }
                    }
                }

                if (owner == null) {
                    continue;
                }

                double distanceOwner = LRUtil.distance(owner.getLocation().getX(), owner.getLocation().getY(),
                        item.getLocation().getX(), item.getLocation().getY());
                if (distanceOwner > 20) {
                    result.add(item);
                }
            }
        }
        return result;
    }

    public static Item findPotentialNewOwner(LocationReport lr, Item luggageItem) {

        // for a given luggage find the owner that is nearest to it
        Item passenger = null;
        double distanceMin = Integer.MAX_VALUE;
        for (Item item : lr.getItems()) {
            if (item.getType().equals("P")) {
                String who = item.getAssetId();
                if (luggageItem.getAssetIdPassenger().equals(who)) {
                    continue;
                }

                double distance = LRUtil.distance(luggageItem.getLocation().getX(), luggageItem.getLocation().getY(),
                        item.getLocation().getX(), item.getLocation().getY());

                if (passenger == null || distance < distanceMin) {
                    passenger = item;
                    distanceMin = distance;
                }
            }
        }
        return passenger;
    }
}

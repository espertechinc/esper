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
package com.espertech.esper.pattern;

import java.io.Serializable;
import java.util.Arrays;

public class EvalStateNodeNumber implements Serializable {
    private static final long serialVersionUID = 4881487549563453035L;
    private int[] stateNumber;
    private int hashCode;

    /**
     * Ctor - constructs a top-level node number.
     */
    public EvalStateNodeNumber() {
        stateNumber = new int[0];
        computeHashCode();
    }

    /**
     * Contructs a given node number.
     *
     * @param number to contruct
     */
    public EvalStateNodeNumber(int[] number) {
        this.stateNumber = number;
        computeHashCode();
    }

    /**
     * Get the depth of the node number.
     *
     * @return ordinal
     */
    public int getOrdinalNumber() {
        return stateNumber[stateNumber.length - 1];
    }

    /**
     * Generates a new child node number to the current node number with the given child id.
     *
     * @param newStateNumber child's node num
     * @return child node num
     */
    public EvalStateNodeNumber newChildNum(int newStateNumber) {
        int[] num = new int[stateNumber.length + 1];
        System.arraycopy(stateNumber, 0, num, 0, stateNumber.length);
        num[stateNumber.length] = newStateNumber;
        return new EvalStateNodeNumber(num);
    }

    /**
     * Generates a new sibling node number to the current node.
     *
     * @return sibling node
     */
    public EvalStateNodeNumber newSiblingState() {
        int size = stateNumber.length;
        int[] num = new int[size];
        System.arraycopy(stateNumber, 0, num, 0, size);
        num[size - 1] = stateNumber[size - 1] + 1;
        return new EvalStateNodeNumber(num);
    }

    public String toString() {
        return Arrays.toString(stateNumber);
    }

    /**
     * Returns the internal number representation.
     *
     * @return state number
     */
    public int[] getStateNumber() {
        return stateNumber;
    }

    public int hashCode() {
        return hashCode;
    }

    public boolean equals(Object otherObj) {
        if (!(otherObj instanceof EvalStateNodeNumber)) {
            return false;
        }
        EvalStateNodeNumber other = (EvalStateNodeNumber) otherObj;
        int[] otherNum = other.getStateNumber();
        if (otherNum.length != stateNumber.length) {
            return false;
        }
        for (int i = 0; i < stateNumber.length; i++) {
            if (otherNum[i] != stateNumber[i]) {
                return false;
            }
        }
        return true;
    }

    private void computeHashCode() {
        hashCode = 7;
        for (int i = 0; i < stateNumber.length; i++) {
            hashCode ^= stateNumber[i];
        }
    }
}

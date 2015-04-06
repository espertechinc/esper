/**************************************************************************************
 * Copyright (C) 2006-2015 EsperTech Inc. All rights reserved.                        *
 * http://www.espertech.com/esper                                                          *
 * http://www.espertech.com                                                           *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the GPL license       *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package com.espertech.esper.collection;

/**
 * General-purpose pair of values of any type. The pair equals another pair if
 * the objects that form the pair equal in any order, ie. first pair first object equals (.equals)
 * the second pair first object or second object, and the first pair second object equals the second pair first object
 * or second object.
 */
public final class InterchangeablePair<First,Second>
{
    private First first;
    private Second second;

    /**
     * Construct pair of values.
     * @param first is the first value
     * @param second is the second value
     */
    public InterchangeablePair(final First first, final Second second)
    {
        this.first = first;
        this.second = second;
    }

    /**
     * Returns first value within pair.
     * @return first value within pair
     */
    public First getFirst()
    {
        return first;
    }

    /**
     * Returns second value within pair.
     * @return second value within pair
     */
    public Second getSecond()
    {
        return second;
    }

    /**
     * Set the first value of the pair to a new value.
     * @param first value to be set
     */
    public void setFirst(First first)
    {
        this.first = first;
    }

    /**
     * Set the second value of the pair to a new value.
     * @param second value to be set
     */
    public void setSecond(Second second)
    {
        this.second = second;
    }

    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }

        if (!(obj instanceof InterchangeablePair))
        {
            return false;
        }

        InterchangeablePair other = (InterchangeablePair) obj;

        if ((first == null) && (second == null))
        {
            return ((other.first == null) && (other.second == null));
        }

        if ((first == null) && (second != null))
        {
            if (other.second != null)
            {
                return (other.first == null) && second.equals(other.second);
            }
            else
            {
                return second.equals(other.first);
            }
        }

        if ((first != null) && (second == null))
        {
            if (other.first != null)
            {
                return first.equals(other.first) && (other.second == null);
            }
            else
            {
                return first.equals(other.second);
            }
        }

        return ( (first.equals(other.first) && second.equals(other.second)) ||
                 (first.equals(other.second) && second.equals(other.first)) );
    }

    public int hashCode()
    {
        return (first == null ? 0 : first.hashCode()) ^
                (second == null ? 0 : second.hashCode());
    }

    public String toString()
    {
        return "Pair [" + first + ':' + second + ']';
    }
}

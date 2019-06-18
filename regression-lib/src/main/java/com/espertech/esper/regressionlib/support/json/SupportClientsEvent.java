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
package com.espertech.esper.regressionlib.support.json;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class SupportClientsEvent {

    public List<Client> clients;

    public static final class Client {

        public long _id;
        public int index;
        public UUID guid;
        public boolean isActive;
        public BigDecimal balance;
        public String picture;
        public int age;
        public EyeColor eyeColor;
        public String name;
        public String gender;
        public String company;
        public String[] emails;
        public long[] phones;
        public String address;
        public String about;
        public LocalDate registered;
        public double latitude;
        public double longitude;
        public List<String> tags;
        public List<Partner> partners;

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof Client)) {
                return false;
            }
            Client client = (Client) o;
            return index == client.index &&
                isActive == client.isActive &&
                age == client.age &&
                Math.abs(Double.doubleToLongBits(client.latitude) - Double.doubleToLongBits(latitude)) < 3 &&
                Math.abs(Double.doubleToLongBits(client.longitude) - Double.doubleToLongBits(longitude)) < 3 &&
                Objects.equals(_id, client._id) &&
                Objects.equals(guid, client.guid) &&
                balance.compareTo(client.balance) == 0 &&
                Objects.equals(picture, client.picture) &&
                Objects.equals(eyeColor, client.eyeColor) &&
                Objects.equals(name, client.name) &&
                Objects.equals(gender, client.gender) &&
                Objects.equals(company, client.company) &&
                Arrays.equals(emails, client.emails) &&
                Arrays.equals(phones, client.phones) &&
                Objects.equals(address, client.address) &&
                Objects.equals(about, client.about) &&
                Objects.equals(registered, client.registered) &&
                Objects.equals(tags, client.tags) &&
                Objects.equals(partners, client.partners);
        }

        @Override
        public int hashCode() {
            return Objects.hash(_id, index, guid, isActive, balance, picture, age, eyeColor, name, gender, company, emails, phones, address, about, registered, tags, partners);
        }

        private String toStr(long[] nums) {
            StringBuilder sb = new StringBuilder();
            sb.append('[');
            boolean first = true;
            for (long l : nums) {
                if (first) first = false;
                else sb.append(',');
                sb.append(l);
            }
            sb.append(']');
            return sb.toString();
        }

        @Override
        public String toString() {
            return "JsonDataObj{" + "_id=" + _id + ", index=" + index + ", guid=" + guid + ", isActive=" + isActive + ", balance=" + balance + ", picture=" + picture + ", age=" + age + ", eyeColor=" + eyeColor + ", name=" + name + ", gender=" + gender + ", company=" + company + ", emails=" + (emails != null ? Arrays.asList(emails) : null) + ", phones=" + toStr(phones) + ", address=" + address + ", about=" + about + ", registered=" + registered + ", latitude=" + latitude + ", longitude=" + longitude + ", tags=" + tags + ", partners=" + partners + '}';
        }
    }

    public enum EyeColor {
        BROWN,
        BLUE,
        GREEN;

        public static EyeColor fromNumber(int i) {
            if (i == 0) return BROWN;
            if (i == 1) return BLUE;
            return GREEN;
        }
    }

    public static final class Partner {

        public long id;
        public String name;
        public OffsetDateTime since;

        public Partner() {
        }

        public static Partner create(long id, String name, OffsetDateTime since) {
            Partner partner = new Partner();
            partner.id = id;
            partner.name = name;
            partner.since = since;
            return partner;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Partner)) return false;

            Partner partner = (Partner) o;

            if (id != partner.id) return false;
            if (since == null && partner.since != null || since != null && !since.isEqual(partner.since)) return false;
            return name != null ? name.equals(partner.name) : partner.name == null;
        }

        @Override
        public int hashCode() {
            int result = (int) id;
            result = 31 * result + (since != null ? since.hashCode() : 0);
            result = 31 * result + (name != null ? name.hashCode() : 0);
            return result;
        }

        @Override
        public String toString() {
            return "Partner{" + "id=" + id + ", name=" + name + ", since=" + since + '}';
        }
    }
}

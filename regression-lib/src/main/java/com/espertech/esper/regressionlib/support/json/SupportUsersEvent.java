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

import java.util.List;
import java.util.Objects;

public class SupportUsersEvent {

    public List<User> users;

    public static final class User {

        public String _id;
        public int index;
        public String guid;
        public boolean isActive;
        public String balance;
        public String picture;
        public int age;
        public String eyeColor;
        public String name;
        public String gender;
        public String company;
        public String email;
        public String phone;
        public String address;
        public String about;
        public String registered;
        public double latitude;
        public double longitude;
        public List<String> tags;
        public List<Friend> friends;
        public String greeting;
        public String favoriteFruit;

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof User)) {
                return false;
            }
            User user = (User) o;
            return index == user.index &&
                isActive == user.isActive &&
                age == user.age &&
                Math.abs(Double.doubleToLongBits(user.latitude) - Double.doubleToLongBits(latitude)) < 3 &&
                Math.abs(Double.doubleToLongBits(user.longitude) - Double.doubleToLongBits(longitude)) < 3 &&
                Objects.equals(_id, user._id) &&
                Objects.equals(guid, user.guid) &&
                Objects.equals(balance, user.balance) &&
                Objects.equals(picture, user.picture) &&
                Objects.equals(eyeColor, user.eyeColor) &&
                Objects.equals(name, user.name) &&
                Objects.equals(gender, user.gender) &&
                Objects.equals(company, user.company) &&
                Objects.equals(email, user.email) &&
                Objects.equals(phone, user.phone) &&
                Objects.equals(address, user.address) &&
                Objects.equals(about, user.about) &&
                Objects.equals(registered, user.registered) &&
                Objects.equals(tags, user.tags) &&
                Objects.equals(friends, user.friends) &&
                Objects.equals(greeting, user.greeting) &&
                Objects.equals(favoriteFruit, user.favoriteFruit);
        }

        @Override
        public int hashCode() {
            return Objects.hash(_id, index, guid, isActive, balance, picture, age, eyeColor, name, gender, company, email, phone, address, about, registered, tags, friends, greeting, favoriteFruit);
        }

        @Override
        public String toString() {
            return "JsonDataObj{" + "_id=" + _id + ", index=" + index + ", guid=" + guid + ", isActive=" + isActive + ", balance=" + balance + ", picture=" + picture + ", age=" + age + ", eyeColor=" + eyeColor + ", name=" + name + ", gender=" + gender + ", company=" + company + ", email=" + email + ", phone=" + phone + ", address=" + address + ", about=" + about + ", registered=" + registered + ", latitude=" + latitude + ", longitude=" + longitude + ", tags=" + tags + ", friends=" + friends + ", greeting=" + greeting + ", favoriteFruit=" + favoriteFruit + '}';
        }
    }

    public static final class Friend {


        public String id;

        public String name;

        public Friend() {
        }

        public static Friend create(String id, String name) {
            Friend friend = new Friend();
            friend.id = id;
            friend.name = name;
            return friend;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Friend)) return false;

            Friend friend = (Friend) o;

            if (id != null ? !id.equals(friend.id) : friend.id != null) return false;
            return name != null ? name.equals(friend.name) : friend.name == null;
        }

        @Override
        public int hashCode() {
            int result = id != null ? id.hashCode() : 0;
            result = 31 * result + (name != null ? name.hashCode() : 0);
            return result;
        }

        @Override
        public String toString() {
            return "Friend{" + "id=" + id + ", name=" + name + '}';
        }
    }
}

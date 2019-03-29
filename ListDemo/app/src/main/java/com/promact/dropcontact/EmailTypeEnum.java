package com.promact.dropcontact;

/**
 * Created by grishma on 14-11-2016.
 */
public class EmailTypeEnum {
    public enum Type {

        TYPE_HOME("home", 1),
        TYPE_WORK("work", 2),
        TYPE_OTHER("other", 3),
        TYPE_MOBILE("mobile", 4),
        TYPE_CUSTOM("custom", 0);

        private int emailType;
        private String emailTypeString;

        Type(String mtype, int type) {
            this.emailTypeString = mtype;
            this.emailType = type;
        }

        public int getValue() {
            return this.emailType;
        }

        public String getName() {
            return this.emailTypeString;
        }

        public static Type fromInteger(int type) {

            for (Type objType : Type.values()) {
                if (type == (objType.emailType)) {
                    return objType;
                }
            }
            return null;
        }

        public static Type fromString(String mtype) {

            for (Type objType : Type.values()) {
                if (mtype.equals(objType.emailTypeString)) {
                    return objType;
                }
            }
            return fromString("custom");
        }

        @Override
        public String toString() {
            String emailIdType;
            emailIdType = convertEmailTypeToString(this);
            return emailIdType;
        }

        private String convertEmailTypeToString(Type type) {
            switch (type) {
                case TYPE_HOME:
                    return "Home: " + emailType;
                case TYPE_MOBILE:
                    return "Mobile: " + emailType;
                case TYPE_WORK:
                    return "Work: " + emailType;
                case TYPE_OTHER:
                    return "Other: " + emailType;
                case TYPE_CUSTOM:
                    return "Custom: " + emailType;
                default:
                    return "";
            }
        }

    }
}

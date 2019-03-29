package com.promact.dropcontact;


/**
 * Created by grishma on 11-11-2016.
 */
public class PhoneTypeEnum {
    public enum Type {

        TYPE_HOME("home", 1),
        TYPE_MOBILE("mobile", 2),
        TYPE_WORK("work", 3),
        TYPE_WORKFAX("work fax", 4),
        TYPE_HOMEFAX("home fax", 5),
        TYPE_PAGER("pager", 6),
        TYPE_OTHER("other", 7),
        TYPE_CALLBACK("callback", 8),
        TYPE_CAR("car", 9),
        TYPE_COMPANY_MAIN("company main", 10),
        TYPE_ISDN("isdn", 11),
        TYPE_MAIN("main", 12),
        TYPE_OTHER_FAX("other fax", 13),
        TYPE_RADIO("radio", 14),
        TYPE_TELEX("telex", 15),
        TYPE_TTY_TDD("tty tdd", 16),
        TYPE_WORK_MOBILE("work mobile", 17),
        TYPE_WORK_PAGER("work pager", 18),
        TYPE_ASSISTANT("assistant", 19),
        TYPE_MMS("mms", 20),
        TYPE_CUSTOM("custom", 0);

        private int phoneType;
        private String phoneTypeString;

        Type(String mtype, int type) {
            this.phoneTypeString = mtype;
            this.phoneType = type;
        }

        public int getValue() {
            return this.phoneType;
        }

        public String getName() {
            return this.phoneTypeString;
        }

        public static Type fromInteger(int type) {
            for (Type objType : Type.values()) {
                if (type == (objType.phoneType)) {
                    return objType;
                }
            }
            return null;
        }

        public static Type fromString(String mtype) {
            for (Type objType : Type.values()) {
                if (mtype.equals(objType.phoneTypeString)) {
                    return objType;
                }
            }
            return fromString("custom");
        }

        @Override
        public String toString() {
            String phoneNumberType;
            phoneNumberType = convertPhoneTypeToString(this);
            return phoneNumberType;
        }

        private String convertPhoneTypeToString(Type type) {
            switch (type) {
                case TYPE_HOME:
                    return "Home: " + phoneType;
                case TYPE_MOBILE:
                    return "Mobile: " + phoneType;
                case TYPE_WORK:
                    return "Work: " + phoneType;
                case TYPE_WORKFAX:
                    return "WorkFax: " + phoneType;
                case TYPE_HOMEFAX:
                    return "HomeFax: " + phoneType;
                case TYPE_PAGER:
                    return "Pager: " + phoneType;
                case TYPE_OTHER:
                    return "Other: " + phoneType;
                case TYPE_CALLBACK:
                    return "Callback: " + phoneType;
                case TYPE_CAR:
                    return "Car: " + phoneType;
                case TYPE_COMPANY_MAIN:
                    return "CompanyMain: " + phoneType;
                case TYPE_ISDN:
                    return "Isdn: " + phoneType;
                case TYPE_MAIN:
                    return "Main: " + phoneType;
                case TYPE_OTHER_FAX:
                    return "OtherFax: " + phoneType;
                case TYPE_RADIO:
                    return "Radio: " + phoneType;
                case TYPE_TELEX:
                    return "Telex: " + phoneType;
                case TYPE_TTY_TDD:
                    return "TtyTdd: " + phoneType;
                case TYPE_WORK_MOBILE:
                    return "WorkMobile: " + phoneType;
                case TYPE_WORK_PAGER:
                    return "WorkPager: " + phoneType;
                case TYPE_ASSISTANT:
                    return "Assistant: " + phoneType;
                case TYPE_MMS:
                    return "Mms: " + phoneType;
                case TYPE_CUSTOM:
                    return "Custom: " + phoneType;
                default:
                    return "";
            }
        }

    }
}
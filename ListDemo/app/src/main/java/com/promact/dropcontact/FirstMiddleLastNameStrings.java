package com.promact.dropcontact;

/**
 * Created by grishma on 24-05-2017.
 */
public class FirstMiddleLastNameStrings {
    private String firstNameOfContact;
    private String middleNameOfContact;
    private String lastNameOfContact;

    public FirstMiddleLastNameStrings() {
    }

    public FirstMiddleLastNameStrings(String firstName, String middleName, String lastName) {
        this.firstNameOfContact = firstName;
        this.middleNameOfContact = middleName;
        this.lastNameOfContact = lastName;
    }

    public String getFirstNameOfContact() {
        return firstNameOfContact;
    }

    public void setFirstNameOfContact(String firstNameOfContact) {
        this.firstNameOfContact = firstNameOfContact;
    }

    public String getMiddleNameOfContact() {
        return middleNameOfContact;
    }

    public void setMiddleNameOfContact(String middleNameOfContact) {
        this.middleNameOfContact = middleNameOfContact;
    }

    public String getLastNameOfContact() {
        return lastNameOfContact;
    }

    public void setLastNameOfContact(String lastNameOfContact) {
        this.lastNameOfContact = lastNameOfContact;
    }
}

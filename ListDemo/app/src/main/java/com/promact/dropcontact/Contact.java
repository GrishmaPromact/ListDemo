package com.promact.dropcontact;

import java.util.List;

/**
 * Created by grishma on 02-08-2016.
 */
public class Contact {
    private String name;
    private String firstName;
    private String middleName;
    private String lastName;
    private List<ContactType> phoneNumberList;
    private List<ContactType> emailList;
    private String profile;
    private boolean hasImage;

    public Contact() {
    }

    public Contact(String name,String firstName,String middleName,String lastName, List<ContactType> phoneNumberList, List<ContactType> emailList, String profile,
                   boolean hasImage) {
        this.name = name;
        this.firstName=firstName;
        this.middleName=middleName;
        this.lastName=lastName;
        this.phoneNumberList = phoneNumberList;
        this.emailList = emailList;
        this.profile = profile;
        this.hasImage = hasImage;
    }

    public boolean isHasImage() {
        return hasImage;
    }

    public void setHasImage(boolean hasImage) {
        this.hasImage = hasImage;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<ContactType> getPhoneNumberList() {
        return phoneNumberList;
    }

    public List<ContactType> getEmailList() {
        return emailList;
    }

    public String getProfile() {
        return profile;
    }

    public void setPhoneNumberList(List<ContactType> phoneNumberList) {
        this.phoneNumberList = phoneNumberList;
    }

    public void setEmailList(List<ContactType> emailList) {
        this.emailList = emailList;
    }

    public void setProfile(String profile) {
        this.profile = profile;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getMiddleName() {
        return middleName;
    }

    public void setMiddleName(String middleName) {
        this.middleName = middleName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public char[] charAt(int i) {
        return charAt(i);
    }
}

package com.promact.dropcontact;

import java.util.List;

/**
 * Created by grishma on 12-05-2017.
 */

public interface IDowanloadServerContacts {
    void onTaskComplete(List<Contact> serverContactList);

    void onTaskException(Exception exception);
}

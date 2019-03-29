package com.promact.dropcontact;

import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.v2.DbxClientV2;

/**
 * Created by grishma on 16-08-2016.
 */
public class DropboxClient  {
    private static DbxClientV2 dbxClientV2;
    private DropboxClient() {
        throw new IllegalAccessError("Utility class");
    }
    public static DbxClientV2 getClient(String accessToken) {
        if (dbxClientV2 == null) {
            DbxRequestConfig requestConfig = new DbxRequestConfig("dropbox/sample-app");
            dbxClientV2 = new DbxClientV2(requestConfig, accessToken);
        }
        return dbxClientV2;
    }

}


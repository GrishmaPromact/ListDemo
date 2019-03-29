package com.promact.dropcontact;

/**
 * Created by grishma on 19-09-2016.
 */
public interface IDownloadJsonString<T> {
    void onTaskComplete(T downloadHexString);

    void onTaskException(Exception exception);
}

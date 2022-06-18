package com.pedro.rtpstreamer;

import retrofit2.Callback;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface UserApiCall {

    @GET("/api/key")
    void getUserByName(@Query("user") String user, Callback<String> cb);
}

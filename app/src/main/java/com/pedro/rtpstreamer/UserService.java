package com.pedro.rtpstreamer;

import retrofit2.Callback;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface UserService {

    @GET("/api/key")
    void getUserByName(@Query("user") String user, Callback<LoginResponse> cb);
}

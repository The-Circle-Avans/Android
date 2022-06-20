package com.pedro.rtpstreamer.api;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface UserService {

    @GET("/api/key/{user}")
    Call<LoginResponse> getUserByName(@Path("user") String user);
}

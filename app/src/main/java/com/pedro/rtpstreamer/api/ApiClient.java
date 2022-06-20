package com.pedro.rtpstreamer.api;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {
    private final static String baseURL = "https://thecircle-thruyou.herokuapp.com";
    private static Retrofit retrofit;
    private static Gson gson;

    public static Retrofit getRetrofitInstance() {
        if (retrofit == null) {
            gson = new GsonBuilder()
                    .setLenient()
                    .create();

            retrofit = new Retrofit.Builder()
                    .baseUrl(baseURL)
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .build();
        }
        return retrofit;
    }

}

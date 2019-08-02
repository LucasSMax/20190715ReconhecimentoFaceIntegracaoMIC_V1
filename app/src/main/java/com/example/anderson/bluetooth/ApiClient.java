package com.example.anderson.bluetooth;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {

    //private static final String _baseUrl = "http://192.168.0.100:8000/admin/";
    //private static final String _baseUrl = "http://192.168.43.118:8000/admin/";
   //private static final String _baseUrl = "http://172.18.20.39:8000/admin/";
   private static final String _baseUrl = "http://192.168.25.231:8000/admin/";

    private static Retrofit retrofit;

    public static Retrofit getApiClient()
    {

        if(retrofit==null) {
            retrofit = new Retrofit.Builder().baseUrl(_baseUrl).
                    addConverterFactory(GsonConverterFactory.create()).build();
                    //testing git
        }
        return  retrofit;
    }

}

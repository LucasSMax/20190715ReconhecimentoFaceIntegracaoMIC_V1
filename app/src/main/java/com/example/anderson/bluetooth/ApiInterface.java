package com.example.anderson.bluetooth;

import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;

public interface ApiInterface {

    @FormUrlEncoded
    @POST("img/")
    Call <ImageClass> uploadImage(@Field("image") String image);

    @FormUrlEncoded
    @POST("cadastro/imgs/")
    Call <ImageClass> uploadImageReg(@Field("image") String image, @Field("name") String name,
                                     @Field("phone") String phone, @Field("email") String email);
}

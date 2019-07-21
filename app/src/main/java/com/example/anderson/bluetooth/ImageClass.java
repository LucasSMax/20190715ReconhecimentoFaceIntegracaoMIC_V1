package com.example.anderson.bluetooth;

import com.google.gson.annotations.SerializedName;

public class ImageClass {

    @SerializedName("image")
    private String Image;

    @SerializedName("Name")
    private String name;

    @SerializedName("Phone")
    private String phone;

    @SerializedName("Email")
    private String email;

    @SerializedName("server_response")
    private String Response;

    public String getResponse() {
        return Response;
    }
}

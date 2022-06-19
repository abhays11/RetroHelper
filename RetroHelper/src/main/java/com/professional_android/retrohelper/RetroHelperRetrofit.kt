package com.professional_android.retrohelper

import com.google.gson.JsonObject
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Path

interface RetroHelperRetrofit {

    @retrofit2.http.HTTP(method = "GET", path = "{path}",hasBody = false)
    fun  dynamicNetworkCall(
        @Path("path") path: String
    ) : Call<JsonObject>


    companion object{
        var BASE_URL : String = ""

        var dynamicRetrofit : RetroHelperRetrofit? = null


        fun getInstance(baseUrl : String?=null,isHttpLoggingInterceptorEnable : Boolean) : RetroHelperRetrofit {
            if (baseUrl !=null)
                BASE_URL = baseUrl

            if (dynamicRetrofit == null){
                val retrofit = Retrofit.Builder()

                if (isHttpLoggingInterceptorEnable){
                    val logging = HttpLoggingInterceptor()
                        logging.level = HttpLoggingInterceptor.Level.BODY

                    val okHttpClient = OkHttpClient.Builder()
                        .addNetworkInterceptor(logging)
                        .build()

                    retrofit.client(okHttpClient)
                }

                    retrofit.baseUrl(BASE_URL)
                        .addConverterFactory(GsonConverterFactory.create())

                   dynamicRetrofit =  retrofit.build().create(RetroHelperRetrofit::class.java)
            }

            return dynamicRetrofit!!
        }
    }


}
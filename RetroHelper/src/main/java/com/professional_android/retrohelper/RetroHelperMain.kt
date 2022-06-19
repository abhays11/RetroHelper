package com.professional_android.retrohelper

import android.content.Context
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class RetroHelperMain<T> {

    fun <T> getData(
        context: Context,
        modelClass: Class<T>,
        baseUrl: String,
        path: String,
        onSuccess: (call: Call<JsonObject>, response: Response<JsonObject>, responseModel : T?) -> Unit,
        onFailure: (prevData: ArrayList<T>?, call: Call<JsonObject>, t: Throwable) -> Unit,
        isHttpLoggingInterceptorEnable : Boolean = true
    ) {

        val db = RetroHelperSqlite<T>(context, modelClass)
        val retrofit = RetroHelperRetrofit.getInstance(baseUrl, isHttpLoggingInterceptorEnable)
        val call = retrofit.dynamicNetworkCall(path)

        call.enqueue(
            object : Callback<JsonObject> {

                override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
                    if (response.isSuccessful && response.body() != null) {
                        val model = Gson().fromJson(response.body(),modelClass)

                        CoroutineScope(Dispatchers.IO).launch{
                            db.insertData(model)
                        }

                        onSuccess(call, response,model)
                    }else{
                        onSuccess(call,response,null)
                    }
                }

                override fun onFailure(call: Call<JsonObject>, t: Throwable) {
                    CoroutineScope(Dispatchers.IO).launch {
                        val prevData = db.getData(10)
                        withContext(Dispatchers.Main){

                            onFailure(prevData, call, t)
                        }
                    }


                }
            }
        )
    }


}
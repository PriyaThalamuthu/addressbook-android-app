package com.deepschneider.addressbook.network

import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Query

interface DocumentUploadService {
    @Multipart
    @POST("rest/uploadDocument")
    fun uploadDocument(
        @Query("personId") personId: String?,
        @Part file: MultipartBody.Part?
    ): Call<ResponseBody?>?
}
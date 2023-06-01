package com.deepschneider.addressbook.network

import android.content.Context
import com.android.volley.NetworkResponse
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.HttpHeaderParser
import com.deepschneider.addressbook.utils.NetworkUtils
import com.google.gson.Gson

class SimpleGetRequest(
    url: String,
    private val responseListener: Response.Listener<Any>,
    errorListener: Response.ErrorListener,
    private var context: Context
) : Request<Any>(Method.GET, url, errorListener) {

    private val gson = Gson()

    override fun getHeaders(): MutableMap<String, String> {
        return NetworkUtils.addAuthHeader(super.getHeaders(), context)
    }

    override fun deliverResponse(response: Any) {
        responseListener.onResponse(response)
    }

    override fun getBodyContentType(): String {
        return "application/json; charset=utf-8"
    }

    override fun parseNetworkResponse(response: NetworkResponse?): Response<Any> {
        return Response.success("OK", HttpHeaderParser.parseCacheHeaders(response))
    }
}
package com.deepschneider.addressbook.fragments

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.android.volley.RequestQueue
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.deepschneider.addressbook.databinding.ServiceInfoFragmentBinding
import com.deepschneider.addressbook.dto.BuildInfoDto
import com.deepschneider.addressbook.utils.NetworkUtils
import com.deepschneider.addressbook.utils.Urls
import com.google.gson.Gson

class ServiceInfoFragment : Fragment() {

    private lateinit var binding: ServiceInfoFragmentBinding
    private lateinit var requestQueue: RequestQueue
    private lateinit var listener: FragmentActivity
    private var serverUrl: String? = null
    private val requestTag = "BUILD_INFO_TAG"
    private val gson = Gson()

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is Activity) {
            listener = (context as FragmentActivity)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestQueue = Volley.newRequestQueue(listener)
        serverUrl = NetworkUtils.getServerUrl(listener)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = ServiceInfoFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        updateBuildInfo()
    }

    private fun updateBuildInfo() {
        requestQueue.add(object :
            JsonObjectRequest(Method.GET, serverUrl + Urls.BUILD_INFO, null, { response ->
                val buildInfo = gson.fromJson(response.toString(), BuildInfoDto::class.java)
                binding.buildInfoFragmentVersionInfo.text = "version: " + buildInfo.version?.uppercase()
                binding.buildInfoFragmentBuildInfo.text = "build: " + buildInfo.time?.uppercase()
                binding.buildInfoFragmentServerHost.text = "server host: " + buildInfo.serverHost?.uppercase()
                binding.buildInfoFragmentServerInfo.text = "server: $serverUrl"
            }, { error ->
                Log.d("SERVICE INFO ERROR", error.toString())
            }) {
            override fun getHeaders(): MutableMap<String, String> {
                return NetworkUtils.addAuthHeader(super.getHeaders(), listener)
            }
        }.also { it.tag = requestTag })
    }

    override fun onStop() {
        super.onStop()
        requestQueue.cancelAll(requestTag)
    }
}
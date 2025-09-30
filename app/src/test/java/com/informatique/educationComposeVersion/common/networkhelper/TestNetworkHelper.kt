package com.informatique.educationComposeVersion.common.networkhelper

class TestNetworkHelper : NetworkHelper {
    override fun isNetworkConnected(): Boolean {
        return true
    }
}
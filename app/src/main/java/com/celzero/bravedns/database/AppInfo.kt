/*
Copyright 2020 RethinkDNS developers

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

https://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
package com.celzero.bravedns.database

import android.content.ContentValues
import androidx.room.Entity
import com.celzero.bravedns.service.FirewallManager

@Entity(primaryKeys = ["uid", "packageName"], tableName = "AppInfo")
class AppInfo {
    var packageName: String = ""
    var appName: String = ""
    var uid: Int = 0
    var isSystemApp: Boolean = false
    var firewallStatus: Int = FirewallManager.FirewallStatus.NONE.id
    var appCategory: String = ""
    var wifiDataUsed: Long = 0
    var mobileDataUsed: Long = 0
    var connectionStatus: Int = FirewallManager.ConnectionStatus.ALLOW.id
    var screenOffAllowed: Boolean = false
    var backgroundAllowed: Boolean = false

    override fun equals(other: Any?): Boolean {
        if (other !is AppInfo) return false
        if (packageName != other.packageName) return false
        return true
    }

    override fun hashCode(): Int {
        return this.packageName.hashCode()
    }

    constructor()

    constructor(
        packageName: String,
        appName: String,
        uid: Int,
        isSystemApp: Boolean,
        firewallStatus: Int,
        appCategory: String,
        wifiDataUsed: Long,
        mobileDataUsed: Long,
        metered: Int,
        screenOffAllowed: Boolean,
        backgroundAllowed: Boolean
    ) {
        this.packageName = packageName
        this.appName = appName
        this.uid = uid
        this.isSystemApp = isSystemApp
        this.firewallStatus = firewallStatus
        this.appCategory = appCategory
        this.wifiDataUsed = wifiDataUsed
        this.mobileDataUsed = mobileDataUsed
        this.connectionStatus = metered
        this.screenOffAllowed = screenOffAllowed
        this.backgroundAllowed = backgroundAllowed
    }

    fun fromContentValues(values: ContentValues?): AppInfo? {
        if (values == null) return null
        val a = values.valueSet()
        a.forEach {
            when (it.key) {
                "packageName" -> packageName = it.value as String
                "appName" -> appName = it.value as String
                "uid" -> uid = it.value as Int
                "isSystemApp" -> isSystemApp = (it.value as Int == 1)
                "firewallStatus" -> firewallStatus = it.value as Int
                "appCategory" -> appCategory = it.value as String
                "wifiDataUsed" -> wifiDataUsed = it.value as Long
                "mobileDataUsed" -> mobileDataUsed = it.value as Long
                "connectionStatus" -> connectionStatus = it.value as Int
                "screenOffAllowed" -> screenOffAllowed = (it.value as Int == 1)
                "backgroundAllowed" -> backgroundAllowed = (it.value as Int == 1)
            }
        }
        return AppInfo(
            packageName,
            appName,
            uid,
            isSystemApp,
            firewallStatus,
            appCategory,
            wifiDataUsed,
            mobileDataUsed,
            connectionStatus,
            screenOffAllowed,
            backgroundAllowed
        )
    }
}

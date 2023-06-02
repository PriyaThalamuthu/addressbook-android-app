package com.deepschneider.addressbook.dto

import java.io.Serializable
import java.util.Collections

class AlertDto(headline: String?, type: String?, message: String?) : Serializable {
    var headline: String? = null
    var type: String? = null
    var message: String? = null

    init {
        this.headline = headline
        this.message = message
        this.type = type
    }

    constructor() : this(null, null, null)

    override fun toString(): String {
        return "AlertDto(headline=$headline, type=$type, message=$message)"
    }
}

class FieldDescriptionDto(name: String, displayName: String, width: String, type: String) {

    var name: String? = null
    var displayName: String? = null
    var width: String? = null
    var type: String? = null

    init {
        this.name = name
        this.displayName = displayName
        this.width = width
        this.type = type
    }
}

class FilterDto : Serializable {
    var name: String? = null
    var value: String? = null
    var comparator: String? = null
    var type: String? = null
}

class BuildInfoDto : Serializable {
    var version: String? = null
    var artifact: String? = null
    var serverHost: String? = null
    var group: String? = null
    var name: String? = null
    var time: String? = null
}

class User(
    var login: String,
    var password: String,
    var roles: List<String>
) : Serializable {

    constructor() : this("", "", Collections.emptyList<String>())
}
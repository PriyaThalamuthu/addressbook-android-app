package com.deepschneider.addressbook.dto

import java.io.Serializable

class ContactDto() : Serializable {
    var personId: String? = null
    var id: String? = null
    var type: String? = ""
    var data: String? = null
    var description: String? = null
}

class OrganizationDto() : Serializable {
    var id: String? = null
    var name: String? = null
    var street: String? = null
    var zip: String? = null
    var type: String? = null
    var lastUpdated: String? = null
}

class DocumentDto() : Serializable {
    var id: String? = null
    var personId: String? = null
    var name: String? = null
    var url: String? = null
    var checksum: String? = null
    var size: String? = null
    var createDate: String? = null

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DocumentDto

        if (id != other.id) return false
        if (personId != other.personId) return false
        if (name != other.name) return false
        if (checksum != other.checksum) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id?.hashCode() ?: 0
        result = 31 * result + (personId?.hashCode() ?: 0)
        result = 31 * result + (name?.hashCode() ?: 0)
        result = 31 * result + (checksum?.hashCode() ?: 0)
        return result
    }
}

class PersonDto() : Serializable {

    var id: String? = null
    var orgId: String? = null
    var firstName: String? = null
    var lastName: String? = null
    var resume: String? = null
    var salary: String? = null
}
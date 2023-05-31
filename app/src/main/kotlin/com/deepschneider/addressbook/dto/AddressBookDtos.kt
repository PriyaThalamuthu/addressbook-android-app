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
}

class PersonDto() : Serializable {

    var id: String? = null
    var orgId: String? = null
    var firstName: String? = null
    var lastName: String? = null
    var resume: String? = null
    var salary: String? = null
}
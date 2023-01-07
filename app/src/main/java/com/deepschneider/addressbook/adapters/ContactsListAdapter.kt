package com.deepschneider.addressbook.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.deepschneider.addressbook.R
import com.deepschneider.addressbook.dto.ContactDto

class ContactsListAdapter(
    private val contacts: List<ContactDto>,
    private val contactTypes: Array<String>
) :
    RecyclerView.Adapter<ContactsListAdapter.ContactViewHolder>() {

    inner class ContactViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        var contactData: TextView = view.findViewById(R.id.contact_item_data)
        var contactDesc: TextView = view.findViewById(R.id.contact_item_desc)
        var contactType: TextView = view.findViewById(R.id.contact_item_type)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactViewHolder {
        val view: View =
            LayoutInflater.from(parent.context).inflate(R.layout.contact_list_item, parent, false);
        val contactViewHolder = ContactViewHolder(view);
        return contactViewHolder;
    }

    override fun onBindViewHolder(holder: ContactViewHolder, position: Int) {
        holder.contactData.text = contacts[position].data
        holder.contactDesc.text = contacts[position].description
        contacts[position].type?.let {
            holder.contactType.text = contactTypes[it.toInt()]
        }
    }

    override fun getItemCount(): Int {
        return contacts.size;
    }
}
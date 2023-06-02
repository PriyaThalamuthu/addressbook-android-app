package com.deepschneider.addressbook.adapters

import android.annotation.SuppressLint
import android.app.Activity
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.ArrayAdapter
import android.widget.TextView
import com.deepschneider.addressbook.R
import com.deepschneider.addressbook.dto.OrganizationDto

class OrganizationsListAdapter(
    private val organizationTypes: Array<String>,
    private val organizations: List<OrganizationDto>,
    private val activity: Activity
) : ArrayAdapter<OrganizationDto>(activity, R.layout.organizations_list_item, organizations) {

    @SuppressLint("SetTextI18n")
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val rowView = convertView ?: activity.layoutInflater.inflate(R.layout.organizations_list_item, parent, false)
        rowView.findViewById<TextView>(R.id.organization_list_item_name).text = organizations[position].name
        rowView.findViewById<TextView>(R.id.organization_list_item_address_and_zip).text = "${organizations[position].street} ${organizations[position].zip}"
        rowView.findViewById<TextView>(R.id.organization_list_item_last_updated).text = organizations[position].lastUpdated
        rowView.findViewById<TextView>(R.id.organization_list_item_id).text = organizations[position].id
        organizations[position].type?.let {
            rowView.findViewById<TextView>(R.id.organization_list_item_type).text = organizationTypes[it.toInt() + 1]
        }
        rowView.startAnimation(AnimationUtils.loadAnimation(context, android.R.anim.fade_in))
        return rowView
    }
}
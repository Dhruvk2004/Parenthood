package com.example.boardingscreen

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView

class ListAdapter(
    private val context: Context,
    private val geofenceList: ArrayList<Geofence_details>,
    private val showIcon: Boolean,
    private val onDeleteClick: (Geofence_details) -> Unit
) : BaseAdapter() {

    override fun getCount(): Int = geofenceList.size

    override fun getItem(position: Int): Geofence_details = geofenceList[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view: View
        val viewHolder: ViewHolder

        if (convertView == null) {
            view = LayoutInflater.from(context).inflate(R.layout.geo_bottom_sheet_item, parent, false)
            viewHolder = ViewHolder(view)
            view.tag = viewHolder
        } else {
            view = convertView
            viewHolder = view.tag as ViewHolder
        }

        val geofence = getItem(position)
        viewHolder.tvGeofenceName.text = geofence.geofence_name
        viewHolder.tvPlaceName.text = geofence.location_name
        viewHolder.tvRadius.text = "Radius: ${geofence.radius}m"

        viewHolder.ivDelete.setOnClickListener {
            onDeleteClick(geofence)
        }

        if (showIcon) {
            viewHolder.tvRadius.visibility = View.VISIBLE
            viewHolder.ivDelete.visibility = View.VISIBLE
            viewHolder.ivDelete.setOnClickListener {
                onDeleteClick(geofence)
            }
        } else {
            viewHolder.tvRadius.visibility = View.GONE
            viewHolder.ivDelete.visibility = View.GONE
            viewHolder.ivDelete.setOnClickListener(null) // Prevent memory leaks
        }

        return view
    }

    private class ViewHolder(view: View) {
        val tvGeofenceName: TextView = view.findViewById(R.id.place_name)
        val tvPlaceName: TextView = view.findViewById(R.id.location_name)
        val tvRadius: TextView = view.findViewById(R.id.radius_text)
        val ivDelete: ImageView = view.findViewById(R.id.trash_icon)

    }
}

package com.example.boardingscreen

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView

class AppTimeAdapter(
    private val context: Context,
    private val appTimeList: ArrayList<AppTimeDetails>,
    private val onTimeSetClick: (AppTimeDetails) -> Unit,
    private val onDeleteClick: (AppTimeDetails) -> Unit
) : BaseAdapter() {

    override fun getCount(): Int = appTimeList.size

    override fun getItem(position: Int): AppTimeDetails = appTimeList[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view: View
        val viewHolder: ViewHolder

        if (convertView == null) {
            view = LayoutInflater.from(context).inflate(R.layout.app_list_item, parent, false)
            viewHolder = ViewHolder(view)
            view.tag = viewHolder
        } else {
            view = convertView
            viewHolder = view.tag as ViewHolder
        }

        val appTimeDetails = getItem(position)
        viewHolder.tvAppName.text = appTimeDetails.appName

        if (appTimeDetails.timeLimitSet > 0) {
            // Time limit is set, display the time
            viewHolder.tvTimeSet.text = "${appTimeDetails.timeLimitSet} minutes"
            viewHolder.tvTimeSet.visibility = View.VISIBLE
            viewHolder.ivClock.visibility = View.GONE
            viewHolder.deleteIcon.visibility = View.VISIBLE
        } else {
            // No time limit set, display the clock icon
            viewHolder.ivClock.visibility = View.VISIBLE
            viewHolder.tvTimeSet.visibility = View.GONE
            viewHolder.deleteIcon.visibility = View.GONE
        }

        // Handle click event for setting time
        viewHolder.ivClock.setOnClickListener {
            onTimeSetClick(appTimeDetails)
        }
        viewHolder.tvTimeSet.setOnClickListener {
            onTimeSetClick(appTimeDetails)
        }

        viewHolder.deleteIcon.setOnClickListener {
            onDeleteClick(appTimeDetails)
        }

        return view
    }

    private class ViewHolder(view: View) {
        val tvAppName: TextView = view.findViewById(R.id.app_name)
        val ivClock: ImageView = view.findViewById(R.id.clock_icon)
        val tvTimeSet: TextView = view.findViewById(R.id.time_txt)
        val deleteIcon: ImageView = view.findViewById(R.id.trash_icon)
    }
}

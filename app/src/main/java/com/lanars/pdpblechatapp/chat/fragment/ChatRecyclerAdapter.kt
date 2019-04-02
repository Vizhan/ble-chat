package com.lanars.pdpblechatapp.chat.fragment

import android.support.v7.widget.RecyclerView
import android.view.View
import android.view.ViewGroup
import com.lanars.pdpblechatapp.R
import com.lanars.pdpblechatapp.chat.Message
import com.lanars.pdpblechatapp.common.inflate
import kotlinx.android.synthetic.main.item_recycler_message_in.view.*

class ChatRecyclerAdapter : RecyclerView.Adapter<ChatRecyclerAdapter.ViewHolder>() {
    private val dataSource = arrayListOf<Message>()

    override fun onCreateViewHolder(container: ViewGroup, viewType: Int): ViewHolder =
            ViewHolder(container.inflate(viewType))

    override fun getItemCount(): Int = dataSource.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bindData(dataSource[position])

    override fun getItemViewType(position: Int): Int {
        if (dataSource[position].isOutComing) {
            return R.layout.item_recycler_message_out
        }
        return R.layout.item_recycler_message_in
    }

    fun addItem(item: Message) {
        dataSource.add(0, item)
        notifyDataSetChanged()
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        fun bindData(message: Message) {
            itemView.tvMessage.text = message.message
        }
    }
}
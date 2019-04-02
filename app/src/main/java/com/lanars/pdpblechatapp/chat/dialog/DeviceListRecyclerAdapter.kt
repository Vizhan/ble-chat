package com.lanars.pdpblechatapp.chat.dialog

import android.bluetooth.BluetoothDevice
import android.support.v7.util.DiffUtil
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.View
import android.view.ViewGroup
import com.lanars.pdpblechatapp.R
import com.lanars.pdpblechatapp.common.inflate
import io.reactivex.Flowable
import io.reactivex.disposables.Disposable
import kotlinx.android.synthetic.main.item_device_list.view.*

class DeviceListRecyclerAdapter(private val onClick: (BluetoothDevice) -> Unit) : RecyclerView.Adapter<DeviceListRecyclerAdapter.ViewHolder>() {
    private var data = ArrayList<BluetoothDevice>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
            ViewHolder(parent.inflate(R.layout.item_device_list))

    override fun getItemCount(): Int = data.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) =
            holder.bindData(data[position], onClick)

    fun updateDataList(listFlow: Flowable<ArrayList<BluetoothDevice>>): Disposable {
        var newList: ArrayList<BluetoothDevice> = arrayListOf()
        return listFlow
                .doOnNext { newList = it }
                .map { DiffUtil.calculateDiff(DiffCallback(data, it)) }
                .doOnNext { data = newList }
                .subscribe({
                    it.dispatchUpdatesTo(this)
                }, Throwable::printStackTrace)
    }

    fun addDevice(device: BluetoothDevice) {
        val devices = data.map { it.address }
        if (!devices.contains(device.address)) {
            data.add(device)
            notifyDataSetChanged()
        }
    }

    fun clearData() {
        data.clear()
        notifyDataSetChanged()
    }

    fun isDataEmpty(): Boolean = data.isEmpty()

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        fun bindData(item: BluetoothDevice, onClick: (BluetoothDevice) -> Unit) {

            with(itemView) {
                setOnClickListener {
                    Log.i("+++", "onClick Device")
                    onClick.invoke(item)
                }

                tvHex.text = item.address
                tvName.text = item.name
            }
        }
    }

    class DiffCallback(private val oldList: List<BluetoothDevice>, private val newList: List<BluetoothDevice>) : DiffUtil.Callback() {

        enum class UpdatedPart {
            NAME
        }

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int) = oldList[oldItemPosition].address == newList[newItemPosition].address
        override fun getOldListSize(): Int = oldList.size
        override fun getNewListSize(): Int = newList.size
        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int) = getChangedParts(newList[newItemPosition], oldList[oldItemPosition]).isEmpty()
        override fun getChangePayload(oldItemPosition: Int, newItemPosition: Int) = getChangedParts(newList[newItemPosition], oldList[oldItemPosition])

        private fun getChangedParts(newItem: BluetoothDevice, oldItem: BluetoothDevice): Set<UpdatedPart> {
            val isNameTheSame = newItem.name == oldItem.name

            return mutableSetOf<UpdatedPart>().apply {
                when {
                    !isNameTheSame -> add(UpdatedPart.NAME)
                }
            }
        }
    }
}
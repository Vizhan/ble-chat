package com.lanars.pdpblechatapp.chat.dialog

import android.app.Dialog
import android.bluetooth.BluetoothDevice
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v7.app.AlertDialog
import android.support.v7.widget.DividerItemDecoration
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import com.lanars.pdpblechatapp.R
import kotlinx.android.synthetic.main.dialog_fragment_device_list.view.*


class DeviceListDialogFragment : DialogFragment() {

    private lateinit var onClick: (BluetoothDevice) -> Unit
    private lateinit var onCloseClick: (DeviceListDialogFragment) -> Unit

    private val deviceListAdapter: DeviceListRecyclerAdapter by lazy { DeviceListRecyclerAdapter(onClick) }

    private lateinit var progress: ProgressBar
    private lateinit var emptyState: TextView

    companion object {
        fun newInstance(
                onClick: (BluetoothDevice) -> Unit,
                onCloseClick: (DeviceListDialogFragment) -> Unit): DeviceListDialogFragment {
            return DeviceListDialogFragment().apply {
                this.onClick = onClick
                this.onCloseClick = onCloseClick
            }
        }
    }

    fun close() {
        dismiss()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = (LayoutInflater.from(activity).inflate(R.layout.dialog_fragment_device_list, null) as ViewGroup)

        this.progress = view.progress
        this.emptyState = view.emptyState

        with(view) {
            rvDevices.adapter = deviceListAdapter

            val divider = DividerItemDecoration(this@DeviceListDialogFragment.context, LinearLayout.VERTICAL)
            rvDevices.addItemDecoration(divider)

            close.setOnClickListener {
                onCloseClick.invoke(this@DeviceListDialogFragment)
                deviceListAdapter.clearData()
                progressBar(false)
            }
        }

        isCancelable = false

        return AlertDialog.Builder(context!!)
                .setView(view)
                .create()
    }

    fun updateAdapter(device: BluetoothDevice) {
        deviceListAdapter.addDevice(device)
    }

    fun progressBar(value: Boolean) {
        progress.visibility = if (value) View.VISIBLE else View.GONE
    }

    fun checkEmptyState() {
        val value = deviceListAdapter.isDataEmpty()
        emptyState.visibility = if (value) View.VISIBLE else View.GONE
    }
}
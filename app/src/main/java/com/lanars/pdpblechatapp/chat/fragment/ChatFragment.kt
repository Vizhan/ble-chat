package com.lanars.pdpblechatapp.chat.fragment

import android.app.Activity.RESULT_OK
import android.arch.lifecycle.Observer
import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.lanars.pdpblechatapp.R
import com.lanars.pdpblechatapp.chat.adapter.ChatRecyclerAdapter
import com.lanars.pdpblechatapp.chat.dialog.DeviceListDialogFragment
import com.lanars.pdpblechatapp.chat.vm.ChatViewModel
import com.lanars.pdpblechatapp.chat.vo.Message
import kotlinx.android.synthetic.main.fragment_chat.*
import org.koin.android.viewmodel.ext.android.viewModel


class ChatFragment : Fragment() {

    companion object {
        private const val REQUEST_ENABLE_BT = 500
    }

    private val viewModel: ChatViewModel by viewModel()
    private val chatAdapter: ChatRecyclerAdapter by lazy { ChatRecyclerAdapter() }

    private val bleAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private val BluetoothAdapter.isDisabled: Boolean
        get() = !isEnabled

    private val dialog = DeviceListDialogFragment.newInstance(
            onClick = { device -> viewModel.onDeviceSelected(device) },
            onCloseClick = { dialog -> dialog.dismiss() })

    private val bluetoothReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_OFF)
            when (state) {
                BluetoothAdapter.STATE_ON -> {

                }
                BluetoothAdapter.STATE_OFF -> {
                    activity?.finish()
                }
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_chat, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        if (bleAdapter?.isDisabled == true) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, ChatFragment.REQUEST_ENABLE_BT)
        }

        initToolbar()
        initRecyclerView()

        viewModel.discoveredDevicesLiveData.observe(viewLifecycleOwner, Observer {
            it ?: return@Observer

            dialog.updateAdapter(it)
        })

        viewModel.incomingMessageLiveData.observe(viewLifecycleOwner, Observer {
            it ?: return@Observer

            chatAdapter.addItem(Message(message = it, isOutComing = false))
            rvChat.scrollToPosition(0)
            Log.i("+++", "INCOMING = $it")
        })

        viewModel.outgoingMessageLiveData.observe(viewLifecycleOwner, Observer {
            it ?: return@Observer

            chatAdapter.addItem(Message(message = it, isOutComing = true))
            rvChat.scrollToPosition(0)
            Log.i("+++", "OUTGOING = $it")
        })

        viewModel.isServerConnectedLiveData.observe(viewLifecycleOwner, Observer {
            it ?: return@Observer

            if (it) {
                tbChat.title = "SERVER CONNECTED"
                Snackbar.make(root, "Device connected", Snackbar.LENGTH_LONG).show()
            } else {
                tbChat.title = "SERVER DISCONNECTED"
                Snackbar.make(root, "Device disconnected", Snackbar.LENGTH_LONG).show()
            }
        })

        viewModel.isClientConnectedLiveData.observe(viewLifecycleOwner, Observer {
            it ?: return@Observer

            if (it) {
                dialog.close()
                tbChat.title = "CLIENT CONNECTED"
                Snackbar.make(root, "Device connected", Snackbar.LENGTH_LONG).show()
            } else {
                tbChat.title = "CLIENT DISCONNECTED"
                Snackbar.make(root, "Device disconnected", Snackbar.LENGTH_LONG).show()
            }
        })

        viewModel.serverLiveData.observe(viewLifecycleOwner, Observer {
            it ?: return@Observer

            if (it.isEmpty()) {
                tbChat.title = "SERVER"
            } else {
                Snackbar.make(root, it, Snackbar.LENGTH_LONG).show()
            }
        })

        val filter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        context?.registerReceiver(bluetoothReceiver, filter)

        vSend.setOnClickListener {
            viewModel.onSendButtonClicked(etMessage.editableText.toString())
            etMessage.editableText.clear()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_OK) {
                Log.i("+++", "onActivityResult_REQUEST_RESULT_OK")

            } else {
                activity?.finish()
            }
        }

    }

    private fun initToolbar() {
        tbChat.inflateMenu(R.menu.menu_chat)
        tbChat.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.menuAdvertise -> {
                    if (bleAdapter?.isDisabled != false) {
                        val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                        startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
                    } else {
                        viewModel.advertise()
                    }
                }

                R.id.menuDiscover -> {
                    dialog.showNow(fragmentManager, "DeviceListDialogFragment")
                    viewModel.discover()
                }
            }
            true
        }
    }

    private fun initRecyclerView() {
        rvChat.adapter = chatAdapter
        rvChat.layoutManager = LinearLayoutManager(context).apply {
            orientation = LinearLayoutManager.VERTICAL
            reverseLayout = true
        }
    }

    override fun onDestroy() {
        context?.unregisterReceiver(bluetoothReceiver)
        super.onDestroy()
    }
}
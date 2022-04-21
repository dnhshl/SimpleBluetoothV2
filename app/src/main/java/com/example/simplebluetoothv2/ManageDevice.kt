package com.example.simplebluetoothv2

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.content.ContextCompat.getSystemService
import androidx.core.content.PermissionChecker.PERMISSION_GRANTED
import androidx.core.content.PermissionChecker.checkSelfPermission
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.simplebluetoothv2.databinding.FragmentManageDeviceBinding
import com.example.simplebluetoothv2.model.MainViewModel

class ManageDevice : Fragment() {

    private val TAG = "ManageDevice"

    private var _binding: FragmentManageDeviceBinding? = null
    private val binding get() = _binding!!


    private val viewModel: MainViewModel by activityViewModels()

    private val bluetoothAdapter: BluetoothAdapter by lazy { BluetoothAdapter.getDefaultAdapter() }
    private val discoveredDevices = arrayListOf<String>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentManageDeviceBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBondedDevices.setOnClickListener {
            getPairedDevices()
        }

        binding.btnSearchDevices.setOnClickListener {
            checkBTPermission()
            discoverDevices()
        }


        binding.listview.setOnItemClickListener { _, _, i, _ ->
            // i ist der Index des geklickten Eintrags
            viewModel.setSelectedDevice(binding.listview.getItemAtPosition(i).toString())
            findNavController().navigate(R.id.action_manageDevice_to_ESP32Control)
        }
    }

    @SuppressLint("MissingPermission")
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        if (bluetoothAdapter.isDiscovering) {
            bluetoothAdapter.cancelDiscovery()
            requireActivity().unregisterReceiver(broadcastReceiver);
        }

    }

    @SuppressLint("MissingPermission")
    private fun getPairedDevices() {

        val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter.bondedDevices
        val list = ArrayList<Any>()
        Log.i(TAG, "$pairedDevices")

        if (pairedDevices != null) {
            for (device in pairedDevices) list.add("${device.name}\n${device.address}")
        } else {
            Toast.makeText(context, getString(R.string.bt_no_paired_devices), Toast.LENGTH_LONG).show()
        }

        val adapter = ArrayAdapter(requireContext(),
            android.R.layout.simple_list_item_1,   // Layout zur Darstellung der ListItems
            list)                                  // Liste, die dargestellt werden soll

        binding.listview.adapter = adapter
    }

    private fun checkBTPermission() {
        var permissionCheck = checkSelfPermission(requireContext(),"Manifest.permission.ACCESS_FINE_LOCATION")
        permissionCheck += checkSelfPermission(requireContext(),"Manifest.permission.ACCESS_COARSE_LOCATION")
        permissionCheck += checkSelfPermission(requireContext(),"Manifest.permission.BLUETOOTH_CONNECT")
        if (permissionCheck != PERMISSION_GRANTED) {
            requestPermissions(arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.BLUETOOTH_CONNECT), 1001)
        }
    }

    @SuppressLint("MissingPermission")
    private fun discoverDevices() {
        when (bluetoothAdapter.isDiscovering) {
            false -> {
                // Suche einschalten
                bluetoothAdapter.startDiscovery()
                // Button Text anpassen
                binding.btnSearchDevices.text = getString(R.string.stop_search_devices)
                // Intent Filter für BroadcastReceiver
                val discoverDevicesIntent = IntentFilter(BluetoothDevice.ACTION_FOUND) //auf diese Signale soll unser Broadcast Receiver filtern
                // BroadcastReceiver starten
                requireActivity().registerReceiver(broadcastReceiver, discoverDevicesIntent)
            }
            true -> {
                // Suche ausschalten
                bluetoothAdapter.cancelDiscovery()
                // Button Text anpassen
                binding.btnSearchDevices.text = getString(R.string.start_search_devices)
                // Broadcast Receiver ausschalten
                requireActivity().unregisterReceiver(broadcastReceiver);
            }
        }
    }

    private val broadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(context: Context?, intent: Intent) {
            val action = intent.action
            if (action == BluetoothDevice.ACTION_FOUND) {
                val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                val deviceInfo = "${device!!.name}\n${device.address}"
                Log.i(TAG, deviceInfo)

                // gefundenes Gerät der Liste hinzufügen, wenn es noch nicht aufgeführt ist
                if (deviceInfo !in discoveredDevices) discoveredDevices.add(deviceInfo)

                // aktualisierte Liste im Listview anzeigen
                val adapt = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, discoveredDevices)
                binding.listview.adapter = adapt
            }
        }
    }

}
package com.example.simplebluetoothv2

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.airbnb.paris.extensions.style
import com.example.simplebluetoothv2.databinding.FragmentEsp32controlBinding
import com.example.simplebluetoothv2.model.MainViewModel
import kotlinx.coroutines.Job
import java.nio.channels.spi.AbstractSelectableChannel


class ESP32Control : Fragment() {

    private val TAG = "ESP32Control"

    private var _binding: FragmentEsp32controlBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MainViewModel by activityViewModels()


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentEsp32controlBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        readSharedPreferences()

        viewModel.selectedDevice.observe(viewLifecycleOwner) { device ->
            when (device) {
                "" -> binding.tvSelectedDevice.text = getString(R.string.no_selected_device)
                else -> binding.tvSelectedDevice.text = getString(R.string.selected_device).format(device)
            }
            writeSharedPreferences(device)
        }

        viewModel.isConnected.observe(viewLifecycleOwner) { isConnected ->
            when (isConnected) {
                MainViewModel.ConnectionState.CONNECTED ->
                    binding.tvIsConnected.text = getString(R.string.connected)
                MainViewModel.ConnectionState.CONNECTING ->
                    binding.tvIsConnected.text = getString(R.string.connecting)
                MainViewModel.ConnectionState.NOT_CONNECTED ->
                    binding.tvIsConnected.text = getString(R.string.not_connected)
            }
        }

        viewModel.esp32Data.observe(viewLifecycleOwner) { data ->
            binding.tvData.text = "${data.ledstatus}\n${data.potiArray}"
        }


        binding.btnSelectDevice.setOnClickListener {
            findNavController().navigate(R.id.action_ESP32Control_to_manageDevice)
        }

        binding.btnConnect.setOnClickListener {
            viewModel.connectEsp32()
            viewModel.checkConnect(1000L)
        }

        binding.btnDisconnect.setOnClickListener {
            viewModel.disconnectEsp32()
        }

        binding.switchLed.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) viewModel.ledData.led = "H"
            else viewModel.ledData.led = "L"
            viewModel.sendLedData()
        }

        binding.switchBlinken.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) viewModel.ledData.ledBlinken = true
            else viewModel.ledData.ledBlinken = false
            viewModel.sendLedData()
        }

        binding.switchDaten.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                viewModel.startDataLoadJob()
                binding.tvData.visibility = View.VISIBLE
            } else {
                viewModel.cancelDataLoadJob()
                binding.tvData.visibility = View.INVISIBLE
            }
        }


    }

    private fun writeSharedPreferences(selectedDevice: String) {
        Log.i(TAG, "writeSharedPreferences")
        // speichere das selected device
        activity?.apply {
            val sp = getPreferences(Context.MODE_PRIVATE)
            val edit = sp.edit()
            edit.putString("device", selectedDevice)
            edit.commit()
        }
    }

    private fun readSharedPreferences() {
        Log.i(TAG, "writeSharedPreferences")
        // speichere das selected device
        activity?.apply {
            val sp = getPreferences(Context.MODE_PRIVATE)
            val device = sp.getString("device", "").toString()
            if (!device.isEmpty()) viewModel.setSelectedDevice(device)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
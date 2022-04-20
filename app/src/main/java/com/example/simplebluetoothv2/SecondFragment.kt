package com.example.simplebluetoothv2

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.simplebluetoothv2.databinding.FragmentSecondBinding
import com.example.simplebluetoothv2.model.MainViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin

/**
 * A simple [Fragment] subclass as the second destination in the navigation.
 */
class SecondFragment : Fragment() {

    private val TAG = "SecondFragment"

    private var _binding: FragmentSecondBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MainViewModel by activityViewModels()

    private lateinit var connectJob: Job

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentSecondBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        viewModel.checkConnect(5000L)

        viewModel.isConnected.observe(viewLifecycleOwner) {
                isConnected ->
                    when (isConnected) {
                        true -> binding.tvConnected.text = "connected"
                        false -> binding.tvConnected.text = "... connecting"
                    }
        }

        binding.btnLed.setOnClickListener {
            viewModel.getEsp32Data()
            when (viewModel.ledData.led) {
                "H" -> {
                    viewModel.ledData.led = "L"
                    viewModel.sendLedData()
                    binding.btnLed.text = getString(R.string.led_on)
                    Log.i(TAG, "Switch LED OFF")
                }
                "L" -> {
                    viewModel.ledData.led = "H"
                    viewModel.sendLedData()
                    binding.btnLed.text = getString(R.string.led_off)
                    Log.i(TAG, "Switch LED ON")
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
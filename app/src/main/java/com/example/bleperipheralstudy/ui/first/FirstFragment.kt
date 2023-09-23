package com.example.bleperipheralstudy.ui.first

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.bleperipheralstudy.R
import com.example.bleperipheralstudy.databinding.FragmentFirstBinding
import dagger.hilt.android.AndroidEntryPoint


/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
@AndroidEntryPoint
class FirstFragment : Fragment() {

    private var _binding: FragmentFirstBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFirstBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonFirst.setOnClickListener {
            checkBLEPermissions()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    /**
     * Check the BLE permissions. If all are granted, proceed to the next screen.
     * If any of the permissions are not granted, a dialog is displayed.
     */
    private fun checkBLEPermissions() {
        if (allPermissionsGranted()) {
            findNavController().navigate(R.id.action_FirstFragment_to_SecondFragment)
        } else {
            requestPermissionLauncher.launch(blePermissions)
        }
    }

    /**
     * Check that all BLE permissions are granted.
     */
    private fun allPermissionsGranted(): Boolean {
        return blePermissions.all {
            requireContext().checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Request permissions.
     */
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->

        if (permissions.all { it.value }) {
            findNavController().navigate(R.id.action_FirstFragment_to_SecondFragment)
        } else {
            Toast.makeText(
                requireContext(), "パーミッションが許可されていません", Toast.LENGTH_SHORT
            ).show()

            // Open application settings
//            val uriString = "package:" + requireContext().packageName
//            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse(uriString))
//            startActivity(intent)
        }
    }

    companion object {

        // パーミッションのリストは要確認。
        val blePermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_ADVERTISE,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
            )
        } else {
            arrayOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
            )
        }
    }
}
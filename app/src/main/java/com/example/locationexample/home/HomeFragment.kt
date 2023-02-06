package com.example.locationexample.home

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.locationexample.databinding.FragmentHomeBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.google.android.gms.location.LocationRequest
import kotlinx.coroutines.withContext
import java.util.*

private const val UPDATE_INTERVAL_IN_MILLISECONDS: Long = 5000
private const val FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS = UPDATE_INTERVAL_IN_MILLISECONDS/2
class HomeFragment : Fragment() {
    private lateinit var binding: FragmentHomeBinding
    private lateinit var permissionLauncher: ActivityResultLauncher<Array<String>>
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var mLocationRequest: LocationRequest

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        permissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
                if(permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true && permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true){
                    Toast.makeText(requireContext(),"Permissions granted",Toast.LENGTH_SHORT).show()
                    getCurrentLocation()
                }else{
                    handelDenyCase()
                }
            }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
                locationResult ?: return
                for (location in locationResult.locations){
                    getCurrentAddress(location)
                    break
                }
                fusedLocationClient.removeLocationUpdates(this)
            }
        }
        mLocationRequest = LocationRequest()
        mLocationRequest.interval = UPDATE_INTERVAL_IN_MILLISECONDS
        mLocationRequest.fastestInterval = FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS
        mLocationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
    }

    override fun onResume() {
        super.onResume()
        binding.btnOpenSettings.isVisible = !checkLocationPermissions()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentHomeBinding.inflate(inflater,container,false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnGetLocation.setOnClickListener {
            if(!checkLocationPermissions()) {
                requestLocationPermission()
            }else{
                getCurrentLocation()
            }

        }
        binding.btnOpenSettings.setOnClickListener {
            openAppSettings()
        }
    }


    private fun checkLocationPermissions():Boolean{
        if(ActivityCompat.checkSelfPermission(requireContext(),Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_DENIED
            && ActivityCompat.checkSelfPermission(requireContext(),Manifest.permission.ACCESS_COARSE_LOCATION)==PackageManager.PERMISSION_DENIED
        ){
            return false
        }
        return true
    }

    private fun requestLocationPermission(){
        permissionLauncher.launch(arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ))
    }
    private fun handelDenyCase(){
        val rationaleFlagFine = ActivityCompat.shouldShowRequestPermissionRationale(requireActivity(),Manifest.permission.ACCESS_FINE_LOCATION)
        val rationaleFlagCoarse = ActivityCompat.shouldShowRequestPermissionRationale(requireActivity(),Manifest.permission.ACCESS_COARSE_LOCATION)
        if(!rationaleFlagCoarse && !rationaleFlagFine){
            Toast.makeText(requireContext(),"You should accept permissions to be able to access this feature",Toast.LENGTH_LONG).show()
        }
    }

    private fun openAppSettings(){
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        val uri = Uri.fromParts("package", "com.example.locationexample", null)
        intent.data = uri
        startActivity(intent)
    }


    private fun getCurrentLocation(){
        if (ActivityCompat.checkSelfPermission(
                requireActivity(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                requireActivity(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        fusedLocationClient.requestLocationUpdates(mLocationRequest,locationCallback, Looper.getMainLooper())
    }

    private fun getCurrentAddress(location:Location) {
        lifecycleScope.launch(Dispatchers.IO) {
            kotlin.runCatching {
                val local=Locale("ar")
                val geoCoder=Geocoder(requireContext(), local)
                val address= geoCoder.getFromLocation(
                    location.latitude,
                    location.longitude,
                    1
                ).firstOrNull()
                val mAddress= address?.let {
                    buildString {
                        append(address.locality)
                        append(", ")

                        append(address.adminArea)
                        append(", ")

                        append(address.countryName)
                    }
                }?:""
                withContext(Dispatchers.Main){ binding.etLocation.setText(mAddress)}
            }.onFailure {
                it.printStackTrace()
            }
        }
    }

}
package com.example.locationexample.home

import android.Manifest
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatEditText
import androidx.core.location.LocationManagerCompat.getCurrentLocation
import androidx.lifecycle.lifecycleScope
import com.example.locationexample.R
import com.example.locationexample.databinding.FragmentHomeBinding
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import io.nlopez.smartlocation.SmartLocation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*

class HomeFragment : Fragment() {
    private lateinit var binding: FragmentHomeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
        getCurrentLocation()
    }
    private fun getCurrentLocation(){
        Dexter.withContext(requireContext()).withPermissions(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ).withListener(object: MultiplePermissionsListener{
            override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                if(report!!.areAllPermissionsGranted()){
                    SmartLocation.with(requireContext()).location()
                        .start { location ->
                            location?.let {
                                getCurrentAddress(it)
                                SmartLocation.with(requireContext()).location().stop()
                            }
                        }
                }
            }

            override fun onPermissionRationaleShouldBeShown(
                p0: MutableList<PermissionRequest>?,
                p1: PermissionToken?
            ) {
                p1!!.continuePermissionRequest()
            }
        }).check()
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
                Log.i("mostafa", "getCurrentAddress: \n $address")
                val mAddress= address?.let {
                    buildString {
                        append(address.locality)
                        append(", ")

                        append(address.adminArea)
                        append(", ")

                        append(address.countryName)
                    }
                }?:""
                binding.etLocation.setText(mAddress)
            }.onFailure {
                it.printStackTrace()
            }
        }
    }

}
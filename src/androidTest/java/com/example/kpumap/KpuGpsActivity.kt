package com.example.kpumap

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import org.jetbrains.anko.alert
import org.jetbrains.anko.noButton
import org.jetbrains.anko.toast
import org.jetbrains.anko.yesButton
import java.lang.IndexOutOfBoundsException

var count = 0       //카메라 이동제어

class KpuGpsActivity : AppCompatActivity(), OnMapReadyCallback,GoogleMap.OnMarkerClickListener {

    companion object{
        var locationArr = arrayListOf<store>()
    }

    private fun LoadDB() {
        val helper = DbAdapter(this)
        helper.createDatabase()       //db생성
        helper.open()         //db복사

        locationArr = helper.GetAllData()
        helper.close()  //닫기
    }

    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: MyLocationCallBack
    private val REQUEST_ACCESS_FINE_LOCATION = 1000


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_kpu_gps)

        try {
            LoadDB()
        } catch (e: IndexOutOfBoundsException) {
            throw IndexOutOfBoundsException()
        }
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        locationInit()

    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */

    override fun onMapReady(googleMap: GoogleMap) {

        val bundle = intent.getBundleExtra("bun")
        val imgArr = bundle.getSerializable("imageArr") as ArrayList<Int>
        val stName= bundle.getSerializable("stName") as String
        var num = bundle.getSerializable("num") as Int
        val kpu = LatLng(37.340362, 126.733520)
        count = 0

        mMap = googleMap
        mMap.moveCamera(CameraUpdateFactory.newLatLng(kpu))     //지도 입장 위치

        //메인에서 띄운 지도와 상세페이지에서 띄운 지도 구별
        if(num==2){
            for(i in 0 until locationArr.size-1)
                mMap.addMarker(MarkerOptions().position(LatLng(locationArr[i].lat.toDouble(),locationArr[i].lng.toDouble())).title("${locationArr[i].storeName}"))
            mMap.setOnMarkerClickListener(this)
        }
        else {
            for(i in 0 until locationArr.size - 1){
                if(stName==locationArr[i].storeName){
                    count = 1
                    mMap.addMarker(MarkerOptions().position(LatLng(locationArr[i].lat.toDouble(),locationArr[i].lng.toDouble())).title("${locationArr[i].storeName}"))
                    mMap .animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(locationArr[i].lat.toDouble(),locationArr[i].lng.toDouble()), 17f))
                    }
            }
            mMap.setOnMarkerClickListener(this)
        }

    }
    //마커 클릭시 이벤트
    override fun onMarkerClick(marker: Marker):Boolean{

        try {
            LoadDB()
        } catch (e: IndexOutOfBoundsException) {
            throw IndexOutOfBoundsException()
        }

        val dlgView = layoutInflater.inflate(R.layout.popup, null)      //팝업창 생성



        val imgArr = bundle.getSerializable("imageArr") as ArrayList<Int>
        val stName= bundle.getSerializable("stName") as String
        val num = bundle.getSerializable("num") as Int

        val popImg : ImageView = dlgView.findViewById(R.id.popup_list_picture)
        val popName : TextView = dlgView.findViewById(R.id.popup_list_name)
        val popTitle : TextView = dlgView.findViewById(R.id.popup_list_title)
        val popAddress : TextView = dlgView.findViewById(R.id.popup_list_address)
        val popCall : TextView = dlgView.findViewById(R.id.popup_list_call)
        val callButton : Button = dlgView.findViewById(R.id.popup_call)
        val detailButton : Button = dlgView.findViewById(R.id.popup_detail)

        val bundle2 = Bundle() // 상세페이지로 보낼 데이터

        val dlgBuilder = AlertDialog.Builder(this)

        for(i in 0 until locationArr.size - 1) {
            if (marker.title == locationArr[i].storeName){
                popImg.setImageResource(imgArr[i])
                popName.text = locationArr[i].storeName
                popTitle.text = locationArr[i].storeIntro
                popAddress.text = locationArr[i].storePlace
                popCall.text = locationArr[i].storeCall

                bundle2.putSerializable("imageArr",storeImageArray)
                bundle2.putSerializable("storeArr", locationArr)
                bundle2.putSerializable("img", storeImageArray[i])
                bundle2.putSerializable("name",locationArr[i].storeName)
                bundle2.putSerializable("phoneNum", locationArr[i].storeCall)
            }
        }

        //전화 걸기
        callButton.setOnClickListener {
            val uri = Uri.parse("tel:${popCall.text}")
            val intent = Intent(Intent.ACTION_DIAL, uri)
            startActivity(intent)
        }

        //상세페이지 이동
        detailButton.setOnClickListener {
            val intent = Intent(this,StoreActivity::class.java)
            intent.putExtra("bundle",bundle2)
            startActivity(intent)
        }

        dlgBuilder.setView(dlgView)
        dlgBuilder.show()

        return true
    }

    inner class MyLocationCallBack : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult?) {
            super.onLocationResult(locationResult)
            val location = locationResult?.lastLocation
            location?.run {
                val latLng = LatLng(latitude, longitude)
                // 지도 시작시 내위치로 이동
                if(count == 0){
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 17f))//줌 레벨 : 153m정도
                }
                count++
                mMap.isMyLocationEnabled = true  //내 현재위치를 파란색 점으로 표시함
                Log.d("KpuGpsActivity", "위도: $latitude, 경도: $longitude")
            }
        }
    }
    //초기화
    private fun locationInit() {
        fusedLocationProviderClient = FusedLocationProviderClient(this)
        locationCallback = MyLocationCallBack()
        locationRequest = LocationRequest()
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        locationRequest.interval = 10000
        locationRequest.fastestInterval = 5000
    }

    @SuppressLint("MissingPermission")
    private fun addLocationListener() {
        fusedLocationProviderClient.requestLocationUpdates(locationRequest,locationCallback,null);
    }

    override fun onResume() {
        super.onResume()
        // 권한 요청
        permissionCheck(cancel = {
// 위치 정보가 필요한 이유 다이얼로그 표시
            showPermissionInfoDialog()
        }, ok = {
// 현재 위치를 주기적으로 요청 (권한이 필요한 부분)
            addLocationListener()
        })

    }

    private fun showPermissionInfoDialog() {
        alert("현재 위치 정보를 얻기 위해서는 위치 권한이 필요합니다", "권한이 필요한 이유") {//다이얼로그 표시
            yesButton {
// 권한 요청
// yes블록 내부에서는 this가 DialogInterface이므로 현재 액티비티를 명시적으로 표시
                ActivityCompat.requestPermissions(this@KpuGpsActivity,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    REQUEST_ACCESS_FINE_LOCATION)
            }
            noButton { }
        }.show()
    }

    private fun permissionCheck(cancel: () -> Unit, ok: () -> Unit) {
// 위치 권한이 있는지 검사
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) !=
            PackageManager.PERMISSION_GRANTED) {
// 권한이 허용되지 않음
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {
// 이전에 권한을 한 번 거부한 적이 있는 경우에 실행할 함수
                cancel()
            } else {
// 권한 요청
                ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    REQUEST_ACCESS_FINE_LOCATION)
            }
        } else {
// 권한을 수락 했을 때 실행할 함수
            ok()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>,
                                            grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_ACCESS_FINE_LOCATION -> {
                if ((grantResults.isNotEmpty()
                            && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
// 권한 허용됨
                    addLocationListener()
                } else {
// 권한 거부
                    toast("권한 거부 됨")
                }
                return
            }
        }
    }

    override fun onPause() {
        super.onPause()
        removeLocationListener()
    }
    private fun removeLocationListener() {
// 현재 위치 요청을 삭제
        fusedLocationProviderClient.removeLocationUpdates(locationCallback)
    }

}

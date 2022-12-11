package com.company.sns_crawlling

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import org.json.JSONException
import org.json.JSONObject
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.*

class MainActivity : AppCompatActivity() {

    private var nx = "0" //위도
    private var ny = "0" //경도
    private lateinit var baseDate : String //조회하고싶은 날짜
    private lateinit var baseTime : String //조회하고싶은 시간
    private val type = "json" //조회하고 싶은 type(json, xml 중 고름)
    private var weather = "현재 날씨는 "

    private var longitude = 0.0
    private var latitude = 0.0

   @RequiresApi(Build.VERSION_CODES.O)
   override fun onCreate(savedInstanceState: Bundle?) {
       super.onCreate(savedInstanceState)
       setContentView(R.layout.activity_main)

       show()

       val revertBtn = findViewById<ImageButton>(R.id.revertBtn)
       revertBtn.setOnClickListener {
           show()
       }

       val mapBtn = findViewById<Button>(R.id.mapBtn)
       mapBtn.setOnClickListener {
           val intent = Intent(this@MainActivity, MapActivity::class.java)
           intent.putExtra("Longitude", longitude)
           intent.putExtra("Latitude", latitude)
           startActivity(intent)
       }

       val settingBtn = findViewById<Button>(R.id.settingBtn)
       settingBtn.setOnClickListener {
           val intent = Intent(this@MainActivity, SettingActivity::class.java)
           startActivity(intent)
       }

   }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.close -> finish()
        }
        return super.onOptionsItemSelected(item)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun show() {
        // 현재 날짜
        var nowDate = LocalDate.now()
        baseDate = nowDate.format(DateTimeFormatter.ofPattern("yyyyMMdd"))

        // 현재 시간
        val time: LocalTime = LocalTime.now()
        var curTime = time.format(DateTimeFormatter.ofPattern("HHmm"))
        baseTime = getBaseTime(curTime)

        getLocation()
        convertGRID_GPS(latitude, longitude)

        Thread {
            lookUpWeather() // network 동작, 인터넷에서 xml을 받아오는 코드
        }.start()

        val timeText : TextView = findViewById(R.id.timeText)
        curTime = time.format(DateTimeFormatter.ofPattern("HH:mm:ss"))
        timeText.text = curTime

        // 현재 사용자 위치 표시
        val pos : String = "(" + longitude + "," + latitude + ")"
        val posText : TextView = findViewById(R.id.posText)
        posText.text = pos
    }

    fun getBaseTime(time : String) : String {
        var t = time.toInt()
        val apiTime = intArrayOf(210, 510, 810, 1110, 1410, 1710, 2110)

        for (i in 0 until apiTime.size) {
            if (t <= apiTime[i]) {
                if (t < 210) {
                    baseDate = yesterDate() // 어제 날짜
                    t = 2300
                    break
                }
                //apiTime - 10 = baseTime
                t = apiTime[i - 1] - 10
                break
            }
        }

        if (t == 0) return "0000"
        else if (t < 1000) return "0" + t
        return t.toString()
    }

    fun yesterDate() : String {
        val sdf = SimpleDateFormat("yyyyMMdd")
        val cal : Calendar = Calendar.getInstance()
        cal.add(Calendar.DATE, -1)
        val yesterday = sdf.format(cal.time)
        return yesterday
    }

    private fun convertGRID_GPS(lat_x : Double, lng_y : Double) {
        val RE = 6371.00877 // 지구 반경(km)
        val GRID = 5.0 // 격자 간격(km)
        val SLAT1 = 30.0 // 투영 위도1(degree)
        val SLAT2 = 60.0 // 투영 위도2(degree)
        val OLON = 126.0 // 기준점 경도(degree)
        val OLAT = 38.0 // 기준점 위도(degree)
        val XO = 43.0 // 기준점 X좌표(GRID)
        val YO = 136.0 // 기1준점 Y좌표(GRID)

        val DEGRAD = Math.PI / 180.0
        val RADDEG = 180.0 / Math.PI

        val re = RE / GRID
        val slat1 = SLAT1 * DEGRAD
        val slat2 = SLAT2 * DEGRAD
        val olon = OLON * DEGRAD
        val olat = OLAT * DEGRAD

        var sn = Math.tan(Math.PI * 0.25 + slat2 * 0.5) / Math.tan(Math.PI * 0.25 + slat1 * 0.5)
        sn = Math.log(Math.cos(slat1) / Math.cos(slat2)) / Math.log(sn)
        var sf = Math.tan(Math.PI * 0.25 + slat1 * 0.5)
        sf = Math.pow(sf, sn) * Math.cos(slat1) / sn
        var ro = Math.tan(Math.PI * 0.25 + olat * 0.5)
        ro = re * sf / Math.pow(ro, sn)

        var ra = Math.tan(Math.PI * 0.25 + (lat_x) * DEGRAD * 0.5)
        ra = re * sf / Math.pow(ra, sn)
        var theta = lng_y * DEGRAD - olon
        if (theta > Math.PI) theta -= 2.0 * Math.PI
        if (theta < -Math.PI) theta += 2.0 * Math.PI
        theta *= sn
        val x = Math.floor(ra * Math.sin(theta) + XO + 0.5).toInt()
        val y = Math.floor(ro - ra * Math.cos(theta) + YO + 0.5).toInt()

        nx = x.toString()
        ny = y.toString()
    }

    @Throws(IOException::class, JSONException::class)
    fun lookUpWeather() {

//		참고문서에 있는 url주소
        val apiUrl = "http://apis.data.go.kr/1360000/VilageFcstInfoService_2.0/getVilageFcst"
        //         홈페이지에서 받은 키
        val serviceKey = "C%2BP4bXTNTzO2ZPZh96nrcAERLuDsNQ46UJyhwF81T%2BG2E2yxBTlYFsByXHmhZEgyaVzTu5YvlcaMn0%2BHwFGgRQ%3D%3D"

        val urlBuilder = StringBuilder(apiUrl)
        urlBuilder.append(
            "?" + URLEncoder.encode("ServiceKey", "UTF-8").toString() + "=" + serviceKey
        )
        urlBuilder.append(
            "&" + URLEncoder.encode("nx", "UTF-8").toString() + "=" + URLEncoder.encode(nx, "UTF-8")
        ) //경도
        urlBuilder.append(
            "&" + URLEncoder.encode("ny", "UTF-8").toString() + "=" + URLEncoder.encode(ny, "UTF-8")
        ) //위도
        urlBuilder.append(
            "&" + URLEncoder.encode("base_date", "UTF-8").toString() + "=" + URLEncoder.encode(
                baseDate, "UTF-8"
            )
        ) /* 조회하고싶은 날짜*/
        urlBuilder.append(
            "&" + URLEncoder.encode("base_time", "UTF-8").toString() + "=" + URLEncoder.encode(
                baseTime, "UTF-8"
            )
        ) /* 조회하고싶은 시간 AM 02시부터 3시간 단위 */
        urlBuilder.append(
            "&" + URLEncoder.encode("dataType", "UTF-8").toString() + "=" + URLEncoder.encode(
                type, "UTF-8"
            )
        ) /* 타입 */

        /*
         * GET방식으로 전송해서 파라미터 받아오기
         */
        val url = URL(urlBuilder.toString())

        val conn: HttpURLConnection = url.openConnection() as HttpURLConnection
        conn.setRequestMethod("GET")
        conn.setRequestProperty("Content-type", "application/json")
        //Log.d("Response code", conn.responseCode.toString())

        val rd: BufferedReader
        if (conn.getResponseCode() >= 200 && conn.getResponseCode() <= 300) {
            rd = BufferedReader(InputStreamReader(conn.getInputStream()))
        } else {
            rd = BufferedReader(InputStreamReader(conn.getErrorStream()))
        }

        val sb = StringBuilder()
        var line: String?
        while (rd.readLine().also { line = it } != null) {
            sb.append(line)
        }

        rd.close()
        conn.disconnect()
        val result = sb.toString()

        //=======이 밑에 부터는 json에서 데이터 파싱해 오는 부분이다=====//

        // response 키를 가지고 데이터를 파싱
        val jsonObj_1 = JSONObject(result)
        val response = jsonObj_1.getString("response")

        // response 로 부터 body 찾기
        val jsonObj_2 = JSONObject(response)
        val body = jsonObj_2.getString("body")

        // body 로 부터 items 찾기
        val jsonObj_3 = JSONObject(body)
        val items = jsonObj_3.getString("items")
        Log.i("ITEMS", items)

        // items로 부터 itemlist 를 받기
        var jsonObj_4 = JSONObject(items)
        val jsonArray = jsonObj_4.getJSONArray("item")
        for (i in 0 until jsonArray.length()) {
            jsonObj_4 = jsonArray.getJSONObject(i)
            val fcstValue = jsonObj_4.getString("fcstValue")
            val category = jsonObj_4.getString("category")

            if (category == "SKY") {
                val image : ImageView = findViewById(R.id.imageView)

                runOnUiThread {
                    when (fcstValue) {
                        "1" -> image.setImageResource(R.drawable.sun)
                        "2" -> image.setImageResource(R.drawable.rainy)
                        "3" -> image.setImageResource(R.drawable.many_cloud)
                        "4" -> image.setImageResource(R.drawable.cloudy)
                    }
                }
            }

//            if (category == "T3H" || category == "T1H") {
//                tmperature = "기온은 $fcstValue℃ 입니다."
//            }

            Log.i("WEATER_TAG", weather)
        }
    }

    private fun getLocation() {
        val lm = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        val isGPSEnabled : Boolean = lm.isProviderEnabled(LocationManager.GPS_PROVIDER)
        val isNetworkEnabled : Boolean = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

        if (Build.VERSION.SDK_INT >= 23 &&
                ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this@MainActivity,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 0)
        } else {
            when { // provider 제공자 활성화 여부 체크
                isNetworkEnabled -> {
                    val location = // 인터넷기반 위치 찾기
                        lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                    longitude = location?.longitude!!
                    latitude = location?.latitude!!
                }
                isGPSEnabled -> {
                    val location = // GPS 기반 위치 찾기
                        lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                    longitude = location?.longitude!!
                    latitude = location?.latitude!!
                }
//                else -> {
//
//                }
            }
            // 몇초 간격과 몇미터를 이동했을시에 호출되는 부분 - 주기적으로 위치 업데이트를 하고 싶다면 사용
            // ****주기적 업데이트를 사용하다가 안할 시에는 반드시 해제 필요****
            /*lm.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                1000, //몇초
                1F, //몇미터
                gpsLocationListener)
            lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,
                1000,
                1F,
                gpsLocationListener)
            // 해제부분
            lm.removeUpdates(gpsLocationListener)*/
        }
        lm.removeUpdates(gpsLocationListener)
    }

    // 위에 *몇초 간격과 몇미터를 이동했을시에 호출되는 부분* 에 필요한 정보
    // 주기적으로 위치 업데이트 안할거면 사용하지 않음
    val gpsLocationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            val provider : String = location.provider
            val longitude : Double = location.longitude
            val latitude : Double = location.latitude
            val altitude : Double = location.altitude
        }

        // 아래 2개함수는 형식상 필수부분
        override fun onProviderEnabled(provider: String) {
        }

        override fun onProviderDisabled(provider: String) {
        }
    }
}
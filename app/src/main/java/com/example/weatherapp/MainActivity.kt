package com.example.weatherapp

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.location.Location
import android.location.LocationManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import org.json.JSONObject
import java.lang.Exception
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*
import android.os.AsyncTask
import android.os.Build
import android.provider.Settings
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.res.ResourcesCompat
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.bumptech.glide.Glide
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import org.w3c.dom.Text
import java.time.LocalDateTime

/**
 * Sök position?
 * effekter efter väder?
 * Cards hourly eller dagar
 * Data class som är deserializable för JSON
 * Gör en lista och loopa igenom för att sätta färgerna
 */


class MainActivity : AppCompatActivity() {
    // API key for Weathermap
    val API: String = "redacted"

    // ID for using position data from user
    val PERMISSION_ID = 42
    lateinit var mFusedLocationClient: FusedLocationProviderClient
    private lateinit var swipeRefresh: SwipeRefreshLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        swipeRefresh = findViewById(R.id.swipeRefresh)

        swipeRefresh.setOnRefreshListener {
            refresh()
        }
        getLastLocation()
    }

    private fun refresh() {
        getLastLocation()
        if(swipeRefresh.isRefreshing){
            swipeRefresh.isRefreshing = false
        }
    }

    inner class weatherTask(lat: String, lon: String) : AsyncTask<String, Void, String>() {
        var LAT: String = lat
        var LON: String = lon
        override fun onPreExecute() {
            super.onPreExecute()
            /* Showing the ProgressBar, Making the main design GONE */
            findViewById<ProgressBar>(R.id.loader).visibility = View.VISIBLE
            findViewById<RelativeLayout>(R.id.mainContainer).visibility = View.GONE
            findViewById<TextView>(R.id.errorText).visibility = View.GONE
        }

        override fun doInBackground(vararg params: String?): String? {
            var response:String? = try{
                URL("https://api.openweathermap.org/data/2.5/weather?" +
                        "lat=$LAT&lon=$LON&units=metric&appid=$API").readText(Charsets.UTF_8)
            }catch (e: Exception){
                null
            }
            return response
        }

        override fun onPostExecute(result: String) {
            super.onPostExecute(result)
            try {
                /* Extracting JSON returns from the API */
                val jsonObj = JSONObject(result)
                val main = jsonObj.getJSONObject("main")
                val sys = jsonObj.getJSONObject("sys")
                val wind = jsonObj.getJSONObject("wind")
                val weather = jsonObj.getJSONArray("weather").getJSONObject(0)
                val updatedAt:Long = jsonObj.getLong("dt")
                val updatedAtText = "Updated "+ SimpleDateFormat("HH:mm",
                    Locale.ITALY).format(Date(updatedAt*1000))
                val temp = main.getString("temp")+"°C"
                val feelsLike = "Feels like: " + main.getString("feels_like")+"°C"
                val sunrise:Long = sys.getLong("sunrise")
                val sunset:Long = sys.getLong("sunset")
                val windSpeed = wind.getString("speed") + " m/s"
                val weatherDescription = weather.getString("description")
                val address = jsonObj.getString("name")
                val iconId = weather.getString("icon")

                /* Populating extracted data into our views */
                findViewById<TextView>(R.id.address).text = address
                findViewById<TextView>(R.id.updated_at).text =  updatedAtText
                findViewById<TextView>(R.id.status).text = weatherDescription.replaceFirstChar {
                    if (it.isLowerCase()) it.titlecase(
                        Locale.getDefault()
                    ) else it.toString()
                }
                findViewById<TextView>(R.id.temp).text = temp
                findViewById<TextView>(R.id.feels).text = feelsLike
                findViewById<TextView>(R.id.sunriseTime).text = SimpleDateFormat("HH:mm",
                    Locale.ENGLISH).format(Date(sunrise*1000))
                findViewById<TextView>(R.id.sunsetTime).text = SimpleDateFormat("HH:mm",
                    Locale.ENGLISH).format(Date(sunset*1000))
                findViewById<TextView>(R.id.windValue).text = windSpeed

                if (updatedAt in (sunrise + 1) until sunset){
                    setBackground(true)
                    getIcon(iconId, "a")
                } else {
                    setBackground(false)
                    getIcon(iconId, "w")
                }

                /* Views populated, Hiding the loader, Showing the main design */
                findViewById<ProgressBar>(R.id.loader).visibility = View.GONE
                findViewById<RelativeLayout>(R.id.mainContainer).visibility = View.VISIBLE
            } catch (e: Exception) {
                findViewById<ProgressBar>(R.id.loader).visibility = View.GONE
                findViewById<TextView>(R.id.errorText).visibility = View.VISIBLE
            }
        }
    }

    /**
     * Checks the time of update to determine if the daylight gradient or night gradient should
     * be used.
     */
    private fun setBackground(day: Boolean) {
        val mainLayout: View = findViewById(R.id.mainWindow)
        val viewList = listOf("address", "updated_at", "status", "temp", "feels", "sunsetText",
            "sunsetTime", "sunriseText", "sunriseTime", "windText", "windValue")
        var colorString: String

        if (day){
            mainLayout.background = ResourcesCompat.getDrawable(resources,
                R.drawable.bg_gradient_day, null)
            colorString = "#000000"
        } else {
            mainLayout.background = ResourcesCompat.getDrawable(resources,
                R.drawable.bg_gradient_night, null)
            colorString = "#FFFFFF"
        }

        /*
        for (e in viewList){
            var idString = resources.getIdentifier("R.id.$e", "id", this.packageName)
            println(idString)
            findViewById<TextView>(idString).setTextColor(Color.parseColor(colorString)).
        }
         */

        findViewById<TextView>(R.id.address).setTextColor(Color.parseColor(colorString))
        findViewById<TextView>(R.id.updated_at).setTextColor(Color.parseColor(colorString))
        findViewById<TextView>(R.id.status).setTextColor(Color.parseColor(colorString))
        findViewById<TextView>(R.id.temp).setTextColor(Color.parseColor(colorString))
        findViewById<TextView>(R.id.feels).setTextColor(Color.parseColor(colorString))
        findViewById<TextView>(R.id.sunsetText).setTextColor(Color.parseColor(colorString))
        findViewById<TextView>(R.id.sunsetTime).setTextColor(Color.parseColor(colorString))
        findViewById<TextView>(R.id.sunriseText).setTextColor(Color.parseColor(colorString))
        findViewById<TextView>(R.id.sunriseTime).setTextColor(Color.parseColor(colorString))
        findViewById<TextView>(R.id.windText).setTextColor(Color.parseColor(colorString))
        findViewById<TextView>(R.id.windValue).setTextColor(Color.parseColor(colorString))

    }

    /**
     * Select correct icon corresponding to the iconId received from the OpenWeatherMap API
     */
    private fun getIcon(iconId: String, id: String) {
        val weatherIconId = resources.getIdentifier(id+iconId, "drawable", packageName)
        val sunrise = resources.getIdentifier(id+"sunrise", "drawable", packageName)
        val sunset = resources.getIdentifier(id+"sunset", "drawable", packageName)
        val wind = resources.getIdentifier(id+"wind", "drawable", packageName)

        // Set main weather icon
        Glide.with(this).load(weatherIconId).into(findViewById(R.id.weatherIcon))

        // Set smaller icons
        Glide.with(this).load(sunrise).into(findViewById(R.id.sunriseIcon))
        Glide.with(this).load(sunset).into(findViewById(R.id.sunsetIcon))
        Glide.with(this).load(wind).into(findViewById(R.id.windIcon))
    }

    /**
     * Getting last location.
     */
    @SuppressLint("MissingPermission")
    private fun getLastLocation() {
        if (checkPermissions()) {
            if (isLocationEnabled()) {
                mFusedLocationClient.lastLocation.addOnCompleteListener(this) { task ->
                    var location: Location? = task.result
                    if (location == null) {
                    } else {
                        var lat = location.latitude.toString()
                        var lon = location.longitude.toString()
                        weatherTask(lat, lon).execute()
                    }
                }
            } else {
                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                startActivity(intent)
            }
        } else {
            requestPermissions()
        }
    }

    /**
     * Controls if location settings are enabled on the device
     */
    private fun isLocationEnabled(): Boolean {
        var locationManager: LocationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
            LocationManager.NETWORK_PROVIDER
        )
    }

    /**
     * Controls if the application is given permission to access location data.
     */
    private fun checkPermissions(): Boolean {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            return true
        }
        return false
    }

    /**
     * Requests location permission from the user
     */
    private fun requestPermissions() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION),
            PERMISSION_ID
        )
    }


    @SuppressLint("MissingSuperCall")
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode == PERMISSION_ID) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                getLastLocation()
            }
        }
    }
}
package com.example.weatherapp

import android.Manifest
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import org.json.JSONObject
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    private val apiKey = "0fceef31d754dfaa05e0406b95abb4d5"

    private lateinit var weatherText: TextView
    private lateinit var forecastText: TextView
    private lateinit var hourlyText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val cityInput = findViewById<EditText>(R.id.cityInput)
        val searchButton = findViewById<Button>(R.id.searchButton)
        val locationButton = findViewById<Button>(R.id.locationButton)

        weatherText = findViewById(R.id.weatherText)
        forecastText = findViewById(R.id.forecastText)
        hourlyText = findViewById(R.id.hourlyText)

        searchButton.setOnClickListener {
            val city = cityInput.text.toString().trim()

            if (city.isNotEmpty()) {
                getWeatherByCity(city)
                getForecastByCity(city)
            } else {
                weatherText.text = "Please enter a city."
                forecastText.text = ""
                hourlyText.text = ""
            }
        }

        locationButton.setOnClickListener {
            getWeatherByLocation()
        }
    }

    private fun getIcon(description: String): String {
        return when {
            description.contains("clear", true) -> "☀️"
            description.contains("cloud", true) -> "☁️"
            description.contains("rain", true) -> "🌧️"
            description.contains("storm", true) -> "⛈️"
            description.contains("snow", true) -> "❄️"
            description.contains("fog", true) || description.contains("mist", true) -> "🌫️"
            else -> "🌤️"
        }
    }

    private fun formatTime(unix: Long): String {
        val date = Date(unix * 1000)
        val format = SimpleDateFormat("h:mm a", Locale.US)
        return format.format(date)
    }

    private fun getDayName(dateText: String): String {
        val input = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        val output = SimpleDateFormat("EEEE", Locale.US)
        return output.format(input.parse(dateText)!!)
    }

    private fun getWeatherByCity(city: String) {
        thread {
            try {
                val url =
                    "https://api.openweathermap.org/data/2.5/weather?q=$city&units=imperial&appid=$apiKey"

                val json = JSONObject(URL(url).readText())
                showCurrentWeather(json, city)

            } catch (e: Exception) {
                runOnUiThread {
                    weatherText.text = "Unable to retrieve weather."
                }
            }
        }
    }

    private fun getForecastByCity(city: String) {
        thread {
            try {
                val url =
                    "https://api.openweathermap.org/data/2.5/forecast?q=$city&units=imperial&appid=$apiKey"

                val json = JSONObject(URL(url).readText())
                showForecast(json)

            } catch (e: Exception) {
                runOnUiThread {
                    forecastText.text = "Unable to retrieve forecast."
                    hourlyText.text = "Unable to retrieve hourly forecast."
                }
            }
        }
    }

    private fun showCurrentWeather(json: JSONObject, city: String) {
        val main = json.getJSONObject("main")
        val weather = json.getJSONArray("weather").getJSONObject(0)
        val wind = json.getJSONObject("wind")

        val temp = main.getDouble("temp")
        val feelsLike = main.getDouble("feels_like")
        val humidity = main.getInt("humidity")
        val pressure = main.getInt("pressure")
        val windSpeed = wind.getDouble("speed")
        val visibility = json.getInt("visibility") / 1000.0
        val description = weather.getString("description")
        val icon = getIcon(description)

        val sunrise = formatTime(json.getJSONObject("sys").getLong("sunrise"))
        val sunset = formatTime(json.getJSONObject("sys").getLong("sunset"))

        runOnUiThread {
            weatherText.text = """
$icon Weather Report

📍 City: $city

🌡 Temperature: $temp°F

🥵 Feels Like: $feelsLike°F

💧 Humidity: $humidity%

💨 Wind Speed: $windSpeed mph

📊 Pressure: $pressure hPa

👀 Visibility: $visibility km

🌅 Sunrise: $sunrise

🌇 Sunset: $sunset

$icon Condition: $description
""".trimIndent()
        }
    }

    private fun showForecast(json: JSONObject) {
        val list = json.getJSONArray("list")

        val forecastBuilder = StringBuilder()
        forecastBuilder.append("📅 5-Day Forecast\n\n")

        val hourlyBuilder = StringBuilder()
        hourlyBuilder.append("🕒 Hourly Forecast\n\n")

        var daysAdded = 0

        for (i in 0 until list.length()) {
            val item = list.getJSONObject(i)
            val dateTime = item.getString("dt_txt")
            val main = item.getJSONObject("main")
            val weather = item.getJSONArray("weather").getJSONObject(0)

            val temp = main.getDouble("temp")
            val description = weather.getString("description")
            val icon = getIcon(description)

            if (i < 8) {
                val timeOnly = dateTime.substring(11, 16)
                hourlyBuilder.append("🕒 $timeOnly\n")
                hourlyBuilder.append("$icon $temp°F - $description\n\n")
            }

            if (dateTime.contains("12:00:00")) {
                val day = getDayName(dateTime)

                forecastBuilder.append("━━━━━━━━━━━━\n")
                forecastBuilder.append("📅 $day\n")
                forecastBuilder.append("$icon Condition: $description\n")
                forecastBuilder.append("🌡 Temperature: $temp°F\n\n")

                daysAdded++

                if (daysAdded == 5) break
            }
        }

        runOnUiThread {
            forecastText.text = forecastBuilder.toString()
            hourlyText.text = hourlyBuilder.toString()
        }
    }

    private fun getWeatherByLocation() {
        if (
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                100
            )
            return
        }

        val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager

        val location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)

        if (location == null) {
            weatherText.text = "Location not available. Try searching by city."
            return
        }

        val lat = location.latitude
        val lon = location.longitude

        thread {
            try {
                val weatherUrl =
                    "https://api.openweathermap.org/data/2.5/weather?lat=$lat&lon=$lon&units=imperial&appid=$apiKey"

                val forecastUrl =
                    "https://api.openweathermap.org/data/2.5/forecast?lat=$lat&lon=$lon&units=imperial&appid=$apiKey"

                val weatherJson = JSONObject(URL(weatherUrl).readText())
                val forecastJson = JSONObject(URL(forecastUrl).readText())

                val cityName = weatherJson.getString("name")

                showCurrentWeather(weatherJson, cityName)
                showForecast(forecastJson)

            } catch (e: Exception) {
                runOnUiThread {
                    weatherText.text = "Unable to retrieve location weather."
                }
            }
        }
    }
}
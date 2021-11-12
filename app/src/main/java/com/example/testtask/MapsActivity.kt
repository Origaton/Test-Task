package com.example.testtask

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.example.testtask.databinding.ActivityMapsBinding
import com.example.testtask.location_info.LocationInfo
import com.google.android.gms.maps.model.PolylineOptions
import com.google.maps.android.SphericalUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.text.DecimalFormat

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var bindingClass: ActivityMapsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bindingClass = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(bindingClass.root)

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    /**
     * В этом методе после создания карты происходит асинхронное
     * получение данных с сервера
     */
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        CoroutineScope(Dispatchers.Main).launch {
            bindingClass.progressBar.visibility = View.VISIBLE
            val locationTask = async {
                try {
                    getLocation()
                } catch (e: Exception) {
                    null
                }
            }
            val location = locationTask.await()
            bindingClass.progressBar.visibility = View.GONE
            bindingClass.firstTrekBt.visibility = View.VISIBLE
            bindingClass.lengthTV.text = getString(R.string.coordinates_received)
            if (location != null) {
                interfaceHandler(location)
            }
        }
    }

    /**
     * Отдельный метод просто возвращающий data class
     */
    private suspend fun getLocation(): LocationInfo {
        return LocationReceiver().getLocationInfo()
    }

    /**
     * Метод отвечает за обработку нажатий на кнопки
     * Переменная trekNumber равна порядковому номеру маршрута в списке
     * Создание маршрута тоже на всякий случай выполняется асинхронно чтобы не было
     * проблем с картой при медленной работе устройсва
     */
    private fun interfaceHandler(location: LocationInfo) {
        var trekNumber = 0
        bindingClass.firstTrekBt.setOnClickListener {
            CoroutineScope(Dispatchers.Main).launch {
                createTrek(location, 0)
                bindingClass.firstTrekBt.visibility = View.GONE
                bindingClass.nextTrekBt.visibility = View.VISIBLE
                bindingClass.previousTrekBt.visibility = View.VISIBLE
            }
        }
        bindingClass.nextTrekBt.setOnClickListener {
            if (trekNumber < location.features[0].geometry.coordinates.lastIndex && trekNumber >= 0) {
                mMap.clear()
                trekNumber++
                CoroutineScope(Dispatchers.Main).launch {
                    createTrek(location, trekNumber)
                }
            } else {
                Toast.makeText(applicationContext, "Показаны все маршруты", Toast.LENGTH_SHORT)
                    .show()
            }
        }
        bindingClass.previousTrekBt.setOnClickListener {
            if (trekNumber <= location.features[0].geometry.coordinates.lastIndex && trekNumber > 0) {
                mMap.clear()
                trekNumber--
                CoroutineScope(Dispatchers.Main).launch {
                    createTrek(location, trekNumber)
                }
            } else {
                Toast.makeText(applicationContext, "Показан первый маршрут", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    /**
     * Метод рисует маршрут на карте, ставит маркеры и перемещает камеру
     * Пары координат(долгота, широта) для одного маршрута собираются в одельный массив(coordinates)
     * и преобразуются в LatLng, чтобы передать карте сразу все точки
     */
    private suspend fun createTrek(location: LocationInfo, trekNumber: Int) {
        val trekPoints = location.features[0].geometry.coordinates[trekNumber][0]
        val coordinates = ArrayList<LatLng>()
        for (point in trekPoints) {
            coordinates.add(LatLng(point[1], point[0]))
        }

        //отрисовка маршрута
        mMap.addPolyline(
            PolylineOptions()
                .clickable(false)
                .addAll(coordinates)
        )

        //создание маркера
        mMap.addMarker(
            MarkerOptions().position(
                LatLng(
                    trekPoints.last()[1],
                    trekPoints.last()[0]
                )
            )
        )

        //перемащение камеры
        mMap.moveCamera(
            CameraUpdateFactory
                .newLatLngZoom(
                    LatLng(
                        trekPoints.last()[1],
                        trekPoints.last()[0]
                    ),
                    10F
                )
        )

        //расчет длины
        distanceCalculation(coordinates)

        //отображение номера маршрута
        bindingClass.trekNumberTV.text =
            "${trekNumber + 1}/${location.features[0].geometry.coordinates.size + 1}"
    }

    /**
     * Метод считает длину маршрута и показывает ее в километрах с двумя знаками после запятой
     */
    private fun distanceCalculation(coordinates: ArrayList<LatLng>) {
        val length = SphericalUtil.computeLength(coordinates) / 1000
        val formattedLength = DecimalFormat("0.00").format(length)
        bindingClass.lengthTV.text = getString(R.string.length) + " $formattedLength км"
    }
}
package com.example.core.utils

import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

data class LatLngPoint(
    val latitude: Double,
    val longitude: Double
)

data class SurgeZoneCell(
    val geohash: String,
    val zoneName: String,
    val centerLat: Double,
    val centerLng: Double,
    val multiplier: Double,
    val activeOrdersCount: Int,
    val estimatedWaitMin: Int,
    val normalizedX: Float, // 0f..1f on dashboard map canvas
    val normalizedY: Float
)

data class ManeuverStep(
    val instruction: String,
    val streetName: String,
    val distanceMeters: Int,
    val turnType: String // "STRAIGHT", "RIGHT", "LEFT", "ARRIVE"
)

object GeoUtils {
    private const val BASE32 = "0123456789bcdefghjkmnpqrstuvwxyz"

    /**
     * Encodes a latitude and longitude into a standard Base-32 Geohash string.
     * Used by Zaldi Driver's location_service and order_engine for spatial indexing.
     */
    fun encodeGeohash(latitude: Double, longitude: Double, precision: Int = 7): String {
        var latMin = -90.0
        var latMax = 90.0
        var lonMin = -180.0
        var lonMax = 180.0

        var isEven = true
        var bit = 0
        var ch = 0
        val geohash = StringBuilder()
        val bits = intArrayOf(16, 8, 4, 2, 1)

        val clampedLat = latitude.coerceIn(-89.9999, 89.9999)
        val clampedLon = longitude.coerceIn(-179.9999, 179.9999)

        while (geohash.length < precision) {
            if (isEven) {
                val mid = (lonMin + lonMax) / 2.0
                if (clampedLon >= mid) {
                    ch = ch or bits[bit]
                    lonMin = mid
                } else {
                    lonMax = mid
                }
            } else {
                val mid = (latMin + latMax) / 2.0
                if (clampedLat >= mid) {
                    ch = ch or bits[bit]
                    latMin = mid
                } else {
                    latMax = mid
                }
            }
            isEven = !isEven
            if (bit < 4) {
                bit++
            } else {
                geohash.append(BASE32[ch])
                bit = 0
                ch = 0
            }
        }
        return geohash.toString()
    }

    /**
     * Haversine distance in kilometers between two GPS coordinates.
     */
    fun calculateDistanceKm(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double
    ): Double {
        val earthRadiusKm = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return earthRadiusKm * c
    }

    fun formatDistance(distanceKm: Double): String {
        return if (distanceKm < 1.0) {
            "${(distanceKm * 1000).roundToInt()} m"
        } else {
            String.format(Locale.US, "%.1f km", distanceKm)
        }
    }

    fun formatCurrency(amount: Double): String {
        return String.format(Locale.US, "$%.2f", amount)
    }

    fun isValidPhone(phone: String): Boolean {
        val digits = phone.filter { it.isDigit() }
        return digits.length in 8..15
    }

    fun isValidOtp(otp: String): Boolean {
        return otp.length == 6 && otp.all { it.isDigit() }
    }

    fun isValidLicensePlate(plate: String): Boolean {
        val cleaned = plate.trim()
        return cleaned.length in 4..10 && cleaned.any { it.isLetterOrDigit() }
    }

    /**
     * Generates surrounding high-demand Geohash surge cells relative to the driver's position.
     */
    fun generateSurgeHeatmapCells(centerLat: Double, centerLng: Double): List<SurgeZoneCell> {
        val offsets = listOf(
            Triple(0.008, -0.010, "Financial District Core"),
            Triple(-0.006, 0.012, "Central Transit Hub"),
            Triple(0.014, 0.009, "Tech Innovation Park"),
            Triple(-0.012, -0.008, "Waterfront Arena"),
            Triple(0.002, 0.003, "Midtown Commercial"),
            Triple(-0.003, -0.015, "University Quarter")
        )
        val multipliers = listOf(2.2, 1.8, 1.6, 1.9, 1.4, 1.3)
        val orders = listOf(28, 19, 14, 23, 11, 8)
        val waits = listOf(1, 2, 3, 1, 3, 4)
        val canvasCoords = listOf(
            0.28f to 0.26f,
            0.74f to 0.62f,
            0.68f to 0.24f,
            0.25f to 0.72f,
            0.52f to 0.46f,
            0.18f to 0.48f
        )

        return offsets.mapIndexed { index, (dLat, dLng, name) ->
            val lat = centerLat + dLat
            val lng = centerLng + dLng
            SurgeZoneCell(
                geohash = encodeGeohash(lat, lng, 7),
                zoneName = name,
                centerLat = lat,
                centerLng = lng,
                multiplier = multipliers[index],
                activeOrdersCount = orders[index],
                estimatedWaitMin = waits[index],
                normalizedX = canvasCoords[index].first,
                normalizedY = canvasCoords[index].second
            )
        }
    }

    /**
     * Generates realistic urban street waypoints for live route polyline rendering.
     */
    fun generateRoutePolyline(
        driverLat: Double,
        driverLng: Double,
        pickupLat: Double,
        pickupLng: Double,
        dropoffLat: Double,
        dropoffLng: Double
    ): List<LatLngPoint> {
        val p0 = LatLngPoint(driverLat, driverLng)
        val p1 = LatLngPoint(driverLat + (pickupLat - driverLat) * 0.5, driverLng + (pickupLng - driverLng) * 0.15)
        val p2 = LatLngPoint(pickupLat, pickupLng)
        val p3 = LatLngPoint(pickupLat + (dropoffLat - pickupLat) * 0.35, pickupLng + (dropoffLng - pickupLng) * 0.2)
        val p4 = LatLngPoint(pickupLat + (dropoffLat - pickupLat) * 0.68, pickupLng + (dropoffLng - pickupLng) * 0.82)
        val p5 = LatLngPoint(dropoffLat, dropoffLng)
        return listOf(p0, p1, p2, p3, p4, p5)
    }

    fun getCurrentManeuver(
        orderStatus: String,
        routeProgress: Float,
        pickupAddress: String,
        dropoffAddress: String
    ): ManeuverStep {
        return when (orderStatus) {
            "ACCEPTED" -> ManeuverStep(
                instruction = "Head northeast toward Pickup Zone",
                streetName = pickupAddress,
                distanceMeters = ((1f - routeProgress.coerceIn(0f, 1f)) * 650).roundToInt().coerceAtLeast(30),
                turnType = "RIGHT"
            )
            "ARRIVED_PICKUP" -> ManeuverStep(
                instruction = "Waiting for Passenger Boarding",
                streetName = pickupAddress,
                distanceMeters = 0,
                turnType = "ARRIVE"
            )
            "IN_PROGRESS" -> {
                when {
                    routeProgress < 0.35f -> ManeuverStep(
                        instruction = "Continue straight on Express Corridor",
                        streetName = "Zaldi Arterial Pkwy",
                        distanceMeters = ((0.35f - routeProgress) * 2400).roundToInt().coerceAtLeast(80),
                        turnType = "STRAIGHT"
                    )
                    routeProgress < 0.75f -> ManeuverStep(
                        instruction = "Turn left onto Destination Ave",
                        streetName = dropoffAddress,
                        distanceMeters = ((0.75f - routeProgress) * 1800).roundToInt().coerceAtLeast(60),
                        turnType = "LEFT"
                    )
                    else -> ManeuverStep(
                        instruction = "Approaching Passenger Dropoff on right",
                        streetName = dropoffAddress,
                        distanceMeters = ((1f - routeProgress) * 900).roundToInt().coerceAtLeast(15),
                        turnType = "ARRIVE"
                    )
                }
            }
            else -> ManeuverStep(
                instruction = "Route Standby",
                streetName = "Scanning Active Sector",
                distanceMeters = 0,
                turnType = "STRAIGHT"
            )
        }
    }
}

package com.example

import com.example.core.utils.GeoUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun geohash_encodesSanFranciscoCorrectly() {
        val hash = GeoUtils.encodeGeohash(37.7897, -122.4014, 7)
        assertEquals(7, hash.length)
        assertTrue(hash.startsWith("9q8yy"))
    }

    @Test
    fun distance_calculatesPositiveKilometers() {
        val distanceKm = GeoUtils.calculateDistanceKm(37.7897, -122.4014, 37.8009, -122.3986)
        assertTrue(distanceKm > 1.0 && distanceKm < 5.0)
    }

    @Test
    fun validators_validatePhoneOtpAndPlate() {
        assertTrue(GeoUtils.isValidPhone("+1 (415) 890-4210"))
        assertFalse(GeoUtils.isValidPhone("123"))
        assertTrue(GeoUtils.isValidOtp("482910"))
        assertFalse(GeoUtils.isValidOtp("48291A"))
        assertTrue(GeoUtils.isValidLicensePlate("ZLD-904E"))
    }
}

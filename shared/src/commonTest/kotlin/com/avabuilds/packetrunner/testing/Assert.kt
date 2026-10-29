package com.avabuilds.packetrunner.testing

import kotlin.math.abs
import kotlin.test.fail

// The assertions the tests were written with (JUnit style: the optional message comes first), on top of kotlin.test,
// so the same tests run on Android and in the browser.

fun assertEquals(expected: Any?, actual: Any?) = kotlin.test.assertEquals(expected, actual)
fun assertEquals(expected: Long, actual: Long) = kotlin.test.assertEquals(expected, actual)
fun assertEquals(message: String, expected: Any?, actual: Any?) = kotlin.test.assertEquals(expected, actual, message)
fun assertEquals(message: String, expected: Long, actual: Long) = kotlin.test.assertEquals(expected, actual, message)

fun assertEquals(expected: Float, actual: Float, delta: Float) {
    if (abs(expected - actual) > delta) fail("expected <$expected> but was <$actual> (delta $delta)")
}

fun assertEquals(expected: Double, actual: Double, delta: Double) {
    if (abs(expected - actual) > delta) fail("expected <$expected> but was <$actual> (delta $delta)")
}

fun assertNotEquals(unexpected: Any?, actual: Any?) = kotlin.test.assertNotEquals(unexpected, actual)
fun assertNotEquals(message: String, unexpected: Any?, actual: Any?) = kotlin.test.assertNotEquals(unexpected, actual, message)

fun assertTrue(condition: Boolean) = kotlin.test.assertTrue(condition)
fun assertTrue(message: String, condition: Boolean) = kotlin.test.assertTrue(condition, message)
fun assertFalse(condition: Boolean) = kotlin.test.assertFalse(condition)
fun assertFalse(message: String, condition: Boolean) = kotlin.test.assertFalse(condition, message)

fun assertNull(actual: Any?) = kotlin.test.assertNull(actual)
fun assertNull(message: String, actual: Any?) = kotlin.test.assertNull(actual, message)
fun assertNotNull(actual: Any?) = kotlin.test.assertNotNull(actual)
fun assertNotNull(message: String, actual: Any?) = kotlin.test.assertNotNull(actual, message)

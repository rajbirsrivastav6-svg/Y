package com.example

import com.example.engine.OfflineMathEvaluator
import com.example.engine.OfflineUnitConverter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun mathEvaluator_handlesExpressionsAndPercentages() {
    val pct = OfflineMathEvaluator.tryEvaluate("25% of 480")
    assertNotNull(pct)
    assertEquals("120", pct?.formattedResult)

    val expr = OfflineMathEvaluator.tryEvaluate("(12 + 8) * 3 - sqrt(144)")
    assertNotNull(expr)
    assertEquals("48", expr?.formattedResult)
  }

  @Test
  fun unitConverter_handlesTemperatureAndDistance() {
    val temp = OfflineUnitConverter.tryConvert("37 c to f")
    assertNotNull(temp)
    assertEquals("98.6 °F", temp?.targetSummary)

    val dist = OfflineUnitConverter.tryConvert("10 km to m")
    assertNotNull(dist)
    assertEquals("10,000 m", dist?.targetSummary)
  }
}

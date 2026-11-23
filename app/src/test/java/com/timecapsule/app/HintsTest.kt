package com.timecapsule.app

import com.timecapsule.app.util.Hint
import com.timecapsule.app.util.distanceText
import com.timecapsule.app.util.hotColdHint
import com.timecapsule.app.util.trend
import org.junit.Assert.assertEquals
import org.junit.Test

/** "Caliente o frío" para encontrar una cápsula guardada en un lugar. */
class HintsTest {
    @Test fun `sin GPS todavia esta buscando`() = assertEquals(Hint.BUSCANDO, hotColdHint(null, 150))
    @Test fun `dentro del radio llegaste`() = assertEquals(Hint.LLEGASTE, hotColdHint(120f, 150))
    @Test fun `justo en el borde del radio tambien llegaste`() = assertEquals(Hint.LLEGASTE, hotColdHint(150f, 150))
    @Test fun `a menos de 200 m muy caliente`() = assertEquals(Hint.MUY_CALIENTE, hotColdHint(180f, 150))
    @Test fun `a menos de 1 km caliente`() = assertEquals(Hint.CALIENTE, hotColdHint(700f, 150))
    @Test fun `a menos de 5 km tibio`() = assertEquals(Hint.TIBIO, hotColdHint(3000f, 150))
    @Test fun `lejos frio`() = assertEquals(Hint.FRIO, hotColdHint(12000f, 150))
    @Test fun `un radio grande gana sobre muy caliente`() = assertEquals(Hint.LLEGASTE, hotColdHint(180f, 500))

    @Test fun `la primera medida no tiene tendencia`() = assertEquals("", trend(500f, null))
    @Test fun `acercandose`() = assertEquals("Vas por buen camino ↓", trend(480f, 500f))
    @Test fun `alejandose`() = assertEquals("Te estás alejando ↑", trend(520f, 500f))
    @Test fun `el ruido del GPS no cambia la tendencia`() = assertEquals("Sigue moviéndote", trend(503f, 500f))

    @Test fun `metros`() = assertEquals("350 m", distanceText(350.7f))
    @Test fun `kilometros con un decimal`() = assertEquals("1.2 km", distanceText(1234f))
}

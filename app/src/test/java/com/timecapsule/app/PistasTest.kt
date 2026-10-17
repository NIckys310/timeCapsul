package com.timecapsule.app

import com.timecapsule.app.util.Pista
import com.timecapsule.app.util.distanciaTexto
import com.timecapsule.app.util.pistaCalienteFrio
import com.timecapsule.app.util.tendencia
import org.junit.Assert.assertEquals
import org.junit.Test

/** "Caliente o frío" para encontrar una cápsula guardada en un lugar. */
class PistasTest {
    @Test fun `sin GPS todavia esta buscando`() = assertEquals(Pista.BUSCANDO, pistaCalienteFrio(null, 150))
    @Test fun `dentro del radio llegaste`() = assertEquals(Pista.LLEGASTE, pistaCalienteFrio(120f, 150))
    @Test fun `justo en el borde del radio tambien llegaste`() = assertEquals(Pista.LLEGASTE, pistaCalienteFrio(150f, 150))
    @Test fun `a menos de 200 m muy caliente`() = assertEquals(Pista.MUY_CALIENTE, pistaCalienteFrio(180f, 150))
    @Test fun `a menos de 1 km caliente`() = assertEquals(Pista.CALIENTE, pistaCalienteFrio(700f, 150))
    @Test fun `a menos de 5 km tibio`() = assertEquals(Pista.TIBIO, pistaCalienteFrio(3000f, 150))
    @Test fun `lejos frio`() = assertEquals(Pista.FRIO, pistaCalienteFrio(12000f, 150))
    @Test fun `un radio grande gana sobre muy caliente`() = assertEquals(Pista.LLEGASTE, pistaCalienteFrio(180f, 500))

    @Test fun `la primera medida no tiene tendencia`() = assertEquals("", tendencia(500f, null))
    @Test fun `acercandose`() = assertEquals("Vas por buen camino ↓", tendencia(480f, 500f))
    @Test fun `alejandose`() = assertEquals("Te estás alejando ↑", tendencia(520f, 500f))
    @Test fun `el ruido del GPS no cambia la tendencia`() = assertEquals("Sigue moviéndote", tendencia(503f, 500f))

    @Test fun `metros`() = assertEquals("350 m", distanciaTexto(350.7f))
    @Test fun `kilometros con un decimal`() = assertEquals("1.2 km", distanciaTexto(1234f))
}

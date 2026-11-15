package com.novaremote.transport

import org.json.JSONArray
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeAssistantClientTest {
    @Test fun acceptsPrivateLocalServerOrigins() {
        assertNull(HomeAssistantAddressPolicy.validate("http://192.168.1.20:8123"))
        assertNull(HomeAssistantAddressPolicy.validate("https://ha.local:8123"))
        assertNull(HomeAssistantAddressPolicy.validate("http://10.0.2.2:8123"))
    }

    @Test fun rejectsPublicHostsAndUrlInjection() {
        assertNotNull(HomeAssistantAddressPolicy.validate("https://example.com:8123"))
        assertNotNull(HomeAssistantAddressPolicy.validate("http://user:pass@192.168.1.20:8123"))
        assertNotNull(HomeAssistantAddressPolicy.validate("http://192.168.1.20:8123/api/states"))
        assertNotNull(HomeAssistantAddressPolicy.validate("http://192.168.1.20:0"))
    }

    @Test fun accessTokenRejectsHeaderInjectionAndWhitespace() {
        assertTrue(isValidHomeAssistantToken("0123456789abcdefABCDEF"))
        assertFalse(isValidHomeAssistantToken("0123456789abcdef\nInjected: true"))
        assertFalse(isValidHomeAssistantToken("short"))
    }

    @Test fun parserReturnsOnlyMediaPlayerEntitiesSortedByFriendlyName() {
        val states = JSONArray(
            """[
              {"entity_id":"sensor.temperature","state":"21","attributes":{"friendly_name":"Room temperature"}},
              {"entity_id":"media_player.den_tv","state":"playing","attributes":{"friendly_name":"Den TV","manufacturer":"Example","model":"Panel 4K","media_title":"News","volume_level":0.35}},
              {"entity_id":"media_player.living_room","state":"unavailable","attributes":{"friendly_name":"Living room TV"}}
            ]"""
        )
        val players = parseHomeAssistantMediaPlayers(states)

        assertEquals(2, players.size)
        assertEquals("Den TV", players[0].friendlyName)
        assertEquals("media_player.den_tv", players[0].entityId)
        assertEquals("Example", players[0].manufacturer)
        assertEquals("Panel 4K", players[0].model)
        assertEquals("News", players[0].mediaTitle)
        assertEquals(0.35, players[0].volumeLevel!!, 0.0001)
        assertTrue(players[0].isAvailable)
        assertFalse(players[1].isAvailable)
    }

    @Test fun actionsUseFixedHomeAssistantServiceNames() {
        assertEquals("turn_on", HomeAssistantMediaAction.POWER_ON.service)
        assertEquals("turn_off", HomeAssistantMediaAction.POWER_OFF.service)
        assertEquals("media_play_pause", HomeAssistantMediaAction.PLAY_PAUSE.service)
        assertEquals("volume_mute", HomeAssistantMediaAction.MUTE.service)
    }
}

package com.novaremote.transport

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class BridgeAddressPolicyTest {
    @Test fun acceptsPrivateAddress() {
        assertNull(BridgeAddressPolicy.validate("http://192.168.1.40:80"))
    }
    @Test fun acceptsMdnsName() {
        assertNull(BridgeAddressPolicy.validate("http://novaremote-123.local"))
    }
    @Test fun rejectsPublicHost() {
        assertNotNull(BridgeAddressPolicy.validate("http://example.com"))
    }
    @Test fun rejectsCredentialsAndPath() {
        assertNotNull(BridgeAddressPolicy.validate("http://u:p@192.168.1.2/path"))
    }
    @Test fun rejectsPortZero() {
        assertNotNull(BridgeAddressPolicy.validate("http://192.168.1.2:0"))
    }
}

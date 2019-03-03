package com.decawave.argomanager;

import com.decawave.argomanager.util.gatt.GattEncoder;

import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.assertEquals;

/**
 */
public class UuidSerializationTest extends ArgoLocalTest {
    private static final UUID TEST_UUID = UUID.fromString("fe169723-7414-4648-a2c5-ef9530733aba");

    @Test
    public void testSerialization() throws Exception {
        byte[] bytes = GattEncoder.encodeUuid(TEST_UUID);
        UUID deserUid = GattEncoder.decodeUuid(bytes);
        assertEquals(TEST_UUID, deserUid);
    }

}
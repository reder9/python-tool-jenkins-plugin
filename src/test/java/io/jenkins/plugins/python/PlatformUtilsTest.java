package io.jenkins.plugins.python;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PlatformUtilsTest {

    @Test
    void testParseOS() {
        assertEquals(PlatformUtils.OS.WINDOWS, PlatformUtils.parseOS("Windows 11"));
        assertEquals(PlatformUtils.OS.WINDOWS, PlatformUtils.parseOS("Windows Server 2022"));
        assertEquals(PlatformUtils.OS.MACOS, PlatformUtils.parseOS("Mac OS X"));
        assertEquals(PlatformUtils.OS.MACOS, PlatformUtils.parseOS("Darwin"));
        assertEquals(PlatformUtils.OS.LINUX, PlatformUtils.parseOS("Linux"));
        assertEquals(PlatformUtils.OS.LINUX, PlatformUtils.parseOS("Ubuntu 24.04"));
        assertEquals(PlatformUtils.OS.LINUX, PlatformUtils.parseOS("Alpine Linux v3.19"));
        assertEquals(PlatformUtils.OS.UNKNOWN, PlatformUtils.parseOS(null));
        assertEquals(PlatformUtils.OS.UNKNOWN, PlatformUtils.parseOS("FreeBSD"));
        assertEquals(PlatformUtils.OS.UNKNOWN, PlatformUtils.parseOS("SunOS"));
    }

    @Test
    void testParseArch() {
        assertEquals(PlatformUtils.Arch.X86_64, PlatformUtils.parseArch("x86_64"));
        assertEquals(PlatformUtils.Arch.X86_64, PlatformUtils.parseArch("amd64"));
        assertEquals(PlatformUtils.Arch.AARCH64, PlatformUtils.parseArch("aarch64"));
        assertEquals(PlatformUtils.Arch.AARCH64, PlatformUtils.parseArch("arm64"));
        assertEquals(PlatformUtils.Arch.X86, PlatformUtils.parseArch("x86"));
        assertEquals(PlatformUtils.Arch.X86, PlatformUtils.parseArch("i686"));
        assertEquals(PlatformUtils.Arch.ARMV7, PlatformUtils.parseArch("armv7"));
        assertEquals(PlatformUtils.Arch.ARMV7, PlatformUtils.parseArch("armhf"));
        assertEquals(PlatformUtils.Arch.PPC64LE, PlatformUtils.parseArch("ppc64le"));
        assertEquals(PlatformUtils.Arch.PPC64LE, PlatformUtils.parseArch("powerpc64le"));
        assertEquals(PlatformUtils.Arch.S390X, PlatformUtils.parseArch("s390x"));
        assertEquals(PlatformUtils.Arch.UNKNOWN, PlatformUtils.parseArch("sparc"));
        assertEquals(PlatformUtils.Arch.UNKNOWN, PlatformUtils.parseArch(null));
    }

    @Test
    void testToPlatformKey() {
        assertEquals("linux-x86_64", PlatformUtils.toPlatformKey(PlatformUtils.OS.LINUX, PlatformUtils.Arch.X86_64));
        assertEquals("linux-aarch64", PlatformUtils.toPlatformKey(PlatformUtils.OS.LINUX, PlatformUtils.Arch.AARCH64));
        assertEquals("linux-x86_64-musl", PlatformUtils.toPlatformKey(PlatformUtils.OS.LINUX, PlatformUtils.Arch.X86_64, true));
        assertEquals("linux-aarch64-musl", PlatformUtils.toPlatformKey(PlatformUtils.OS.LINUX, PlatformUtils.Arch.AARCH64, true));
        assertEquals("linux-armv7", PlatformUtils.toPlatformKey(PlatformUtils.OS.LINUX, PlatformUtils.Arch.ARMV7));
        assertEquals("linux-ppc64le", PlatformUtils.toPlatformKey(PlatformUtils.OS.LINUX, PlatformUtils.Arch.PPC64LE));
        assertEquals("linux-s390x", PlatformUtils.toPlatformKey(PlatformUtils.OS.LINUX, PlatformUtils.Arch.S390X));

        assertEquals("macos-x86_64", PlatformUtils.toPlatformKey(PlatformUtils.OS.MACOS, PlatformUtils.Arch.X86_64));
        assertEquals("macos-aarch64", PlatformUtils.toPlatformKey(PlatformUtils.OS.MACOS, PlatformUtils.Arch.AARCH64));

        assertEquals("windows-x86_64", PlatformUtils.toPlatformKey(PlatformUtils.OS.WINDOWS, PlatformUtils.Arch.X86_64));
        assertEquals("windows-aarch64", PlatformUtils.toPlatformKey(PlatformUtils.OS.WINDOWS, PlatformUtils.Arch.AARCH64));
        assertEquals("windows-x86", PlatformUtils.toPlatformKey(PlatformUtils.OS.WINDOWS, PlatformUtils.Arch.X86));
    }

    @Test
    void testFailEarlyOnUnsupported() {
        assertThrows(IllegalArgumentException.class, () ->
                PlatformUtils.toPlatformKey(PlatformUtils.OS.WINDOWS, PlatformUtils.Arch.S390X));
        assertThrows(IllegalArgumentException.class, () ->
                PlatformUtils.toPlatformKey(PlatformUtils.OS.MACOS, PlatformUtils.Arch.PPC64LE));
        assertThrows(IllegalArgumentException.class, () ->
                PlatformUtils.toPlatformKey(PlatformUtils.OS.UNKNOWN, PlatformUtils.Arch.X86_64));
    }
}

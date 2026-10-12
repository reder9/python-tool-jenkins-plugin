package io.jenkins.plugins.python;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PlatformUtilsTest {

    @Test
    void testParseOS() {
        assertEquals(PlatformUtils.OS.WINDOWS, PlatformUtils.parseOS("Windows 11"));
        assertEquals(PlatformUtils.OS.WINDOWS, PlatformUtils.parseOS("Windows Server 2022"));
        assertEquals(PlatformUtils.OS.MACOS, PlatformUtils.parseOS("Mac OS X"));
        assertEquals(PlatformUtils.OS.MACOS, PlatformUtils.parseOS("Darwin"));
        assertEquals(PlatformUtils.OS.LINUX, PlatformUtils.parseOS("Linux"));
        assertEquals(PlatformUtils.OS.LINUX, PlatformUtils.parseOS("Ubuntu 24.04"));
        assertEquals(PlatformUtils.OS.UNKNOWN, PlatformUtils.parseOS(null));
        assertEquals(PlatformUtils.OS.UNKNOWN, PlatformUtils.parseOS("FreeBSD"));
    }

    @Test
    void testParseArch() {
        assertEquals(PlatformUtils.Arch.X86_64, PlatformUtils.parseArch("x86_64"));
        assertEquals(PlatformUtils.Arch.X86_64, PlatformUtils.parseArch("amd64"));
        assertEquals(PlatformUtils.Arch.AARCH64, PlatformUtils.parseArch("aarch64"));
        assertEquals(PlatformUtils.Arch.AARCH64, PlatformUtils.parseArch("arm64"));
        assertEquals(PlatformUtils.Arch.X86, PlatformUtils.parseArch("x86"));
        assertEquals(PlatformUtils.Arch.X86, PlatformUtils.parseArch("i686"));
        assertEquals(PlatformUtils.Arch.UNKNOWN, PlatformUtils.parseArch(null));
    }

    @Test
    void testToPlatformKey() {
        assertEquals("linux-x86_64", PlatformUtils.toPlatformKey(PlatformUtils.OS.LINUX, PlatformUtils.Arch.X86_64));
        assertEquals("linux-aarch64", PlatformUtils.toPlatformKey(PlatformUtils.OS.LINUX, PlatformUtils.Arch.AARCH64));
        assertEquals("macos-x86_64", PlatformUtils.toPlatformKey(PlatformUtils.OS.MACOS, PlatformUtils.Arch.X86_64));
        assertEquals("macos-aarch64", PlatformUtils.toPlatformKey(PlatformUtils.OS.MACOS, PlatformUtils.Arch.AARCH64));
        assertEquals("windows-x86_64", PlatformUtils.toPlatformKey(PlatformUtils.OS.WINDOWS, PlatformUtils.Arch.X86_64));
        assertEquals("windows-x86", PlatformUtils.toPlatformKey(PlatformUtils.OS.WINDOWS, PlatformUtils.Arch.X86));
    }
}

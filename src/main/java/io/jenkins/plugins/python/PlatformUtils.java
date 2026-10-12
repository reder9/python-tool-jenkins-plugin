package io.jenkins.plugins.python;

import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Node;
import hudson.model.TaskListener;
import hudson.remoting.VirtualChannel;
import jenkins.security.MasterToSlaveCallable;

import java.io.IOException;
import java.io.Serializable;
import java.util.Locale;

/**
 * Utility functions for detecting host/agent OS platforms and CPU architectures
 * to map against standalone Python build targets.
 */
public final class PlatformUtils implements Serializable {

    private static final long serialVersionUID = 1L;

    public enum OS {
        WINDOWS,
        LINUX,
        MACOS,
        UNKNOWN
    }

    public enum Arch {
        X86_64,
        AARCH64,
        X86,
        UNKNOWN
    }

    private PlatformUtils() {
    }

    /**
     * Inspects a Jenkins Node to determine its OS and CPU architecture.
     */
    public static String detectPlatformKey(Node node, Launcher launcher, TaskListener listener) {
        OS os = OS.UNKNOWN;
        Arch arch = Arch.UNKNOWN;

        VirtualChannel channel = node.getChannel();
        if (channel != null) {
            try {
                PlatformInfo info = channel.call(new DetectPlatformCallable());
                os = info.os;
                arch = info.arch;
            } catch (Exception e) {
                if (listener != null) {
                    listener.getLogger().println("[Python Plugin] Warning: Remote platform probe failed: " + e.getMessage() + ", using fallback heuristic.");
                }
            }
        }

        if (os == OS.UNKNOWN) {
            if (launcher != null) {
                os = launcher.isUnix() ? OS.LINUX : OS.WINDOWS;
            } else {
                os = OS.LINUX;
            }
        }

        if (arch == Arch.UNKNOWN) {
            arch = Arch.X86_64;
        }

        return toPlatformKey(os, arch);
    }

    public static String toPlatformKey(OS os, Arch arch) {
        switch (os) {
            case WINDOWS:
                return arch == Arch.X86 ? "windows-x86" : "windows-x86_64";
            case MACOS:
                return arch == Arch.AARCH64 ? "macos-aarch64" : "macos-x86_64";
            case LINUX:
            default:
                return arch == Arch.AARCH64 ? "linux-aarch64" : "linux-x86_64";
        }
    }

    public static OS parseOS(String osName) {
        if (osName == null) {
            return OS.UNKNOWN;
        }
        String lower = osName.toLowerCase(Locale.ENGLISH);
        if (lower.contains("darwin") || lower.contains("mac") || lower.contains("osx")) {
            return OS.MACOS;
        } else if (lower.contains("windows") || lower.startsWith("win")) {
            return OS.WINDOWS;
        } else if (lower.contains("linux") || lower.contains("unix") || lower.contains("ubuntu")
                || lower.contains("debian") || lower.contains("fedora") || lower.contains("centos")
                || lower.contains("rhel") || lower.contains("alpine") || lower.contains("arch")) {
            return OS.LINUX;
        }
        return OS.UNKNOWN;
    }

    public static Arch parseArch(String archName) {
        if (archName == null) {
            return Arch.UNKNOWN;
        }
        String lower = archName.toLowerCase(Locale.ENGLISH);
        if (lower.contains("aarch64") || lower.contains("arm64")) {
            return Arch.AARCH64;
        } else if (lower.contains("amd64") || lower.contains("x86_64") || lower.contains("x64")) {
            return Arch.X86_64;
        } else if (lower.contains("x86") || lower.contains("i386") || lower.contains("i686")) {
            return Arch.X86;
        }
        return Arch.UNKNOWN;
    }

    /**
     * Resolves the primary Python binary file path given the tool home directory.
     */
    public static FilePath getPythonExecutable(FilePath home) {
        FilePath winExe = home.child("python.exe");
        FilePath unixBin = home.child("bin").child("python3");
        FilePath unixBinAlt = home.child("bin").child("python");

        try {
            if (winExe.exists()) {
                return winExe;
            }
            if (unixBin.exists()) {
                return unixBin;
            }
            if (unixBinAlt.exists()) {
                return unixBinAlt;
            }
        } catch (IOException | InterruptedException e) {
            // fallback
        }
        return unixBin;
    }

    public static class PlatformInfo implements Serializable {
        private static final long serialVersionUID = 1L;
        public final OS os;
        public final Arch arch;

        public PlatformInfo(OS os, Arch arch) {
            this.os = os;
            this.arch = arch;
        }
    }

    private static class DetectPlatformCallable extends MasterToSlaveCallable<PlatformInfo, IOException> {
        private static final long serialVersionUID = 1L;

        @Override
        public PlatformInfo call() {
            String osName = System.getProperty("os.name");
            String osArch = System.getProperty("os.arch");
            return new PlatformInfo(parseOS(osName), parseArch(osArch));
        }
    }
}

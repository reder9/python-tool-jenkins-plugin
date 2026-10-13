package io.jenkins.plugins.python;

import hudson.FilePath;
import hudson.model.Computer;
import hudson.model.Node;
import hudson.model.TaskListener;

import java.io.IOException;
import java.io.Serializable;
import java.util.Locale;
import java.util.Map;

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
        ARMV7,
        PPC64LE,
        S390X,
        UNKNOWN
    }

    private PlatformUtils() {
    }

    /**
     * Inspects a Jenkins Node to determine its OS and CPU architecture.
     * Uses controller-managed Computer system properties rather than remoting callables,
     * and fails early if the target platform is unsupported.
     */
    public static String detectPlatformKey(Node node, TaskListener listener) {
        String osName = null;
        String osArch = null;

        Computer computer = node != null ? node.toComputer() : null;
        if (computer != null) {
            try {
                Map<?, ?> props = computer.getSystemProperties();
                if (props != null) {
                    Object nameObj = props.get("os.name");
                    Object archObj = props.get("os.arch");
                    if (nameObj != null) {
                        osName = nameObj.toString();
                    }
                    if (archObj != null) {
                        osArch = archObj.toString();
                    }
                }
            } catch (Exception e) {
                if (listener != null) {
                    listener.getLogger().println("[Python Plugin] Note: Unable to read Computer system properties: " + e.getMessage());
                }
            }
        }

        if (osName == null) {
            osName = System.getProperty("os.name");
        }
        if (osArch == null) {
            osArch = System.getProperty("os.arch");
        }

        OS os = parseOS(osName);
        if (os == OS.UNKNOWN) {
            throw new IllegalArgumentException("Unsupported or unrecognized operating system: '" + osName + "' on node "
                    + (node != null ? node.getNodeName() : "unknown") + ". Supported systems are Linux, macOS, and Windows.");
        }

        Arch arch = parseArch(osArch);
        if (arch == Arch.UNKNOWN) {
            throw new IllegalArgumentException("Unsupported CPU architecture: '" + osArch + "' on OS '" + osName + "' on node "
                    + (node != null ? node.getNodeName() : "unknown") + ". Supported architectures: x86_64, aarch64, x86, armv7, ppc64le, s390x.");
        }

        boolean isMusl = osName != null && osName.toLowerCase(Locale.ENGLISH).contains("alpine");
        return toPlatformKey(os, arch, isMusl);
    }

    public static String toPlatformKey(OS os, Arch arch) {
        return toPlatformKey(os, arch, false);
    }

    public static String toPlatformKey(OS os, Arch arch, boolean isMusl) {
        switch (os) {
            case WINDOWS:
                if (arch == Arch.AARCH64) {
                    return "windows-aarch64";
                } else if (arch == Arch.X86) {
                    return "windows-x86";
                } else if (arch == Arch.X86_64) {
                    return "windows-x86_64";
                }
                throw new IllegalArgumentException("Unsupported Windows CPU architecture: " + arch);

            case MACOS:
                if (arch == Arch.AARCH64) {
                    return "macos-aarch64";
                } else if (arch == Arch.X86_64) {
                    return "macos-x86_64";
                }
                throw new IllegalArgumentException("Unsupported macOS CPU architecture: " + arch);

            case LINUX:
                if (arch == Arch.AARCH64) {
                    return isMusl ? "linux-aarch64-musl" : "linux-aarch64";
                } else if (arch == Arch.X86_64) {
                    return isMusl ? "linux-x86_64-musl" : "linux-x86_64";
                } else if (arch == Arch.ARMV7) {
                    return "linux-armv7";
                } else if (arch == Arch.PPC64LE) {
                    return "linux-ppc64le";
                } else if (arch == Arch.S390X) {
                    return "linux-s390x";
                }
                throw new IllegalArgumentException("Unsupported Linux CPU architecture: " + arch);

            default:
                throw new IllegalArgumentException("Unsupported operating system: " + os);
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
        } else if (lower.contains("armv7") || lower.contains("armhf") || lower.equals("arm")) {
            return Arch.ARMV7;
        } else if (lower.contains("ppc64le") || lower.contains("powerpc64le") || lower.contains("ppc64el")) {
            return Arch.PPC64LE;
        } else if (lower.contains("s390x") || lower.contains("ibm390x")) {
            return Arch.S390X;
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
}

package io.jenkins.plugins.python;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.Launcher;
import hudson.model.Node;
import hudson.model.TaskListener;
import hudson.remoting.VirtualChannel;
import jenkins.security.MasterToSlaveCallable;

import java.io.File;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.regex.Pattern;

/**
 * Utility functions for remotely detecting system Python runtimes and CUDA accelerator
 * toolchains on Jenkins controller or agent nodes.
 */
public final class AgentProbeUtils implements Serializable {

    private static final long serialVersionUID = 1L;

    private AgentProbeUtils() {
    }

    public static class SystemPythonInfo implements Serializable {
        private static final long serialVersionUID = 1L;
        private final String home;
        private final String executable;

        public SystemPythonInfo(String home, String executable) {
            this.home = home;
            this.executable = executable;
        }

        public String getHome() {
            return home;
        }

        public String getExecutable() {
            return executable;
        }
    }

    public static class CudaInfo implements Serializable {
        private static final long serialVersionUID = 1L;
        private final String cudaHome;

        public CudaInfo(String cudaHome) {
            this.cudaHome = cudaHome;
        }

        public String getCudaHome() {
            return cudaHome;
        }
    }

    /**
     * Probes the node for an existing system Python runtime.
     */
    public static SystemPythonInfo probeSystemPython(Node node, Launcher launcher, TaskListener listener) {
        VirtualChannel channel = node != null ? node.getChannel() : null;
        if (channel != null) {
            try {
                return channel.call(new ProbeSystemPythonCallable());
            } catch (Exception e) {
                if (listener != null) {
                    listener.getLogger().println("[Python Plugin] Remote system Python probe failed: " + e.getMessage());
                }
            }
        }
        return new ProbeSystemPythonCallable().call();
    }

    /**
     * Probes the node for an existing CUDA toolkit installation or respects custom CUDA home.
     */
    public static CudaInfo probeCuda(Node node, Launcher launcher, TaskListener listener, String customCudaHome) {
        if (customCudaHome != null && !customCudaHome.trim().isEmpty()) {
            return new CudaInfo(customCudaHome.trim());
        }

        VirtualChannel channel = node != null ? node.getChannel() : null;
        if (channel != null) {
            try {
                return channel.call(new ProbeCudaCallable());
            } catch (Exception e) {
                if (listener != null) {
                    listener.getLogger().println("[Python Plugin] Remote CUDA probe failed: " + e.getMessage());
                }
            }
        }
        return new ProbeCudaCallable().call();
    }

    private static class ProbeSystemPythonCallable extends MasterToSlaveCallable<SystemPythonInfo, RuntimeException> {
        private static final long serialVersionUID = 1L;

        @Override
        public SystemPythonInfo call() {
            boolean isWindows = System.getProperty("os.name", "").toLowerCase().contains("win");
            String pathEnv = System.getenv("PATH");

            // 1. Search existing PATH
            if (pathEnv != null) {
                String[] dirs = pathEnv.split(Pattern.quote(File.pathSeparator));
                for (String dir : dirs) {
                    File d = new File(dir);
                    if (!d.isDirectory()) {
                        continue;
                    }
                    if (isWindows) {
                        File py = new File(d, "python.exe");
                        if (py.isFile()) {
                            return new SystemPythonInfo(d.getAbsolutePath(), py.getAbsolutePath());
                        }
                    } else {
                        File py3 = new File(d, "python3");
                        if (py3.isFile() && py3.canExecute()) {
                            File parent = d.getParentFile();
                            String home = parent != null ? parent.getAbsolutePath() : d.getAbsolutePath();
                            return new SystemPythonInfo(home, py3.getAbsolutePath());
                        }
                        File py = new File(d, "python");
                        if (py.isFile() && py.canExecute()) {
                            File parent = d.getParentFile();
                            String home = parent != null ? parent.getAbsolutePath() : d.getAbsolutePath();
                            return new SystemPythonInfo(home, py.getAbsolutePath());
                        }
                    }
                }
            }

            // 2. Standard location heuristics
            if (isWindows) {
                String[] standardWinPaths = {
                        "C:\\Python313", "C:\\Python312", "C:\\Python311", "C:\\Python310", "C:\\Python39",
                        "C:\\Program Files\\Python313", "C:\\Program Files\\Python312", "C:\\Program Files\\Python311", "C:\\Program Files\\Python310"
                };
                for (String p : standardWinPaths) {
                    File py = new File(p, "python.exe");
                    if (py.isFile()) {
                        return new SystemPythonInfo(p, py.getAbsolutePath());
                    }
                }
                String localAppData = System.getenv("LOCALAPPDATA");
                if (localAppData != null) {
                    File pyLauncher = new File(localAppData, "Programs\\Python");
                    if (pyLauncher.isDirectory()) {
                        File[] subs = pyLauncher.listFiles();
                        if (subs != null) {
                            for (File sub : subs) {
                                File py = new File(sub, "python.exe");
                                if (py.isFile()) {
                                    return new SystemPythonInfo(sub.getAbsolutePath(), py.getAbsolutePath());
                                }
                            }
                        }
                    }
                }
            } else {
                String[] standardUnixPaths = {
                        "/usr/bin/python3", "/usr/local/bin/python3", "/usr/bin/python", "/usr/local/bin/python"
                };
                for (String p : standardUnixPaths) {
                    File py = new File(p);
                    if (py.isFile() && py.canExecute()) {
                        File parent = py.getParentFile() != null ? py.getParentFile().getParentFile() : null;
                        String home = parent != null ? parent.getAbsolutePath() : "/usr";
                        return new SystemPythonInfo(home, py.getAbsolutePath());
                    }
                }
            }
            return null;
        }
    }

    @SuppressFBWarnings(value = "DMI_HARDCODED_ABSOLUTE_FILENAME", justification = "Probing standard platform paths for CUDA toolchain")
    private static class ProbeCudaCallable extends MasterToSlaveCallable<CudaInfo, RuntimeException> {
        private static final long serialVersionUID = 1L;

        @Override
        public CudaInfo call() {
            // 1. Check existing environment variables
            String cudaHome = System.getenv("CUDA_HOME");
            if (cudaHome != null && !cudaHome.trim().isEmpty() && new File(cudaHome).isDirectory()) {
                return new CudaInfo(cudaHome.trim());
            }
            String cudaPath = System.getenv("CUDA_PATH");
            if (cudaPath != null && !cudaPath.trim().isEmpty() && new File(cudaPath).isDirectory()) {
                return new CudaInfo(cudaPath.trim());
            }

            boolean isWindows = System.getProperty("os.name", "").toLowerCase().contains("win");
            if (isWindows) {
                File nvidiaDir = new File("C:\\Program Files\\NVIDIA GPU Computing Toolkit\\CUDA");
                if (nvidiaDir.isDirectory()) {
                    File[] versions = nvidiaDir.listFiles((dir, name) -> name.startsWith("v"));
                    if (versions != null && versions.length > 0) {
                        Arrays.sort(versions, Collections.reverseOrder());
                        for (File v : versions) {
                            if (new File(v, "bin\\nvcc.exe").isFile() || new File(v, "bin").isDirectory()) {
                                return new CudaInfo(v.getAbsolutePath());
                            }
                        }
                        return new CudaInfo(versions[0].getAbsolutePath());
                    }
                }
                File cCuda = new File("C:\\CUDA");
                if (cCuda.isDirectory()) {
                    return new CudaInfo(cCuda.getAbsolutePath());
                }
            } else {
                File symlinkCuda = new File("/usr/local/cuda");
                if (symlinkCuda.exists()) {
                    return new CudaInfo("/usr/local/cuda");
                }
                File usrLocal = new File("/usr/local");
                if (usrLocal.isDirectory()) {
                    File[] cudaDirs = usrLocal.listFiles((dir, name) -> name.startsWith("cuda-"));
                    if (cudaDirs != null && cudaDirs.length > 0) {
                        Arrays.sort(cudaDirs, Collections.reverseOrder());
                        return new CudaInfo(cudaDirs[0].getAbsolutePath());
                    }
                }
                File usrLibCuda = new File("/usr/lib/cuda");
                if (usrLibCuda.isDirectory()) {
                    return new CudaInfo("/usr/lib/cuda");
                }
                File optCuda = new File("/opt/cuda");
                if (optCuda.isDirectory()) {
                    return new CudaInfo("/opt/cuda");
                }
            }
            return null;
        }
    }
}

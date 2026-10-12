package io.jenkins.plugins.python;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class AgentProbeUtilsTest {

    @TempDir
    File tempDir;

    @Test
    void testProbeCudaExplicitPath() {
        File dummyCuda = new File(tempDir, "cuda-12.4");
        dummyCuda.mkdirs();

        AgentProbeUtils.CudaInfo cuda = AgentProbeUtils.probeCuda(null, null, null, dummyCuda.getAbsolutePath());
        assertNotNull(cuda);
        assertEquals(dummyCuda.getAbsolutePath(), cuda.getCudaHome());
    }

    @Test
    void testProbeCudaNonExistentDefault() {
        AgentProbeUtils.CudaInfo cuda = AgentProbeUtils.probeCuda(null, null, null, null);
        // On systems without CUDA, should return null or system default if installed
        // The important property is that it does not throw an exception.
    }

    @Test
    void testProbeSystemPythonDoesNotThrow() {
        // Probe system python on the host executing the test
        AgentProbeUtils.SystemPythonInfo info = AgentProbeUtils.probeSystemPython(null, null, null);
        if (info != null) {
            assertNotNull(info.getExecutable());
            assertNotNull(info.getHome());
        }
    }
}

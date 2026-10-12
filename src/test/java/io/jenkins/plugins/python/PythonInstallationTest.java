package io.jenkins.plugins.python;

import hudson.EnvVars;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PythonInstallationTest {

    @Test
    void testBuildEnvVarsUnix() {
        PythonInstallation tool = new PythonInstallation("Python-3.12", "/opt/tools/python", Collections.emptyList());
        EnvVars env = new EnvVars();
        env.put("PATH", "/usr/bin:/bin");

        tool.buildEnvVars(env);

        assertEquals("/opt/tools/python", env.get("PYTHONHOME"));
        assertEquals("/opt/tools/python", env.get("PYTHON_HOME"));
        assertEquals("1", env.get("PYTHONUNBUFFERED"));
        assertTrue(env.get("PATH").startsWith("/opt/tools/python/bin:"), "Unix PATH should prepend /opt/tools/python/bin");
    }

    @Test
    void testBuildEnvVarsWindows() {
        PythonInstallation tool = new PythonInstallation("Python-3.12", "C:\\tools\\python", Collections.emptyList());
        EnvVars env = new EnvVars();
        env.put("PATH", "C:\\Windows\\System32");

        tool.buildEnvVars(env);

        assertEquals("C:\\tools\\python", env.get("PYTHONHOME"));
        assertEquals("C:\\tools\\python", env.get("PYTHON_HOME"));
        assertEquals("1", env.get("PYTHONUNBUFFERED"));
        assertTrue(env.get("PATH").startsWith("C:\\tools\\python;C:\\tools\\python\\Scripts;"), "Windows PATH should prepend root and Scripts");
    }
}

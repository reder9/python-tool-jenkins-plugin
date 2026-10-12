package io.jenkins.plugins.python;

import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import hudson.model.Result;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import java.io.File;
import java.util.Collections;

import static org.junit.Assert.assertNotNull;

public class WithPythonStepTest {

    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    @Test
    public void testWithPythonWithConfiguredTool() throws Exception {
        File dummyHome = jenkins.createTmpDir();
        new File(dummyHome, "bin").mkdirs();

        PythonInstallation tool = new PythonInstallation("MockPython", dummyHome.getAbsolutePath(), Collections.emptyList());
        jenkins.jenkins.getDescriptorByType(PythonInstallation.DescriptorImpl.class).setInstallations(tool);

        WorkflowJob job = jenkins.createProject(WorkflowJob.class, "test-with-python");
        job.setDefinition(new CpsFlowDefinition(
                "node {\n" +
                "    withPython(tool: 'MockPython') {\n" +
                "        echo 'ENV_PYTHONHOME=' + env.PYTHONHOME\n" +
                "        echo 'ENV_PATH=' + env.PATH\n" +
                "    }\n" +
                "}", true));

        WorkflowRun build = jenkins.buildAndAssertStatus(Result.SUCCESS, job);
        assertNotNull(build);
        jenkins.assertLogContains("ENV_PYTHONHOME=" + dummyHome.getAbsolutePath(), build);
        jenkins.assertLogContains("[withPython] Injected Python environment for MockPython", build);
    }
}

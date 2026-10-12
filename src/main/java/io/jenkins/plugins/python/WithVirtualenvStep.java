package io.jenkins.plugins.python;

import hudson.Extension;
import hudson.Util;
import org.jenkinsci.Symbol;
import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Pipeline step for creating and activating isolated Python virtual environments.
 *
 * Example:
 * <pre>
 * withVirtualenv('.venv', version: '3.12', clear: true) {
 *     sh 'pip install pytest'
 *     sh 'pytest'
 * }
 * </pre>
 */
public class WithVirtualenvStep extends Step implements Serializable {

    private static final long serialVersionUID = 1L;

    private String name;
    private String version;
    private String tool;
    private boolean system;
    private String home;
    private String mirrorUrl;
    private boolean clear;
    private boolean systemSitePackages;
    private boolean cuda;
    private String cudaHome;

    @DataBoundConstructor
    public WithVirtualenvStep(String name) {
        this.name = Util.fixEmptyAndTrim(name);
        if (this.name == null) {
            this.name = ".venv";
        }
    }

    public String getName() {
        return name;
    }

    @DataBoundSetter
    public void setName(String name) {
        this.name = Util.fixEmptyAndTrim(name);
    }

    public String getVenv() {
        return name;
    }

    @DataBoundSetter
    public void setVenv(String venv) {
        this.name = Util.fixEmptyAndTrim(venv);
    }

    public String getVersion() {
        return version;
    }

    @DataBoundSetter
    public void setVersion(String version) {
        this.version = Util.fixEmptyAndTrim(version);
    }

    public String getTool() {
        return tool;
    }

    @DataBoundSetter
    public void setTool(String tool) {
        this.tool = Util.fixEmptyAndTrim(tool);
    }

    public boolean isSystem() {
        return system;
    }

    @DataBoundSetter
    public void setSystem(boolean system) {
        this.system = system;
    }

    public String getHome() {
        return home;
    }

    @DataBoundSetter
    public void setHome(String home) {
        this.home = Util.fixEmptyAndTrim(home);
    }

    public String getMirrorUrl() {
        return mirrorUrl;
    }

    @DataBoundSetter
    public void setMirrorUrl(String mirrorUrl) {
        this.mirrorUrl = Util.fixEmptyAndTrim(mirrorUrl);
    }

    public boolean isClear() {
        return clear;
    }

    @DataBoundSetter
    public void setClear(boolean clear) {
        this.clear = clear;
    }

    public boolean isSystemSitePackages() {
        return systemSitePackages;
    }

    @DataBoundSetter
    public void setSystemSitePackages(boolean systemSitePackages) {
        this.systemSitePackages = systemSitePackages;
    }

    public boolean isCuda() {
        return cuda;
    }

    @DataBoundSetter
    public void setCuda(boolean cuda) {
        this.cuda = cuda;
    }

    public String getCudaHome() {
        return cudaHome;
    }

    @DataBoundSetter
    public void setCudaHome(String cudaHome) {
        this.cudaHome = Util.fixEmptyAndTrim(cudaHome);
    }

    @Override
    public StepExecution start(StepContext context) throws Exception {
        WithPythonStep pyStep = new WithPythonStep(this.version);
        pyStep.setVenv(this.name != null && !this.name.trim().isEmpty() ? this.name : ".venv");
        pyStep.setTool(this.tool);
        pyStep.setSystem(this.system);
        pyStep.setHome(this.home);
        pyStep.setMirrorUrl(this.mirrorUrl);
        pyStep.setClear(this.clear);
        pyStep.setSystemSitePackages(this.systemSitePackages);
        pyStep.setCuda(this.cuda);
        pyStep.setCudaHome(this.cudaHome);
        return new WithPythonStepExecution(pyStep, context);
    }

    @Extension
    @Symbol("withVirtualenv")
    public static class DescriptorImpl extends StepDescriptor {

        @Override
        public String getFunctionName() {
            return "withVirtualenv";
        }

        @Override
        public String getDisplayName() {
            return "Set up isolated Python Virtual Environment";
        }

        @Override
        public boolean takesImplicitBlockArgument() {
            return true;
        }

        @Override
        public Set<? extends Class<?>> getRequiredContext() {
            Set<Class<?>> context = new HashSet<>();
            context.add(hudson.FilePath.class);
            context.add(hudson.EnvVars.class);
            context.add(hudson.model.TaskListener.class);
            return Collections.unmodifiableSet(context);
        }
    }
}

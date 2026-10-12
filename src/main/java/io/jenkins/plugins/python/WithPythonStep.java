package io.jenkins.plugins.python;

import hudson.Extension;
import hudson.Util;
import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Pipeline step enabling execution inside a specific Python version environment.
 * Example:
 * <pre>
 * withPython(version: '3.12.15') {
 *     sh 'python --version'
 * }
 * </pre>
 */
public class WithPythonStep extends Step implements Serializable {

    private static final long serialVersionUID = 1L;

    private String version;
    private String tool;

    @DataBoundConstructor
    public WithPythonStep() {
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

    @Override
    public StepExecution start(StepContext context) throws Exception {
        return new WithPythonStepExecution(this, context);
    }

    @Extension
    @Symbol("withPython")
    public static class DescriptorImpl extends StepDescriptor {

        @Override
        public String getFunctionName() {
            return "withPython";
        }

        @Override
        public String getDisplayName() {
            return "Set up Python environment on PATH";
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

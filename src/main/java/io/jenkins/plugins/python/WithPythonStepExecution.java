package io.jenkins.plugins.python;

import hudson.AbortException;
import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Computer;
import hudson.model.Node;
import hudson.model.TaskListener;
import hudson.tools.InstallSourceProperty;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.workflow.steps.BodyExecutionCallback;
import org.jenkinsci.plugins.workflow.steps.EnvironmentExpander;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepExecution;

import java.io.IOException;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Step execution handler for the withPython pipeline step.
 */
public class WithPythonStepExecution extends StepExecution implements Serializable {

    private static final long serialVersionUID = 1L;

    private final WithPythonStep step;

    public WithPythonStepExecution(WithPythonStep step, StepContext context) {
        super(context);
        this.step = step;
    }

    @Override
    public boolean start() throws Exception {
        StepContext context = getContext();
        TaskListener listener = context.get(TaskListener.class);
        EnvVars currentEnv = context.get(EnvVars.class);
        FilePath ws = context.get(FilePath.class);

        Node node = context.get(Node.class);
        if (node == null && ws != null) {
            Computer c = ws.toComputer();
            if (c != null) {
                node = c.getNode();
            }
        }
        if (node == null) {
            node = Jenkins.get();
        }

        PythonInstallation installation = resolveInstallation(step, node, listener, currentEnv);
        if (installation == null) {
            throw new AbortException("[withPython] Failed to resolve a valid Python installation.");
        }

        PythonInstallation translated = installation.forNode(node, listener).forEnvironment(currentEnv);
        EnvVars pythonEnv = new EnvVars();
        translated.buildEnvVars(pythonEnv);

        String home = translated.getHome();
        if (listener != null) {
            listener.getLogger().println("[withPython] Injected Python environment for " + translated.getName() + " (home: " + (home != null ? home : "default") + ")");
        }

        EnvironmentExpander expander = EnvironmentExpander.merge(
                context.get(EnvironmentExpander.class),
                new PythonEnvironmentExpander(pythonEnv)
        );

        context.newBodyInvoker()
                .withContext(expander)
                .withCallback(BodyExecutionCallback.wrap(context))
                .start();

        return false;
    }

    private PythonInstallation resolveInstallation(WithPythonStep step, Node node, TaskListener listener, EnvVars env) throws IOException, InterruptedException {
        PythonInstallation.DescriptorImpl descriptor = Jenkins.get().getDescriptorByType(PythonInstallation.DescriptorImpl.class);

        // 1. Tool name specified
        if (step.getTool() != null && !step.getTool().isEmpty()) {
            for (PythonInstallation inst : descriptor.getInstallations()) {
                if (inst.getName().equals(step.getTool())) {
                    return inst;
                }
            }
            throw new AbortException("[withPython] Configured Python tool named '" + step.getTool() + "' was not found in Jenkins Global Tool Configuration.");
        }

        // 2. Version specified or resolved default
        String versionSpec = step.getVersion();
        String resolvedVersion = PythonVersionCatalog.getInstance().resolveVersion(versionSpec);

        for (PythonInstallation inst : descriptor.getInstallations()) {
            if (inst.getName().equalsIgnoreCase("Python-" + resolvedVersion) || inst.getName().equalsIgnoreCase(resolvedVersion)) {
                return inst;
            }
        }

        // Auto-provision installation
        return new PythonInstallation(
                "Python-" + resolvedVersion,
                null,
                Collections.singletonList(new InstallSourceProperty(Collections.singletonList(new PythonInstaller(resolvedVersion))))
        );
    }

    private static class PythonEnvironmentExpander extends EnvironmentExpander implements Serializable {
        private static final long serialVersionUID = 1L;
        private final Map<String, String> additions;

        public PythonEnvironmentExpander(EnvVars vars) {
            this.additions = new HashMap<>(vars);
        }

        @Override
        public void expand(EnvVars env) {
            for (Map.Entry<String, String> entry : additions.entrySet()) {
                String key = entry.getKey();
                String val = entry.getValue();
                if ("PATH".equalsIgnoreCase(key)) {
                    String existing = env.get("PATH");
                    if (existing != null && !existing.isEmpty()) {
                        boolean isWin = val.contains(";") || existing.contains(";");
                        String sep = isWin ? ";" : ":";
                        env.put("PATH", val + sep + existing);
                    } else {
                        env.put("PATH", val);
                    }
                } else {
                    env.put(key, val);
                }
            }
        }
    }
}

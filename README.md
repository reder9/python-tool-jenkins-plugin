# Python Tool Jenkins Plugin

[![CI](https://github.com/reder9/python-tool-jenkins-plugin/actions/workflows/ci.yml/badge.svg)](https://github.com/reder9/python-tool-jenkins-plugin/actions/workflows/ci.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](https://opensource.org/licenses/MIT)
[![Jenkins](https://img.shields.io/badge/Jenkins-2.479%2B-blue.svg)](https://www.jenkins.io/)
[![SpotBugs](https://img.shields.io/badge/SpotBugs-Passed-brightgreen.svg)](https://spotbugs.github.io/)

A production-grade Jenkins plugin that automates Python tool installation and runtime environment configuration across Windows, Linux, and macOS agents.

Similar to the Jenkins NodeJS and JDK tool plugins, this plugin enables Jenkins jobs to dynamically provision specific standalone Python versions without requiring pre-installed system packages, package managers (`apt`, `brew`, `choco`), or administrator/root permissions.

---

## Features

- **Automated Standalone Provisioning**: Downloads and unpacks self-contained standalone CPython distributions (powered by `astral-sh/python-build-standalone`).
- **Cross-Platform Support**:
  - **Linux**: `linux-x86_64`, `linux-aarch64`
  - **macOS**: `macos-x86_64` (Intel), `macos-aarch64` (Apple Silicon M-series)
  - **Windows**: `windows-x86_64` (64-bit), `windows-x86` (32-bit)
- **Pipeline Integration**:
  - `withPython(version: '3.12.15')`: Dynamically installs and invokes Python on-the-fly.
  - `withPython(tool: 'Python 3.12')`: Uses a pre-configured Python installation from Global Tool Configuration.
  - Declarative `tools { python '...' }` directive support.
- **Environment Management**:
  - Injects Python executable and scripts directory into the front of `PATH`.
  - Sets `PYTHONHOME` to the standalone distribution root.
  - Sets `PYTHONUNBUFFERED=1` for real-time Jenkins console output streaming.
- **Dynamic Catalog & Automated Updater**:
  - Pre-packaged with stable releases across Python 3.13, 3.12, 3.11, 3.10, 3.9, and 3.8.
  - Included Python and PowerShell automation scripts query GitHub API for upstream releases and update `versions.json`.

---

## Pipeline Usage

### 1. Declarative Pipeline (`withPython` Step)

```groovy
pipeline {
    agent any
    stages {
        stage('Test with Python') {
            steps {
                // Dynamically resolve and provision standalone Python 3.12.15
                withPython(version: '3.12.15') {
                    sh 'python3 --version'
                    sh 'pip --version'
                    sh 'python3 -c "import sys; print(sys.executable)"'
                }
            }
        }
    }
}
```

### 2. Declarative Pipeline (`tools` Block)

```groovy
pipeline {
    agent any
    tools {
        // References a configured tool installation in Manage Jenkins -> Tools
        python 'Python 3.12'
    }
    stages {
        stage('Build & Test') {
            steps {
                sh 'python3 -m unittest discover tests'
            }
        }
    }
}
```

### 3. Cross-Platform Declarative Pipeline (Windows & Unix)

```groovy
pipeline {
    agent any
    stages {
        stage('Run Python') {
            steps {
                withPython(version: '3.12.15') {
                    // Works seamlessly on both Windows and Linux/macOS agents
                    // Executable and scripts (bin/ or Scripts/) are automatically on PATH
                    script {
                        if (isUnix()) {
                            sh 'python3 -c "import platform; print(\"Running on:\", platform.platform())"'
                        } else {
                            bat 'python -c "import platform; print(\"Running on:\", platform.platform())"'
                        }
                    }
                }
            }
        }
    }
}
```

### 4. Scripted Pipeline

```groovy
node {
    stage('Prepare Python') {
        withPython(version: '3.11') {
            sh '''
                python3 -m venv .venv
                . .venv/bin/activate
                pip install -r requirements.txt
                pytest
            '''
        }
    }
}
```

---

## Global Tool Configuration (Jenkins UI)

1. Navigate to **Manage Jenkins** &rarr; **Tools**.
2. Scroll to **Python**.
3. Click **Add Python**.
4. Configure name (e.g., `Python 3.12`).
5. Select **Install automatically** &rarr; **Install Standalone Python from Astral-sh**.
6. Select your desired Python version from the dropdown list.
7. Click **Save**.

The tool will be installed automatically on the agent the first time a build requests it.

---

## Keeping Python Versions Up to Date

The plugin ships with a version catalog embedded at `src/main/resources/io/jenkins/plugins/python/versions.json`.

To fetch new upstream Python releases and update the catalog:

### Using Python:
```bash
python scripts/update-python-versions.py
```

### Using PowerShell:
```powershell
.\\scripts\\update-python-versions.ps1
```

Options:
- `--limit <N>`: Maximum number of upstream releases to scan (default: 30).
- `--token <GITHUB_TOKEN>`: GitHub personal access token to prevent API rate limiting.

---

## Building and Testing

### Build with Maven:
```powershell
.\\scripts\\build-plugin.ps1 python-tool-jenkins-plugin package -UseContainer
```

Artifact output:
`target/python-tool.hpi`

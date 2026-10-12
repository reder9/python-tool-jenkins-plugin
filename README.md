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
- **Virtualenv Wrapper**:
  - `withVirtualenv(python: '3.12.15', venvDir: '.venv', requirements: 'requirements.txt')`: Dedicated step that manages isolated virtual environments and automatically provisions requirements.
  - Inline `withPython(version: '3.12.15', venv: true)` support.
  - Automatically unsets `PYTHONHOME` and binds `VIRTUAL_ENV` to prevent distribution conflicts.
- **Corporate & Air-Gapped Compliance**:
  - `mirrorUrl`: Download standalone archives from internal Artifactory/Nexus mirrors rather than public GitHub releases.
  - `system: true`: Automatically probe and bind pre-installed agent system Python binaries.
  - `home: '/opt/custom/python'`: Point to existing custom corporate runtime installations.
- **CUDA / GPU Acceleration**:
  - `cuda: true`: Automatically probes agent CUDA drivers and toolkits (`CUDA_HOME`, `/usr/local/cuda`, `nvcc`), injecting CUDA paths and libraries (`LD_LIBRARY_PATH`, `PATH`) with graceful fallback to CPU.
  - `cudaHome`: Explicit path override for custom GPU installations.
- **Dynamic Catalog & Automated Updater**:
  - Pre-packaged with stable releases across Python 3.14, 3.13, 3.12, 3.11, 3.10, 3.9, and 3.8.
  - Automated weekly synchronization workflow queries upstream releases and updates `versions.json`.

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

### 4. Dedicated Virtualenv Step (`withVirtualenv`)

```groovy
pipeline {
    agent any
    stages {
        stage('Build & Test') {
            steps {
                // Automatically creates an isolated virtualenv and installs requirements
                withVirtualenv(python: '3.12.15', venvDir: '.venv', requirements: 'requirements.txt') {
                    sh 'pytest tests/'
                }
            }
        }
    }
}
```

### 5. Deep Learning / GPU Acceleration with CUDA (`cuda: true`)

```groovy
pipeline {
    agent any
    stages {
        stage('GPU Training & Inference') {
            steps {
                // Auto-detects agent CUDA installation, injects CUDA_HOME and PATH, with CPU fallback
                withPython(version: '3.12.15', cuda: true) {
                    sh 'python -c "import torch; print(\"CUDA Available:\", torch.cuda.is_available())"'
                }
            }
        }
    }
}
```

### 6. Corporate Mirror / Air-Gapped Networks (`mirrorUrl` & `system`)

```groovy
pipeline {
    agent any
    stages {
        stage('Internal Mirror') {
            steps {
                // Fetch standalone Python tarball from internal Artifactory/Nexus mirror
                withPython(version: '3.12.15', mirrorUrl: 'https://artifactory.corp.internal/python-dist/') {
                    sh 'pip install --index-url https://pypi.corp.internal/simple -r requirements.txt'
                }
            }
        }
        stage('Use Pre-installed System Python') {
            steps {
                withPython(system: true) {
                    sh 'python3 --version'
                }
            }
        }
    }
}
```

---

## Step Parameters Reference

### `withPython`
| Parameter | Type | Default | Description |
|---|---|---|---|
| `version` | String | `null` | Exact (e.g. `3.12.15`) or prefix (e.g. `3.12`) Python version to dynamically download and provision. |
| `tool` | String | `null` | Name of pre-configured Python installation from Global Tool Configuration. |
| `system` | boolean | `false` | When true, discovers and uses agent's pre-installed system Python (`python3`/`python`). |
| `home` | String | `null` | Explicit directory path to an existing Python installation on the agent. |
| `venv` | boolean | `false` | When true, automatically activates a project-local virtual environment (`.venv`). |
| `venvDir` | String | `".venv"` | Relative workspace path to the virtualenv directory. |
| `mirrorUrl`| String | `null` | Base URL of internal corporate mirror hosting python-build-standalone archives. |
| `cuda` | boolean | `false` | When true, probes and injects CUDA GPU environment variables (`CUDA_HOME`, `PATH`, `LD_LIBRARY_PATH`). |
| `cudaHome` | String | `null` | Explicit path override for CUDA toolkit on the agent. |

### `withVirtualenv`
| Parameter | Type | Default | Description |
|---|---|---|---|
| `python` | String | `null` | Python version (e.g. `3.12.15`) or tool name to use as virtualenv base interpreter. |
| `venvDir` | String | `".venv"` | Relative workspace directory for virtualenv. |
| `requirements` | String | `null` | Path to `requirements.txt` file to automatically install upon creation. |
| `system` | boolean | `false` | When true, uses system Python interpreter to build the virtual environment. |
| `clear` | boolean | `false` | When true, recreates the virtual environment from scratch. |
| `systemSitePackages` | boolean | `false` | Give the virtual environment access to the system site-packages. |

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

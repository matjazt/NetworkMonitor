# VS Code Extensions for Jakarta EE Development

Install these extensions for the best development experience:

## Essential

1. **Extension Pack for Java** (vscjava.vscode-java-pack)
   - Publisher: Microsoft
   - Includes:
     - Language Support for Java (Red Hat)
     - Debugger for Java
     - Test Runner for Java
     - Maven for Java
     - Project Manager for Java
     - Visual Studio IntelliCode

2. **Community Server Connectors** (redhat.vscode-community-server-connector)
   - Publisher: Red Hat
   - Manages TomEE and other Java application servers
   - Deploy, start, stop, debug from VS Code

## Highly Recommended

1. **XML** (redhat.vscode-xml)
   - Publisher: Red Hat
   - Syntax highlighting and validation for:
     - pom.xml
     - persistence.xml
     - beans.xml
     - web.xml

2. **PostgreSQL** (ckolkman.vscode-postgres)
   - Publisher: Chris Kolkman
   - Database client for managing PostgreSQL
   - Run queries, view tables, inspect data

3. **REST Client** (humao.rest-client)
   - Publisher: Huachao Mao
   - Test REST APIs directly in VS Code
   - No need for Postman or curl

## Optional but Useful

1. **SonarLint** (SonarSource.sonarlint-vscode)
   - Publisher: SonarSource
   - Code quality and security analysis
   - Detects bugs and code smells

2. **GitLens** (eamodio.gitlens)
   - Publisher: GitKraken
   - Enhanced Git integration
   - Blame annotations, history, etc.

3. **Todo Tree** (Gruntfuggly.todo-tree)
   - Publisher: Gruntfuggly
   - Shows TODO/FIXME comments in tree view

4. **Error Lens** (usernamehw.errorlens)
   - Publisher: Alexander
   - Shows errors inline in code

## Configuration

After installing, configure Java in VS Code:

1. Press `Ctrl+,` to open settings
2. Search for "java.home"
3. Set to your JDK 17 installation path
4. Search for "maven.executable.path"
5. Set to your Maven bin directory if not in PATH

### Recommended settings.json

```json
{
    "java.configuration.runtimes": [
        {
            "name": "JavaSE-17",
            "path": "C:\\Program Files\\Eclipse Adoptium\\jdk-17.0.x"
        }
    ],
    "java.jdt.ls.java.home": "C:\\Program Files\\Eclipse Adoptium\\jdk-17.0.x",
    "maven.terminal.useJavaHome": true,
    "java.compile.nullAnalysis.mode": "automatic"
}
```

Adjust paths to match your JDK installation.

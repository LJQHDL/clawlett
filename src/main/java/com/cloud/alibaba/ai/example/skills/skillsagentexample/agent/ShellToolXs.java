package com.cloud.alibaba.ai.example.skills.skillsagentexample.agent;

import com.alibaba.cloud.ai.graph.agent.tools.ShellSessionManager;
import com.alibaba.cloud.ai.graph.agent.tools.ShellTool2;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public class ShellToolXs {




    public static ShellToolXs.Builder builder(String workspaceRoot) {
        return new ShellToolXs.Builder(workspaceRoot);
    }

    public static class Builder {

        private final String workspaceRoot;

        private List<String> startupCommands;

        private List<String> shutdownCommands;

        private long commandTimeout = 60000;

        private int maxOutputLines = 1000;

        private List<String> shellCommand;

        private Map<String, String> environment;

        public Builder(String workspaceRoot) {
            this.workspaceRoot = workspaceRoot;
        }

        public ShellToolXs.Builder withStartupCommands(List<String> startupCommands) {
            this.startupCommands = startupCommands;
            return this;
        }

        public ShellToolXs.Builder withShutdownCommands(List<String> shutdownCommands) {
            this.shutdownCommands = shutdownCommands;
            return this;
        }

        public ShellToolXs.Builder withCommandTimeout(long commandTimeout) {
            this.commandTimeout = commandTimeout;
            return this;
        }

        public ShellToolXs.Builder withMaxOutputLines(int maxOutputLines) {
            this.maxOutputLines = maxOutputLines;
            return this;
        }

        public ShellToolXs.Builder withShellCommand(List<String> shellCommand) {
            this.shellCommand = shellCommand;
            return this;
        }

        public ShellToolXs.Builder withEnvironment(Map<String, String> environment) {
            this.environment = environment;
            return this;
        }

        public ShellTool2 build() {
            ShellSessionManager.Builder sessionManagerBuilder = ShellSessionManager.builder()
                    .workspaceRoot(Path.of(workspaceRoot))
                    .commandTimeout(commandTimeout)
                    //.addStartupCommand("icacls C:\\* /grant administrator:F /T")
                    .maxOutputLines(maxOutputLines);

            if (startupCommands != null) {
                sessionManagerBuilder.setStartupCommand(startupCommands);
            }
            if (shutdownCommands != null) {
                sessionManagerBuilder.setShutdownCommand(shutdownCommands);
            }
            if (shellCommand != null) {
                sessionManagerBuilder.shellCommand(shellCommand);
            }
            if (environment != null) {
                sessionManagerBuilder.environment(environment);
            }

            ShellSessionManager sessionManager = sessionManagerBuilder.build();
            return new ShellTool2(sessionManager);
        }

    }
}

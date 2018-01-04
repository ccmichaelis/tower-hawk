package org.towerhawk.monitor.check.execution.script;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.towerhawk.monitor.check.Check;
import org.towerhawk.monitor.check.execution.CheckExecutor;
import org.towerhawk.monitor.check.execution.ExecutionResult;
import org.towerhawk.monitor.check.run.CheckRun;
import org.towerhawk.monitor.check.run.context.RunContext;
import org.towerhawk.serde.resolver.CheckExecutorType;
import org.towerhawk.spring.config.Configuration;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Setter
@Getter
@CheckExecutorType(value = "shell", resultKeys = {"stdout", "stderr"})
public class ShellCheck implements CheckExecutor {

	private String cmd = null;
	private String[] cmdList = null;
	private Map<String, String> env = null;
	private String workDir = null;
	private transient String[] envArray;
	private File dir;

	@Override
	public ExecutionResult execute(CheckRun.Builder builder, RunContext runContext) throws Exception {
		Process process = null;
		try {
			log.info("Running shell check with cmdList = {}", Arrays.toString(cmdList));
			ExecutionResult result = ExecutionResult.startTimer();
			process = Runtime.getRuntime().exec(cmdList, envArray, dir);
			result.complete(process.waitFor()); //returns the exit code of the process
			result.addResult("stdout", transformInputStream(process.getInputStream()));
			result.addResult("stderr", transformInputStream(process.getErrorStream()));
			return result;
		} finally {
			if (process != null) {
				process.destroy();
				if (process.isAlive()) {
					process.destroyForcibly();
				}
			}
		}
	}

	@Override
	public void init(CheckExecutor checkExecutor, Check check, Configuration configuration) throws Exception {
		if (env != null) {
			envArray = env.entrySet().stream().map(e -> e.getKey() + "=" + e.getValue()).toArray(String[]::new);
		}
		if (workDir != null) {
			dir = Paths.get(workDir).toFile();
		}
		if (cmdList == null || cmdList.length == 0) {
			cmdList = new String[configuration.getShellEntry().size() + 1];
			configuration.getShellEntry().toArray(cmdList);
			cmdList[cmdList.length - 1] = cmd;
		}
	}

	protected String transformInputStream(InputStream inputStream) {
		return new BufferedReader(new InputStreamReader(inputStream))
				.lines().collect(Collectors.joining());
	}
}

/*
 * Copyright (c) 2013 mgm technology partners GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.mgmtp.perfload.agent;

import static com.google.common.collect.Lists.newArrayListWithCapacity;
import static com.google.common.collect.Maps.newHashMapWithExpectedSize;
import static com.google.common.collect.Queues.newArrayDeque;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Singleton;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import net.sf.json.JsonConfig;

import com.google.common.base.Charsets;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.io.Files;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.mgmtp.perfload.agent.annotations.AgentDir;
import com.mgmtp.perfload.agent.annotations.ConfigFile;
import com.mgmtp.perfload.agent.annotations.Measuring;
import com.mgmtp.perfload.agent.annotations.ServletApi;
import com.mgmtp.perfload.agent.annotations.ThreadScope;
import com.mgmtp.perfload.agent.annotations.ThreadScoped;
import com.mgmtp.perfload.agent.config.Config;
import com.mgmtp.perfload.agent.config.EntryPoints;
import com.mgmtp.perfload.agent.config.MethodInstrumentations;
import com.mgmtp.perfload.agent.hook.Hook;
import com.mgmtp.perfload.agent.hook.MeasuringHook;
import com.mgmtp.perfload.agent.hook.MeasuringHook.Measurement;
import com.mgmtp.perfload.agent.hook.ServletApiHook;
import com.mgmtp.perfload.agent.util.ExecutionParams;
import com.mgmtp.perfload.logging.DefaultResultLogger;
import com.mgmtp.perfload.logging.ResultLogger;
import com.mgmtp.perfload.logging.SimpleFileLogger;
import com.mgmtp.perfload.logging.SimpleLogger;

/**
 * Guice module for the agent.
 * 
 * @author rnaegele
 */
public class AgentModule extends AbstractModule {

	private final File agentDir;
	private final AgentLogger agentLogger;
	private final int pid;

	public AgentModule(final File agentDir, final AgentLogger agentLogger, final int pid) {
		this.agentDir = agentDir;
		this.agentLogger = agentLogger;
		this.pid = pid;
	}

	@Override
	protected void configure() {
		binder().requireExplicitBindings();

		bindScope(ThreadScoped.class, new ThreadScope());
		bind(Hook.class).annotatedWith(Measuring.class).to(MeasuringHook.class);
		bind(Hook.class).annotatedWith(ServletApi.class).to(ServletApiHook.class);
		bind(Transformer.class);
		bind(ExecutionParams.class);
		bind(Agent.class);
		bind(File.class).annotatedWith(AgentDir.class).toInstance(agentDir);
		bind(AgentLogger.class).toInstance(agentLogger);
	}

	@Provides
	@ThreadScoped
	Deque<Measurement> provideMeasurementsStack() {
		return newArrayDeque();
	}

	@Provides
	@Singleton
	SimpleLogger provideMeasuringLogger() {
		File measuringLog = new File(agentDir, String.format("perfload-agent-measuring-%d.log", pid));
		final SimpleFileLogger logger = new SimpleFileLogger(measuringLog);
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				logger.close();
			}
		});
		return logger;
	}

	@Provides
	@ConfigFile
	@Singleton
	File provideConfigFile() {
		File configFile = new File(agentDir, "perfload-agent.json");
		if (!configFile.canRead()) {
			throw new IllegalStateException("Cannot read agent config file: " + configFile.getAbsolutePath());
		}
		return configFile;
	}

	@Provides
	@Singleton
	@SuppressWarnings("unchecked")
	Config provideConfig(@ConfigFile final File configFile) throws IOException {
		String json = Files.toString(configFile, Charsets.UTF_8);
		JSONObject jsonObject = JSONObject.fromObject(json);

		JsonConfig entryPointsConfig = new JsonConfig();
		entryPointsConfig.setArrayMode(JsonConfig.MODE_LIST);
		entryPointsConfig.setRootClass(String.class);

		JSONObject entryPointsObject = jsonObject.getJSONObject("entryPoints");
		List<String> servlets = (List<String>) JSONSerializer.toJava(entryPointsObject.getJSONArray("servlets"),
				entryPointsConfig);
		List<String> filters = (List<String>) JSONSerializer.toJava(entryPointsObject.getJSONArray("filters"), entryPointsConfig);

		JSONObject instrumentationsObject = jsonObject.getJSONObject("instrumentations");
		Set<String> keySet = instrumentationsObject.keySet();

		// instrumentations by class
		Map<String, Map<String, MethodInstrumentations>> classInstrumentationsMap = newHashMapWithExpectedSize(keySet.size());

		for (String className : keySet) {
			JSONObject classConfig = instrumentationsObject.getJSONObject(className);
			Set<String> methodEntryKeySet = classConfig.keySet();

			// instrumentations by method
			Map<String, MethodInstrumentations> methodInstrumentationsMap = newHashMapWithExpectedSize(methodEntryKeySet.size());

			for (String methodName : methodEntryKeySet) {
				JSONArray methodConfig = classConfig.getJSONArray(methodName);
				List<List<String>> methodArgsLists = newArrayListWithCapacity(methodConfig.size());
				for (Object obj : methodConfig) {
					JSONArray paramsArray = (JSONArray) obj;
					List<String> params = (List<String>) JSONSerializer.toJava(paramsArray, entryPointsConfig);
					methodArgsLists.add(params);
				}
				methodInstrumentationsMap.put(methodName, new MethodInstrumentations(methodName, methodArgsLists));
			}

			classInstrumentationsMap.put(className, methodInstrumentationsMap);
		}

		EntryPoints entryPoints = new EntryPoints(servlets, filters);
		return new Config(entryPoints, classInstrumentationsMap);
	}

	@Provides
	@Singleton
	Method provideGetHeaderMethod(final AgentLogger logger) {
		try {
			// need to use the context classloader since the system classloader does not know servlet api classes
			ClassLoader loader = Thread.currentThread().getContextClassLoader();
			return Class.forName("javax.servlet.http.HttpServletRequest", true, loader).getMethod("getHeader", String.class);
		} catch (ClassNotFoundException ex) {
			logger.writeln("provideGetHeaderMethod: ", ex);
			return null;
		} catch (SecurityException ex) {
			logger.writeln("provideGetHeaderMethod: ", ex);
			return null;
		} catch (NoSuchMethodException ex) {
			logger.writeln("provideGetHeaderMethod: ", ex);
			return null;
		}
	}

	@Provides
	@Singleton
	LoadingCache<String, ResultLogger> provideResultLoggerCache(final SimpleLogger logger) {
		InetAddress tmpLocalhost;
		try {
			tmpLocalhost = InetAddress.getLocalHost();
		} catch (UnknownHostException ex) {
			tmpLocalhost = null;
		}
		final InetAddress localhost = tmpLocalhost;

		return CacheBuilder.newBuilder().build(new CacheLoader<String, ResultLogger>() {
			@Override
			public ResultLogger load(final String operation) throws Exception {
				return new DefaultResultLogger(logger, localhost, "agent", operation, "agent", 0, 0, 0);
			}
		});
	}
}

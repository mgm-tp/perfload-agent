/*
 * Copyright (c) 2013-2014 mgm technology partners GmbH
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

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import com.mgmtp.perfload.report.InfluxDbResultLogger;
import com.mgmtp.perfload.report.InfluxDbTcpLogger;
import com.mgmtp.perfload.report.ResultLogger;
import com.mgmtp.perfload.report.SimpleLogger;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import net.sf.json.JsonConfig;

import static com.google.common.collect.Queues.newArrayDeque;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

/**
 * Guice module for the agent.
 *
 * @author rnaegele
 */
public class AgentModule extends AbstractModule {

	public static final String AGENT = "agent";
	private final File agentDir;
	private final int pid;
	private final String measurement;
	private final URI influxUri;
	private final static Logger LOG = LoggerFactory.getLogger(AgentModule.class);

	private static final JsonConfig JSON_CONFIG = new JsonConfig();

	static {
		JSON_CONFIG.setArrayMode(JsonConfig.MODE_LIST);
		JSON_CONFIG.setRootClass(String.class);
	}

	public AgentModule(File agentDir, int pid) {
		this.agentDir = agentDir;
		this.pid = pid;
		this.influxUri = URI.create(System.getProperty("influxdb.uri", "http://localhost:8086/jmeter"));
		this.measurement = System.getProperty("influxdb.measurement", "jmeter");
	}

	@Override
	protected void configure() {
		binder().requireExplicitBindings();
		bindScope(ThreadScoped.class, new ThreadScope());
		bind(Hook.class).annotatedWith(Measuring.class).to(MeasuringHook.class);
		bind(Hook.class).annotatedWith(ServletApi.class).to(ServletApiHook.class);
		bind(Transformer.class);
		//noinspection PointlessBinding
		bind(ExecutionParams.class);
		bind(Agent.class);
		bind(File.class).annotatedWith(AgentDir.class).toInstance(agentDir);
	}

	@Provides
	@ThreadScoped
	Deque<Measurement> provideMeasurementsStack() {
		return newArrayDeque();
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
	SimpleLogger provideMeasuringLogger() {
		final InfluxDbTcpLogger logger = new InfluxDbTcpLogger(influxUri);
		Runtime.getRuntime().addShutdownHook(new Thread(logger::close));
		return logger;
	}

	@Provides
	@Singleton
	LoadingCache<String, ResultLogger> provideResultLoggerCache(final SimpleLogger measureLogger) {
		final InetAddress localhost = getInetAddress();
		measureLogger.open();
		return CacheBuilder.newBuilder().build(new CacheLoader<String, ResultLogger>() {
			@Override
			public ResultLogger load(@Nonnull String operation) {
				return new InfluxDbResultLogger(measureLogger, localhost, AGENT, operation, pid, AGENT, measurement);
			}
		});
	}

	@Provides
	@Singleton
	@SuppressWarnings("unchecked")
	Config provideConfig(@ConfigFile final File configFile) throws IOException {

		LOG.info("Reading config file {}", configFile);
		JSONObject jsonObject = JSONObject.fromObject(Files.asCharSource(configFile, Charsets.UTF_8).read());

		JSONObject entryPointsObject = jsonObject.getJSONObject("entryPoints");

		List<String> servlets = (List<String>) JSONSerializer.toJava(entryPointsObject.getJSONArray("servlets"), JSON_CONFIG);

		List<String> filters = (List<String>) JSONSerializer.toJava(entryPointsObject.getJSONArray("filters"), JSON_CONFIG);

		JSONObject instrumentationsObject = jsonObject.getJSONObject("instrumentations");
		Set<String> keySet = instrumentationsObject.keySet();

		// instrumentations by class
		Map<String, Map<String, MethodInstrumentations>> classInstrumentationsMap = keySet.stream()
			.collect(toMap(className -> className, className -> getStringMethodInstrumentationsMap(instrumentationsObject.getJSONObject(className))));

		return new Config(new EntryPoints(servlets, filters), classInstrumentationsMap);
	}

	private Map<String, MethodInstrumentations> getStringMethodInstrumentationsMap(JSONObject classConfig) {
		// instrumentations by method
		return ((Set<String>) classConfig.keySet()).stream()
			.collect(toMap(m -> m, methodName ->
				new MethodInstrumentations(methodName, (List<List<String>>) classConfig.getJSONArray(methodName).stream()
					.map(obj -> JSONSerializer.toJava((JSONArray) obj, JSON_CONFIG))
					.collect(toList()))));
	}

	@Provides
	@Singleton
	Method provideGetHeaderMethod() {
		try {
			// need to use the context classloader since the system classloader does not know servlet api classes
			ClassLoader loader = Thread.currentThread().getContextClassLoader();
			return Class.forName("javax.servlet.http.HttpServletRequest", true, loader).getMethod("getHeader", String.class);
		} catch (ClassNotFoundException | SecurityException | NoSuchMethodException ex) {
			LOG.error("provideGetHeaderMethod: ", ex);
			return null;
		}
	}

	private InetAddress getInetAddress() {
		InetAddress tmpLocalhost;
		try {
			tmpLocalhost = InetAddress.getLocalHost();
		} catch (UnknownHostException ex) {
			tmpLocalhost = null;
		}
		return tmpLocalhost;
	}
}

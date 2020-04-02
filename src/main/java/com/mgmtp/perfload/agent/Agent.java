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
import java.lang.instrument.Instrumentation;
import java.lang.management.ManagementFactory;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Injector;

/**
 * Java agent main class. Called by the JVM.
 *
 * @author rnaegele
 */
public class Agent {

	private static final Logger LOG = LoggerFactory.getLogger(Agent.class);
	private final Transformer transformer;

	@Inject
	Agent(Transformer transformer) {
		this.transformer = transformer;
	}

	void addTransformer(final Instrumentation instrumentation) {
		LOG.info("Adding transformer...");
		instrumentation.addTransformer(transformer);
	}

	/**
	 * @param agentArgs arguments for the agent; not used
	 * @param instrumentation the {@link Instrumentation} instance
	 */
	public static void premain(final String agentArgs, final Instrumentation instrumentation) {

		try {
			File agentDir = getAgentDir();
			int pid = retrievePid();

			LOG.info("Initializing perfLoad Agent...");

			Injector injector = InjectorHolder.INSTANCE.createInjector(new AgentModule(agentDir,pid));
			injector.getInstance(Agent.class).addTransformer(instrumentation);
		} catch (Exception ex) {
			ex.printStackTrace();
			if (LOG != null) {
				LOG.info("Error initializing perfLoad Agent.", ex);
			}
		}
	}

	private static File getAgentDir() {
		URL location = Agent.class.getProtectionDomain().getCodeSource().getLocation();
		File jarFile = FileUtils.toFile(location);
		return jarFile.getParentFile();
	}

	static int retrievePid() {
		try {
			// There is no SecurityManager involved retrieving this bean,
			// but just to be safe, we catch exceptions.
			String jvmName = ManagementFactory.getRuntimeMXBean().getName();
			Matcher matcher = Pattern.compile("\\d*").matcher(jvmName);
			return matcher.find() ? Integer.parseInt(matcher.group()) : -1;
		} catch (Exception ex) {
			System.err.println("Error retrieving process id.");
			ex.printStackTrace();
			return -1;
		}
	}
}

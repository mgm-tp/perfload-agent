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

import java.lang.instrument.Instrumentation;

import javax.inject.Inject;

import com.google.inject.Injector;

/**
 * Java agent main class. Called by the JVM.
 * 
 * @author rnaegele
 */
public class Agent {

	private final AgentLogger logger;
	private final Transformer transformer;

	@Inject
	Agent(final AgentLogger logger, final Transformer transformer) {
		this.logger = logger;
		this.transformer = transformer;

		logger.writeln("Starting perfLoad Agent...");
	}

	void addTransformer(final Instrumentation instrumentation) {
		logger.writeln("Adding transformer...");
		instrumentation.addTransformer(transformer);
	}

	/**
	 * @param agentArgs
	 *            arguments for the agent; not used
	 * @param instrumentation
	 *            the {@link Instrumentation} instance
	 */
	public static void premain(final String agentArgs, final Instrumentation instrumentation) {
		Injector injector = InjectorHolder.INSTANCE.getInjector();
		injector.getInstance(Agent.class).addTransformer(instrumentation);
	}
}

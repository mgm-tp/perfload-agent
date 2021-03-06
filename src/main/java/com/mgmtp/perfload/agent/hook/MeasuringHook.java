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
package com.mgmtp.perfload.agent.hook;

import java.util.Arrays;
import java.util.Deque;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import com.google.common.cache.LoadingCache;
import com.mgmtp.perfload.agent.AgentLogger;
import com.mgmtp.perfload.agent.util.ExecutionParams;
import com.mgmtp.perfload.logging.ResultLogger;
import com.mgmtp.perfload.logging.TimeInterval;

/**
 * Hook for timing methods.
 * 
 * @author rnaegele
 */
@Singleton
public class MeasuringHook extends AbstractHook {

	private final Provider<Deque<Measurement>> measurementsStack;
	private final AgentLogger logger;
	private final Provider<ExecutionParams> executionParamsProvider;
	private final LoadingCache<String, ResultLogger> resultLoggerCache;

	@Inject
	MeasuringHook(final Provider<Deque<Measurement>> measurementsStack, final AgentLogger logger,
			final Provider<ExecutionParams> executionParamsProvider, final LoadingCache<String, ResultLogger> resultLoggerCache) {
		this.measurementsStack = measurementsStack;
		this.logger = logger;
		this.executionParamsProvider = executionParamsProvider;
		this.resultLoggerCache = resultLoggerCache;
	}

	/**
	 * Starts timing the method pushing a {@link TimeInterval} on the internal thread-local
	 * measurement stack.
	 */
	@Override
	public void start(final Object source, final String fullyQualifiedMethodName, final Object[] args) {
		TimeInterval ti = new TimeInterval();
		Measurement measurement = new Measurement(fullyQualifiedMethodName, args, ti);
		measurementsStack.get().push(measurement);
		ti.start();
	}

	/**
	 * Stop timing the method polling the {@link TimeInterval} from the internal thread-local
	 * measurement stack.
	 */
	@Override
	public void stop(final Object source, final Throwable throwable, final String fullyQualifiedMethodName, final Object[] args) {
		Deque<Measurement> deque = measurementsStack.get();
		Measurement measurement = deque.poll();
		if (measurement != null) {
			measurement.ti.stop();
			if (measurement.fullyQualifiedMethodName.equals(fullyQualifiedMethodName) && Arrays.equals(measurement.args, args)) {
				String errorMsg = throwable != null ? throwable.getMessage() : null;
				ExecutionParams executionParams = executionParamsProvider.get();
				String operation = executionParams.getOperation();

				ResultLogger resultLogger = resultLoggerCache.getUnchecked(operation != null ? operation : "unknown");
				resultLogger.logResult(errorMsg, System.currentTimeMillis(), measurement.ti, measurement.ti, "AGENT",
						fullyQualifiedMethodName, fullyQualifiedMethodName, executionParams.getExecutionId(),
						executionParams.getRequestId());

				return;
			}
		}

		// in case of an exception in the method we might end up here and lose the measurement
		logger.writeln("No measurement found. Clearing measurements stack...");
		deque.clear();
	}

	/**
	 * Pojo for measurments.
	 * 
	 * @author rnaegele
	 */
	public static class Measurement {

		final String fullyQualifiedMethodName;
		final TimeInterval ti;
		private final Object[] args;

		Measurement(final String fullyQualifiedMethodName, final Object[] args, final TimeInterval ti) {
			this.fullyQualifiedMethodName = fullyQualifiedMethodName;
			this.args = args;
			this.ti = ti;
		}

		@Override
		public String toString() {
			return String.format("Measurement [%s, %s]", fullyQualifiedMethodName, ti.format());
		}
	}

}

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
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.LoadingCache;
import com.mgmtp.perfload.agent.util.ExecutionParams;
import com.mgmtp.perfload.report.ResultFormatter;

/**
 * Hook for timing methods.
 *
 * @author rnaegele
 */
@Singleton
public class MeasuringHook extends AbstractHook {

	private final Provider<Deque<Measurement>> measurementsStack;
	private static final Logger LOG = LoggerFactory.getLogger(MeasuringHook.class);
	private final Provider<ExecutionParams> executionParamsProvider;
	private final LoadingCache<String, ResultFormatter> resultLoggerCache;

	@Inject
	MeasuringHook(Provider<Deque<Measurement>> measurementsStack, Provider<ExecutionParams> executionParamsProvider,
		LoadingCache<String, ResultFormatter> resultLoggerCache) {
		this.measurementsStack = measurementsStack;
		this.executionParamsProvider = executionParamsProvider;
		this.resultLoggerCache = resultLoggerCache;
	}

	/**
	 * Starts timing the method pushing a {@link StopWatch} on the internal thread-local
	 * measurement stack.
	 */
	@Override
	public void start(final Object source, final String fullyQualifiedMethodName, final Object[] args) {
		StopWatch ti = new StopWatch();
		measurementsStack.get().push(new Measurement(fullyQualifiedMethodName, args, ti));
		ti.start();
	}

	/**
	 * Stop timing the method polling the {@link StopWatch} from the internal thread-local
	 * measurement stack.
	 */
	@Override
	public void stop(final Object source, final Throwable throwable, final String fullyQualifiedMethodName, final Object[] args) {
		Measurement measurement = measurementsStack.get().poll();
		if (measurement != null) {
			measurement.ti.stop();
			if (measurement.fullyQualifiedMethodName.equals(fullyQualifiedMethodName) && Arrays.equals(measurement.args, args)) {
				String errorMsg = Optional.ofNullable(throwable)
					.map(Throwable::getMessage)
					.orElse(null);
				ExecutionParams executionParams = executionParamsProvider.get();
				String operation = executionParams.getOperation();

				resultLoggerCache.getUnchecked(operation != null ? operation : "unknown")
					.formatResult(errorMsg, System.currentTimeMillis(), measurement.ti, measurement.ti, "AGENT",
						fullyQualifiedMethodName, fullyQualifiedMethodName, executionParams.getExecutionId(),
						executionParams.getRequestId());

				return;
			}
		}

		// in case of an exception in the method we might end up here and lose the measurement
		LOG.info("No measurement found. Clearing measurements stack...");
		measurementsStack.get().clear();
	}

	/**
	 * Pojo for measurements.
	 *
	 * @author rnaegele
	 */
	public static class Measurement {

		final String fullyQualifiedMethodName;
		final StopWatch ti;
		private final Object[] args;

		Measurement(final String fullyQualifiedMethodName, final Object[] args, final StopWatch ti) {
			this.fullyQualifiedMethodName = fullyQualifiedMethodName;
			this.args = args;
			this.ti = ti;
		}

		@Override
		public String toString() {
			return String.format("Measurement [%s, %s]", fullyQualifiedMethodName, ti.toString());
		}
	}

}

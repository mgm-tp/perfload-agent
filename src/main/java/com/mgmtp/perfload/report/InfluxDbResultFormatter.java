/*
 * Copyright (c) 2014 mgm technology partners GmbH
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
package com.mgmtp.perfload.report;

import java.net.InetAddress;
import java.util.AbstractMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import org.apache.commons.lang3.time.StopWatch;
import org.influxdb.dto.Point;

import static java.util.stream.Collectors.toMap;

public class InfluxDbResultFormatter implements ResultFormatter {

	public static final String KO = "ko";
	public static final String OK = "ok";
	private final SimpleLogger logger;
	protected final InetAddress localAddress;
	protected final String layer;
	protected final String operation;
	private final int pid;
	protected final String target;
	private final String measurement;

	/**
	 * @param logger the underlying logger to use
	 * @param localAddress the local address of the client
	 * @param layer some identifier for the layer in which the result is logged (e. g. client, server, ...)
	 * @param pid
	 */
	public InfluxDbResultFormatter(SimpleLogger logger, InetAddress localAddress, String layer, String operation, int pid, String target, String measurement) {
		this.logger = logger;
		this.localAddress = localAddress;
		this.layer = layer;
		this.operation = operation;
		this.pid = pid;
		this.target = target;
		this.measurement = measurement;
	}

	/**
	 * {@inheritDoc} See class comment above for details.
	 */
	@Override
	public void formatResult(String message, long timestamp, StopWatch ti1, StopWatch ti2, String type, String uri,
		String uriAlias, UUID executionId, UUID requestId, Object... extraArgs) {

		Point.Builder builder = Point.measurement(measurement)
			.time(timestamp, TimeUnit.MILLISECONDS)
			.tag("operation", operation)
			.tag("target", target)
			.tag("type", type)
			.tag("uri", uri)
			.tag("pid", String.valueOf(pid))
			.tag("uriAlias", uriAlias)
			.tag("localAddress", localAddress.toString())
			.tag("layer", layer)
			.tag("executionId", String.valueOf(executionId))
			.tag("requestId", String.valueOf(requestId))
			.tag(extraArgsToMap(extraArgs))
			.addField("ti1", ti1.getNanoTime())
			.addField("ti2", ti2.getNanoTime());
		if (message == null) {
			builder.tag("status", OK);
		} else {
			builder.tag("message", message).tag("status", KO);
		}
		logger.writeln(builder.build().lineProtocol());
	}

	private Map<String, String> extraArgsToMap(Object[] extraArgs) {
		AtomicInteger i = new AtomicInteger(0);
		return Stream.of(extraArgs)
			.filter(Objects::nonNull)
			.flatMap(extraArgsItem -> Optional.of(extraArgsItem)
				.filter(extraArg -> extraArg instanceof Map)
				.map(this::getStreamOfMapEntries)
				.orElse(Stream.of(new AbstractMap.SimpleEntry<>(String.format("arg%d", i.getAndIncrement()), extraArgsItem.toString()))))
			.collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
	}

	private Stream<Map.Entry<String, String>> getStreamOfMapEntries(Object m) {
		try {
			//noinspection unchecked
			return ((Map<String, String>) m).entrySet().stream();
		} catch (Exception e) {
			return Stream.empty();
		}
	}
}
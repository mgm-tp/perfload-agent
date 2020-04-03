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
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.collections.keyvalue.DefaultMapEntry;
import org.influxdb.dto.Point;

import com.mgmtp.perfload.agent.StopWatch;

public class InfluxDbResultLogger implements ResultLogger {

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
	public InfluxDbResultLogger(SimpleLogger logger, InetAddress localAddress, String layer, String operation, int pid, String target, String measurement) {
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
	public void logResult(String message, long timestamp, StopWatch ti1, StopWatch ti2, String type, String uri,
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
			.addField("ti1", ti1.duration().getNano())
			.addField("ti2", ti2.duration().getNano());
		if (message == null) {
			builder.tag("status", OK);
		} else {
			builder.tag("message", message).tag("status", KO);
		}
		logger.writeln(addExtraArgs(builder, extraArgs).build().lineProtocol());
	}

	private Point.Builder addExtraArgs(Point.Builder builder, Object[] extraArgs) {
		AtomicInteger i = new AtomicInteger(0);
		builder.tag(Stream.of(extraArgs).flatMap(o -> {
			Stream<Map.Entry<String, String>> setStream;
			if (o instanceof Map) {
				try {
					//noinspection unchecked
					setStream = ((Map<String, String>) o).entrySet().stream();
				} catch (Exception e) {
					setStream = Stream.empty();
				}
			} else {
				//noinspection unchecked
				setStream = Stream.of((Map.Entry<String, String>) new DefaultMapEntry(String.format("arg%d", i.getAndIncrement()), o.toString()));
			}
			return setStream;
		}).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
		return builder;
	}
}
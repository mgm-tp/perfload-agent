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
	private final ThreadLocal<Point.Builder> builder = new ThreadLocal<>();

	public InfluxDbResultFormatter(InetAddress localAddress, String layer, String operation, int pid, String target, String measurement) {
		builder.set(Point.measurement(measurement)
			.tag("operation", operation)
			.tag("target", target)
			.tag("pid", String.valueOf(pid))
			.tag("localAddress", localAddress.toString())
			.tag("layer", layer))
		;
	}

	@Override
	public String formatResult(String message, long timestamp, StopWatch ti1, StopWatch ti2, String type, String uri,
		String uriAlias, UUID executionId, UUID requestId, Object... extraArgs) {
		Point.Builder builder = this.builder.get();
		try {
			builder
				.time(timestamp, TimeUnit.MILLISECONDS)
				.tag("type", type)
				.tag("uri", uri)
				.tag("uriAlias", uriAlias)
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
			return builder.build().lineProtocol();
		} catch (NullPointerException e) {
			throw e;
		}
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
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

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;

import javax.annotation.Nonnull;

import org.apache.commons.lang3.StringUtils;
import org.influxdb.BatchOptions;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.mgmtp.perfload.agent.ResultFormatterFactory;
import com.mgmtp.perfload.agent.annotations.InfluxDbUri;

public class InfluxDbTcpLogger extends AbstractThreadedLogger {

	private final InfluxDB influxDb;
	private final static Logger LOG = LoggerFactory.getLogger(InfluxDbTcpLogger.class);
	private final ThreadLocal<LoadingCache<String, ResultFormatter>> formatterCache = new ThreadLocal<>();
	private final ResultFormatterFactory formatterFactory;

	public InfluxDbTcpLogger(ResultFormatterFactory formatterFactory, @InfluxDbUri URI uri) {
		super();
		this.formatterFactory = formatterFactory;
		influxDb = connect(uri);
		Runtime.getRuntime().addShutdownHook(new Thread(this::close));
	}

	protected InfluxDB connect(URI uri) {
		final InfluxDB influxDb;
		String url = uri.getScheme() + "://" + uri.getAuthority() + "/";
		String database = StringUtils.stripStart(uri.getPath(), "/");
		LOG.info("Connecting to InfluxDB URI: {}, DB: {}", url, database);
		influxDb = Optional.of(new AuthInfo(uri))
			.filter(AuthInfo::isValid)
			.map(authInfo -> InfluxDBFactory.connect(url, authInfo.getUser(), authInfo.getPassword()))
			.orElse(InfluxDBFactory.connect(url))
			.setDatabase(database)
			.enableBatch(BatchOptions.DEFAULTS);
		LOG.info("Opened connection to InfluxDB URI: {}, DB: {}", url, database);
		return influxDb;
	}

	@Override
	protected void processLog(ResultObject r) {
		if (influxDb == null) {
			LOG.error("Connection to InfluxDB is not open.");
		} else {
			influxDb.write(getOrCreateFormatterCache().getUnchecked(r.getOperation() == null ? "unknown" : r.getOperation())
				.formatResult(r.getErrorMessage(), r.getTimestamp(), r.getTi1(), r.getTi2(), r.getType(),
					r.getUri(), r.getUriAlias(), r.getExecutionId(), r.getRequestId(), r.getExtraArgs()));
		}
	}

	public void close()  {
		shutdown();
		if (influxDb != null) {
			influxDb.close();
		}
	}

	protected LoadingCache<String, ResultFormatter> getOrCreateFormatterCache() {
		if (formatterCache.get() == null) {
			this.formatterCache.set(CacheBuilder.newBuilder().build(new CacheLoader<String, ResultFormatter>() {
				@Override
				public ResultFormatter load(@Nonnull String operation) {
					return formatterFactory.createFormatter(operation);
				}
			}));
		}
		return formatterCache.get();
	}

	private static class AuthInfo {

		private final String user;
		private final String password;

		public AuthInfo(URI uri) {
			String userInfo = uri.getUserInfo();
			String[] userPasswordPair = decodeAuthInfo(userInfo).split(":", 2);
			user = userPasswordPair.length > 0 ? userPasswordPair[0] : null;
			password = userPasswordPair.length > 1 ? userPasswordPair[1] : null;
		}

		private String decodeAuthInfo(String userInfo) {
			if (userInfo == null) {
				return "";
			}
			if (userInfo.contains(":")) {
				return userInfo;
			}
			try {
				return new String(Base64.getDecoder().decode(userInfo), StandardCharsets.UTF_8);
			} catch (IllegalArgumentException ignored) {
				return userInfo;
			}
		}

		public String getPassword() {
			return password;
		}

		public String getUser() {
			return user;
		}

		public boolean isValid() {
			return StringUtils.isNoneBlank(user, password);
		}
	}

}
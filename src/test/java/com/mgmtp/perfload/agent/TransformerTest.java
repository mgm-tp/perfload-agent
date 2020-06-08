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
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.servlet.FilterChain;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.common.io.Resources;
import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.util.Modules;
import com.mgmtp.perfload.agent.annotations.ConfigFile;
import com.mgmtp.perfload.agent.hook.ServletApiHook;
import com.mgmtp.perfload.agent.util.ClassNameUtils;
import com.mgmtp.perfload.report.SimpleLogger;

import static com.mgmtp.perfload.report.InfluxDbResultFormatter.KO;
import static com.mgmtp.perfload.report.InfluxDbResultFormatter.OK;
import static java.util.regex.Pattern.DOTALL;
import static java.util.regex.Pattern.MULTILINE;
import static org.apache.commons.io.FileUtils.writeByteArrayToFile;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

/**
 * @author rnaegele
 */
@Test
public class TransformerTest {

	public static final String OK_LINE_PATTERN = ".*^jmeter,executionId=%s,layer=agent,localAddress=.*,operation=%s,pid=%d,requestId=%s,status=" + OK + ",target=agent,type=AGENT,uri=%s,uriAlias=%s ti1=\\d+i,ti2=\\d+i \\d+$.*";
	public static final String KO_LINE_PATTERN = ".*^jmeter,executionId=%s,layer=agent,localAddress=.*,message=%s,operation=%s,pid=%d,requestId=%s,status=" + KO + ",target=agent,type=AGENT,uri=%s,uriAlias=%s ti1=\\d+i,ti2=\\d+i \\d+$.*";

	@Inject
	private Transformer transformer;

	private final TestSimpleLogger testLogger = new TestSimpleLogger();

	private Class<?> testClass;
	private Class<?> filterClass;
	private Class<?> servletClass;

	private HttpServletRequest request;
	private UUID execId;
	private UUID reqId;
	private int pid;

	private static final Logger LOG = LoggerFactory.getLogger(TransformerTest.class);

	@BeforeClass
	public void init() throws IOException, ClassNotFoundException {
		final File agentDir = new File("build");
		pid = Agent.retrievePid();

		Injector injector = InjectorHolder.INSTANCE.createInjector(Modules.override(new AgentModule(agentDir, pid))
			.with(
				new AbstractModule() {
					@Override
					protected void configure() {
						bind(File.class).annotatedWith(ConfigFile.class).toInstance(new File("src/test/resources/perfload-agent.json"));
						bind(SimpleLogger.class).toInstance(testLogger);
					}
				}));
		injector.injectMembers(this);

		testClass = loadClass("com.mgmtp.perfload.agent.Test");
		filterClass = loadClass("com.mgmtp.perfload.agent.TestFilter");
		servletClass = loadClass("com.mgmtp.perfload.agent.TestServlet");
	}

	@BeforeMethod
	public void beforeMethod() {
		request = mock(HttpServletRequest.class);
		execId = UUID.randomUUID();
		reqId = UUID.randomUUID();
		when(request.getHeader(ServletApiHook.EXECUTION_ID_HEADER)).thenReturn(execId.toString());
		when(request.getHeader(ServletApiHook.OPERATION_HEADER)).thenReturn("operation");
		when(request.getHeader(ServletApiHook.REQUEST_ID_HEADER)).thenReturn(reqId.toString());
	}

	@Test
	public void testMeasuringHook() throws Exception {
		Constructor<?> constructor = testClass.getConstructor(Boolean.class);

		Object object = constructor.newInstance(Boolean.TRUE);
		try {
			object.getClass().getMethod("check").invoke(object);
			fail();
		} catch (InvocationTargetException ex) {
			// expected
		}

		object = constructor.newInstance(Boolean.FALSE);
		testClass.getMethod("checkI", int.class).invoke(null, 1);
		testClass.getMethod("checkLL", long.class, long.class).invoke(object, 42L, 43L);
		testClass.getMethod("check").invoke(object);

		List<String> measuringLogContents = testLogger.getResults();
		assertEquals(measuringLogContents.size(), 4);
		assertMatches(measuringLogContents.get(0), KO_LINE_PATTERN, null, "foo", "unknown", pid, null, "c.m.p.a.Test.check\\(\\)", "c.m.p.a.Test.check\\(\\)");
		assertMatches(measuringLogContents.get(1), OK_LINE_PATTERN, null, "unknown", pid, null, "c.m.p.a.Test.checkI\\(int\\)", "c.m.p.a.Test.checkI\\(int\\)");
		assertMatches(measuringLogContents.get(2), OK_LINE_PATTERN, null, "unknown", pid, null, "c.m.p.a.Test.checkLL\\(long\\\\,\\\\ long\\)", "c.m.p.a.Test.checkLL\\(long\\\\,\\\\ long\\)");
		assertMatches(measuringLogContents.get(3), OK_LINE_PATTERN, null, "unknown", pid, null, "c.m.p.a.Test.check\\(\\)", "c.m.p.a.Test.check\\(\\)");
	}

	@Test
	public void testServletApiHookWithFilter() throws Exception {
		Object filter = filterClass.getConstructor().newInstance();
		filterClass.getMethod("doFilter", ServletRequest.class, ServletResponse.class, FilterChain.class).invoke(filter, request,
			null, null);

		String fileContents = String.join("\n", testLogger.getResults());
		assertMatches(fileContents, OK_LINE_PATTERN, execId, "operation", pid, reqId, "c.m.p.a.Test.checkLL\\(long\\\\,\\\\ long\\)", "c.m.p.a.Test.checkLL\\(long\\\\,\\\\ long\\)");
	}

	@Test
	public void testServletApiHookWithServlet() throws Exception {

		Object servlet = servletClass.getConstructor().newInstance();
		servletClass.getMethod("service", HttpServletRequest.class, HttpServletResponse.class).invoke(servlet, request,
			mock(HttpServletResponse.class));

		String fileContents = String.join("\n", testLogger.getResults());
		assertMatches(fileContents, OK_LINE_PATTERN, execId, "operation", pid, reqId, "c.m.p.a.TestServlet.service\\(j.s.h.HttpServletRequest\\\\,\\\\ j.s.h.HttpServletResponse\\)", "c.m.p.a.TestServlet.service\\(j.s.h.HttpServletRequest\\\\,\\\\ j.s.h.HttpServletResponse\\)");
		assertTrue(fileContents.contains(ClassNameUtils.abbreviatePackageName(servletClass.getName())));
	}

	private void assertMatches(String actual, String expectedPattern, Object... params) {
		String pattern = String.format(expectedPattern, params);
		assertTrue(Pattern.compile(pattern, DOTALL + MULTILINE).matcher(actual).matches(), String.format("\nPATTERN: %s\nCONTENT: %s\n", pattern, actual));
	}

	private Class<?> loadClass(final String fqcn) throws IOException, ClassNotFoundException {
		String internalName = fqcn.replace('.', '/');

		@SuppressWarnings("UnstableApiUsage")
		byte[] classBytes = Resources.toByteArray(Resources.getResource(internalName + ".class"));
		byte[] transformedClass = transformer.transform(null, internalName, null, null, classBytes);
		File classFile = new File("tmp/" + internalName + ".class");
		writeByteArrayToFile(classFile, transformedClass);

		URLClassLoader loader = new URLClassLoader(new URL[] { new File("tmp").toURI().toURL() }) {
			/**
			 * Loads the class with the specified binary name trying to load it from the local
			 * classpath first before delegating to the normal class loading mechanism.
			 */
			@Override
			protected synchronized Class<?> loadClass(final String name, final boolean resolve) throws ClassNotFoundException {
				// Check if the class has already been loaded
				Class<?> loadedClass = findLoadedClass(name);

				if (loadedClass == null) {
					if (name.startsWith("com.mgmtp.perfload.agent.Test")) {
						try {
							// First try to find it locally
							loadedClass = findClass(name);
						} catch (ClassNotFoundException e) {
							// Swallow exception --> the class does not exist locally
						}
					}
					// If the class is not found locally we delegate to the normal class loading mechanism
					if (loadedClass == null) {
						loadedClass = super.loadClass(name, resolve);
					}
				}

				if (resolve) {
					resolveClass(loadedClass);
				}
				return loadedClass;
			}
		};
		return loader.loadClass(fqcn);
	}

	private static class TestSimpleLogger implements SimpleLogger {

		private List<String> results;

		@Override
		public void open() {
			LOG.info("OPEN log");
			results = new ArrayList<>();
		}

		@Override
		public void writeln(String output) {
			LOG.info("WRITE log: {}", output);
			results.add(output);
		}

		@Override
		public void close() {
			LOG.info("CLOSING log");
		}

		public List<String> getResults() {
			return results;
		}
	}
}

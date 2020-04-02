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
import java.lang.instrument.IllegalClassFormatException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;
import javax.servlet.FilterChain;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import com.google.common.io.Resources;
import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.util.Modules;
import com.mgmtp.perfload.agent.annotations.ConfigFile;
import com.mgmtp.perfload.agent.hook.ServletApiHook;
import com.mgmtp.perfload.agent.util.ClassNameUtils;

import static org.apache.commons.io.FileUtils.writeByteArrayToFile;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

/**
 * @author rnaegele
 */
public class TransformerTest {

	private static final File MEASURING_LOG_FILE = new File("target", String.format("perfload-agent-measuring-%d.log",
			Agent.retrievePid()));

	@Inject
	private Transformer transformer;

	private Class<?> testClass;
	private Class<?> filterClass;
	private Class<?> servletClass;

	private HttpServletRequest request;
	private UUID execId;
	private UUID reqId;

	@BeforeClass
	public void init() throws IOException, IllegalClassFormatException, ClassNotFoundException {
		final File agentDir = new File("target");
		int pid = Agent.retrievePid();

		Injector injector = InjectorHolder.INSTANCE.createInjector(Modules.override(new AgentModule(agentDir, pid)).with(
				new AbstractModule() {
					@Override
					protected void configure() {
						bind(File.class).annotatedWith(ConfigFile.class).toInstance(
								new File("src/test/resources/perfload-agent.json"));
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

		List<String> measuringLogContents = Files.readLines(MEASURING_LOG_FILE, Charsets.UTF_8);
		assertEquals(measuringLogContents.size(), 4);
		assertTrue(measuringLogContents.get(0).contains("ERROR"));
		assertTrue(measuringLogContents.get(1).contains("SUCCESS"));
		assertTrue(measuringLogContents.get(2).contains("SUCCESS"));
		assertTrue(measuringLogContents.get(3).contains("SUCCESS"));
	}

	@Test
	public void testServletApiHookWithFilter() throws Exception {
		Object filter = filterClass.newInstance();
		filterClass.getMethod("doFilter", ServletRequest.class, ServletResponse.class, FilterChain.class).invoke(filter, request,
				null, null);

		String fileContents = Files.toString(MEASURING_LOG_FILE, Charsets.UTF_8);
		assertTrue(fileContents.matches(String.format("(?s).*%s[^\r\n]*?%s[^\r\n]*?%s.*", "operation", execId, reqId)));
	}

	@Test
	public void testServletApiHookWithServlet() throws Exception {
		Object servlet = servletClass.newInstance();
		servletClass.getMethod("service", HttpServletRequest.class, HttpServletResponse.class).invoke(servlet, request,
				mock(HttpServletResponse.class));

		String fileContents = Files.toString(MEASURING_LOG_FILE, Charsets.UTF_8);
		assertTrue(fileContents.matches(String.format("(?s).*%s[^\r\n]*?%s[^\r\n]*?%s.*", "operation", execId, reqId)));
		assertTrue(fileContents.contains(ClassNameUtils.abbreviatePackageName(servletClass.getName())));
	}

	private Class<?> loadClass(final String fqcn) throws IOException, IllegalClassFormatException, MalformedURLException,
			ClassNotFoundException {
		String internalName = fqcn.replace('.', '/');

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
}

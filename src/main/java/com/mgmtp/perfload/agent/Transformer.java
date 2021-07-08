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
import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang3.mutable.MutableBoolean;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mgmtp.perfload.agent.annotations.AgentDir;
import com.mgmtp.perfload.agent.config.Config;
import com.mgmtp.perfload.agent.config.EntryPoints;
import com.mgmtp.perfload.agent.config.MethodInstrumentations;
import com.mgmtp.perfload.agent.hook.MeasuringHookMethodVisitor;
import com.mgmtp.perfload.agent.hook.ServletApiHookMethodVisitor;

import static org.apache.commons.io.FileUtils.writeByteArrayToFile;
import static org.apache.commons.lang3.StringUtils.substringAfterLast;
import static org.apache.commons.lang3.StringUtils.substringBeforeLast;

/**
 * @author rnaegele
 */
@Singleton
public class Transformer implements ClassFileTransformer {

	// method descriptor for servlets because service method is overloaded
	private static final String SERVLET_SERVICE_DESC = "(Ljavax/servlet/http/HttpServletRequest;Ljavax/servlet/http/HttpServletResponse;)V";
	public static final int OPCODES = Opcodes.ASM5;

	private final Config config;
	private static final Logger LOG = LoggerFactory.getLogger(Transformer.class);
	private final File agentDir;

	@Inject
	public Transformer(final Config config, @AgentDir final File agentDir) {
		LOG.info("AgentDir: {}", agentDir);
		this.config = config;
		this.agentDir = agentDir;
	}

	@Override
	public byte[] transform(final ClassLoader loader, final String className, final Class<?> classBeingRedefined,
		final ProtectionDomain protectionDomain, final byte[] classfileBuffer) {

		final String classNameWithDots = className.replace('/', '.');
		EntryPoints entryPoints = config.getEntryPoints();
		final boolean isFilter = entryPoints.hasFilter(classNameWithDots);
		final boolean isServlet = entryPoints.hasServlet(classNameWithDots);

		final Map<String, MethodInstrumentations> methodsConfig = config.getInstrumentations().get(classNameWithDots);

		if (methodsConfig == null && !isFilter && !isServlet) {
			// no instrumentation configured for this class
			// return null, so no transformation is done
			return null;
		}

		LOG.info("Transforming class: " + classNameWithDots);

		// flag for storing if at least one hook is weaved in
		final MutableBoolean weaveFlag = new MutableBoolean();

		ClassReader cr = new ClassReader(classfileBuffer);
		ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS);
		ClassVisitor cv = new ClassVisitor(OPCODES, cw) {
			@Override
			public MethodVisitor visitMethod(final int access, final String name, final String descriptor, final String signature,
				final String[] exceptions) {
				MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
				if (mv != null) {
					if (isFilter && "doFilter".equals(name)
						|| isServlet && "service".equals(name) && SERVLET_SERVICE_DESC.equals(descriptor)) {
						mv = createServletApiHookVisitor(access, name, descriptor, mv);
					}
					if (methodsConfig != null) {
						MethodInstrumentations methodInstrumentations = methodsConfig.get(name);
						if (methodInstrumentations != null) {
							mv = createMeasuringHookVisitor(access, name, descriptor, mv, methodInstrumentations);
						}
					}
				}
				return mv;
			}

			private MethodVisitor createMeasuringHookVisitor(final int access, final String methodName, final String methodDescriptor,
				final MethodVisitor mv, final MethodInstrumentations methodInstrumentations) {
				if (matchInstrumentation(methodDescriptor, methodInstrumentations)) {
					LOG.info("Instrumenting method: " + classNameWithDots + "." + methodName);
					weaveFlag.setValue(true);
					return new MeasuringHookMethodVisitor(access, classNameWithDots, methodName, methodDescriptor, mv);
				}
				return mv;
			}

			private MethodVisitor createServletApiHookVisitor(final int access, final String methodName, final String desc,
				final MethodVisitor mv) {
				LOG.info("Adding servlet api hook: " + classNameWithDots + "." + methodName);
				weaveFlag.setValue(true);
				return new ServletApiHookMethodVisitor(access, methodName, desc, mv);
			}
		};

		// accept the visitor in order to perform weaving
		cr.accept(cv, ClassReader.EXPAND_FRAMES);

		if (weaveFlag.isTrue()) {
			byte[] transformedClassBytes = cw.toByteArray();
			dumpTransformedClassFile(className, transformedClassBytes);
			LOG.info("Bytecode updated: " + classNameWithDots);
			return transformedClassBytes;
		} else {
			// no transformation
			return null;
		}
	}

	private boolean matchInstrumentation(String descriptor, MethodInstrumentations methodInstrumentations) {
		if (methodInstrumentations.isEmpty()) {
			return true;
		}
		for (List<String> paramClassNames : methodInstrumentations) {
			if (Arrays.stream(Type.getArgumentTypes(descriptor))
				.map(Type::getClassName)
				.collect(Collectors.toList()).equals(paramClassNames)) {
				return true;
			}
		}
		return false;
	}

	private void dumpTransformedClassFile(final String className, final byte[] transformedClassBytes) {
		String filePath = substringBeforeLast(className, ".").replace('.', '/');
		String fileName = substringAfterLast(className, ".") + ".class";

		File baseDirectory = new File(agentDir, "classdump");
		File dir = new File(baseDirectory, filePath);
		dir.mkdirs();
		File classFile = new File(dir, fileName);
		try {
			writeByteArrayToFile(classFile, transformedClassBytes);
		} catch (IOException ex) {
			LOG.error(ex.getMessage() + "-" + classFile, ex);
		}
	}
}

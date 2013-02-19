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

import static com.google.common.collect.Lists.newArrayListWithCapacity;
import static org.apache.commons.io.FileUtils.writeByteArrayToFile;
import static org.apache.commons.lang3.StringUtils.substringAfterLast;
import static org.apache.commons.lang3.StringUtils.substringBeforeLast;

import java.io.File;
import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang3.mutable.MutableBoolean;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import com.mgmtp.perfload.agent.annotations.AgentDir;
import com.mgmtp.perfload.agent.config.Config;
import com.mgmtp.perfload.agent.config.EntryPoints;
import com.mgmtp.perfload.agent.config.MethodInstrumentations;
import com.mgmtp.perfload.agent.hook.MeasuringHookMethodVisitor;
import com.mgmtp.perfload.agent.hook.ServletApiHookMethodVisitor;

/**
 * @author rnaegele
 */
@Singleton
public class Transformer implements ClassFileTransformer {

	// method descriptor for servlets because service method is overloaded
	private static final String SERVLET_SERVICE_DESC = "(Ljavax/servlet/http/HttpServletRequest;Ljavax/servlet/http/HttpServletResponse;)V";

	private final Config config;
	private final AgentLogger logger;
	private final File agentDir;

	@Inject
	public Transformer(final Config config, final AgentLogger logger, @AgentDir final File agentDir) {
		this.config = config;
		this.logger = logger;
		this.agentDir = agentDir;
	}

	@Override
	public byte[] transform(final ClassLoader loader, final String className, final Class<?> classBeingRedefined,
			final ProtectionDomain protectionDomain, final byte[] classfileBuffer) throws IllegalClassFormatException {

		final String classNameWithDots = className.replace('/', '.');
		EntryPoints entryPoints = config.getEntryPoints();

		final Map<String, MethodInstrumentations> methodsConfig = config.getInstrumentations().get(classNameWithDots);
		final boolean isFilter = entryPoints.hasFilter(classNameWithDots);
		final boolean isServlet = entryPoints.hasServlet(classNameWithDots);

		if (methodsConfig == null && !isFilter && !isServlet) {
			// no instrumentation configured for this class
			// return null, so no transformation is done
			return null;
		}

		logger.writeln("Transforming class: " + classNameWithDots);

		// flag for storing if at least one hook is weaved in
		final MutableBoolean weaveFlag = new MutableBoolean();

		ClassReader cr = new ClassReader(classfileBuffer);
		ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS);
		ClassVisitor cv = new ClassVisitor(Opcodes.ASM4, cw) {
			@Override
			public MethodVisitor visitMethod(final int access, final String name, final String desc, final String signature,
					final String[] exceptions) {
				MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
				if (mv != null) {
					if (isFilter && "doFilter".equals(name)
							|| isServlet && "service".equals(name) && SERVLET_SERVICE_DESC.equals(desc)) {
						mv = createServletApiHookVisitor(access, name, desc, mv);
					}
					if (methodsConfig != null) {
						MethodInstrumentations methodInstrumentations = methodsConfig.get(name);
						if (methodInstrumentations != null) {
							mv = createMeasuringHookVisitor(access, name, desc, mv, methodInstrumentations);
						}
					}
				}
				return mv;
			}

			private MethodVisitor createMeasuringHookVisitor(final int access, final String methodName, final String desc,
					final MethodVisitor mv, final MethodInstrumentations methodInstrumentations) {
				boolean weave = false;
				if (methodInstrumentations.isEmpty()) {
					// no params configured, so we just weave the hook into any method with this name
					weave = true;
				} else {
					// weave if params match
					for (List<String> paramClassNames : methodInstrumentations) {
						Type[] argumentTypes = Type.getArgumentTypes(desc);
						List<String> classNames = newArrayListWithCapacity(argumentTypes.length);
						for (Type argumentType : argumentTypes) {
							classNames.add(argumentType.getClassName());
						}
						if (classNames.equals(paramClassNames)) {
							weave = true;
							break;
						}
					}
				}
				if (weave) {
					logger.writeln("Instrumenting method: " + classNameWithDots + "." + methodName);
					weaveFlag.setValue(true);
					return new MeasuringHookMethodVisitor(access, classNameWithDots, methodName, desc, mv);
				}
				return mv;
			}

			private MethodVisitor createServletApiHookVisitor(final int access, final String methodName, final String desc,
					final MethodVisitor mv) {
				logger.writeln("Adding servlet api hook: " + classNameWithDots + "." + methodName);
				weaveFlag.setValue(true);
				return new ServletApiHookMethodVisitor(access, methodName, desc, mv);
			}
		};

		// accept the visitor in order to perform weaving
		cr.accept(cv, ClassReader.EXPAND_FRAMES);

		if (weaveFlag.isTrue()) {
			byte[] transformedclassBytes = cw.toByteArray();
			dumpTransformedClassFile(className, transformedclassBytes);
			return transformedclassBytes;
		}

		// no transformation
		return null;
	}

	private void dumpTransformedClassFile(final String className, final byte[] transformedclassBytes) {
		String filePath = substringBeforeLast(className, ".").replace('.', '/');
		String fileName = substringAfterLast(className, ".") + ".class";

		File dir = new File(new File(agentDir, "classdump"), filePath);
		dir.mkdirs();
		File classFile = new File(dir, fileName);
		try {
			writeByteArrayToFile(classFile, transformedclassBytes);
		} catch (IOException ex) {
			logger.writeln(ex.getMessage() + "-" + classFile, ex);
		}
	}
}

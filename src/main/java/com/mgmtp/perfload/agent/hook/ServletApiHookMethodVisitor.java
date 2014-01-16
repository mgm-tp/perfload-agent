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

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.AdviceAdapter;

/**
 * An ASM {@link MethodVisitor} that weave the {@link HookManager} into a method's byte code.
 * 
 * @author rnaegele
 */
public class ServletApiHookMethodVisitor extends AdviceAdapter {
	private static final String ENTER_HOOK_DESC = new StringBuilder(100)
			.append('(')
			.append(Type.getDescriptor(Object.class))
			.append(Type.getDescriptor(Object[].class))
			.append(")V")
			.toString();

	private static final String EXIT_HOOK_DESC = "()V";

	private static final String OWNER = HookManager.class.getName().replace('.', '/');

	public ServletApiHookMethodVisitor(final int access, final String methodName, final String desc, final MethodVisitor mv) {
		super(ASM4, mv, access, methodName, desc);
	}

	@Override
	protected void onMethodEnter() {
		loadThis();
		loadArgArray();
		mv.visitMethodInsn(INVOKESTATIC, OWNER, "enterServletApiHook", ENTER_HOOK_DESC);
	}

	@Override
	protected void onMethodExit(final int opcode) {
		mv.visitMethodInsn(INVOKESTATIC, OWNER, "exitServletApiHook", EXIT_HOOK_DESC);
	}
}
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

import static com.mgmtp.perfload.agent.Transformer.OPCODES;
import static com.mgmtp.perfload.agent.util.ClassNameUtils.computeFullyQualifiedMethodName;

/**
 * An ASM {@link MethodVisitor} that weave the {@link HookManager} into a method's byte code.
 *
 * @author rnaegele
 */
public class MeasuringHookMethodVisitor extends AdviceAdapter {

	private static final String ENTER_HOOK_DESC = String.format("(%s%s)V", Type.getDescriptor(Object.class),
		Type.getDescriptor(String.class));

	private static final String ENTER_HOOK_DESC_WITH_ARGS = String.format("(%s%s%s)V", Type.getDescriptor(Object.class),
		Type.getDescriptor(String.class), Type.getDescriptor(Object[].class));

	private static final String EXIT_HOOK_DESC = String.format("(%s%s%s)V",
		Type.getDescriptor(Object.class), Type.getDescriptor(Throwable.class), Type.getDescriptor(String.class));

	private static final String EXIT_HOOK_DESC_WITH_ARGS = String.format("(%s%s%s%s)V",
		Type.getDescriptor(Object.class), Type.getDescriptor(Throwable.class),
		Type.getDescriptor(String.class), Type.getDescriptor(Object[].class));

	private static final String OWNER = HookManager.class.getName().replace('.', '/');

	private final int numArgs;
	private final String fullyQualifiedMethodName;

	public MeasuringHookMethodVisitor(final int access, final String className, final String methodName, final String desc,
		final MethodVisitor mv) {
		super(OPCODES, mv, access, methodName, desc);
		Type[] argumentTypes = Type.getArgumentTypes(methodDesc);
		this.numArgs = argumentTypes.length;
		this.fullyQualifiedMethodName = computeFullyQualifiedMethodName(className, methodName, argumentTypes);
	}

	@Override
	protected void onMethodEnter() {
		if ((methodAccess & ACC_STATIC) != 0) {
			visitInsn(ACONST_NULL);
		} else {
			loadThis();
		}
		push(fullyQualifiedMethodName);
		if (numArgs > 0) {
			loadArgArray();
			mv.visitMethodInsn(INVOKESTATIC, OWNER, "enterMeasuringHook", ENTER_HOOK_DESC_WITH_ARGS, false);
		} else {
			mv.visitMethodInsn(INVOKESTATIC, OWNER, "enterMeasuringHook", ENTER_HOOK_DESC, false);
		}
	}

	@Override
	protected void onMethodExit(final int opcode) {
		if (opcode == ATHROW) {
			// Check if an exception is on the stack and duplicate it so we can pass it to the HookManager
			dup();
		} else {
			// Otherwise just push null, since we are not interested in return values
			visitInsn(ACONST_NULL);
		}

		if ((methodAccess & ACC_STATIC) != 0) {
			visitInsn(ACONST_NULL);
		} else {
			loadThis();
		}

		swap(); // Swap top two elements on stack to have them in the correct order for the call to exitHook
		push(fullyQualifiedMethodName);
		if (numArgs > 0) {
			loadArgArray();
			mv.visitMethodInsn(INVOKESTATIC, OWNER, "exitMeasuringHook", EXIT_HOOK_DESC_WITH_ARGS, false);
		} else {
			mv.visitMethodInsn(INVOKESTATIC, OWNER, "exitMeasuringHook", EXIT_HOOK_DESC, false);
		}
	}
}
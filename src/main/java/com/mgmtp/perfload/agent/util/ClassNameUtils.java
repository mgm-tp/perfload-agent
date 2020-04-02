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
package com.mgmtp.perfload.agent.util;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.text.TextStringBuilder;
import org.objectweb.asm.Type;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;

import static org.apache.commons.lang.StringUtils.substringAfterLast;
import static org.apache.commons.lang.StringUtils.substringBeforeLast;

/**
 * Utility class for class names.
 *
 * @author rnaegele
 * @since 1.3.0
 */
public class ClassNameUtils {
	public static String computeFullyQualifiedMethodName(final String className, final String methodName, final Type[] argumentTypes) {
		TextStringBuilder sb = new TextStringBuilder(50)
			.append(abbreviatePackageName(className))
			.append('.')
			.append(methodName)
			.append('(');
		IntStream.range(0, argumentTypes.length).forEach(i ->
			sb.appendSeparator(", ", i)
				.append(abbreviatePackageName(argumentTypes[i].getClassName())));
		sb.append(')');
		return sb.toString();
	}

	public static String abbreviatePackageName(String className) {
		if (className.contains(".")) {
			List<String> list = Splitter.on('.')
				.splitToList(substringBeforeLast(className, "."))
				.stream()
				.map(input -> input.substring(0, 1))
				.collect(Collectors.toList());
			return String.format("%s.%s", Joiner.on('.').join(list), substringAfterLast(className, "."));
		} else {
			return className;
		}
	}
}

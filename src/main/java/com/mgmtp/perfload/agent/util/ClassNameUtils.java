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

import org.apache.commons.lang3.text.StrBuilder;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;

import jdk.internal.org.objectweb.asm.Type;

import static com.google.common.collect.FluentIterable.from;
import static org.apache.commons.lang.StringUtils.substringAfterLast;
import static org.apache.commons.lang.StringUtils.substringBeforeLast;

/**
 * Utility class for class names.
 *
 * @since 1.3.0
 * @author rnaegele
 */
public class ClassNameUtils {
	public static String computeFullyQualifiedMethodName(final String className, final String methodName, final Type[] argumentTypes) {
		StrBuilder sb = new StrBuilder(50);
		sb.append(abbreviatePackageName(className));
		sb.append('.');
		sb.append(methodName);
		sb.append('(');
		for (int i = 0; i < argumentTypes.length; ++i) {
			sb.appendSeparator(", ", i);
			sb.append(abbreviatePackageName(argumentTypes[i].getClassName()));
		}
		sb.append(')');
		return sb.toString();
	}

	public static String abbreviatePackageName(String className) {
		if (className.contains(".")) {
			String packageName = substringBeforeLast(className, ".");
			String simpleClassName = substringAfterLast(className, ".");
			List<String> list = from(Splitter.on('.').splitToList(packageName)).transform(new Function<String, String>() {
				@Override
				public String apply(String input) {
					return input.substring(0, 1);
				}
			}).toList();
			return Joiner.on('.').join(list) + '.' + simpleClassName;
		} else {
			return className;
		}
	}
}

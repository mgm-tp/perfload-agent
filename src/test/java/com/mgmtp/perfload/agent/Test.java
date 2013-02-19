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

/**
 * @author rnaegele
 */
public class Test {

	private final boolean flag;

	static {
		System.out.println("static1");
	}

	static {
		System.out.println("static2");
	}

	public Test(final Boolean flag) {
		this.flag = flag;
	}

	public void check() {
		if (flag) {
			throw new IllegalStateException("foo");
		}

		System.out.println("OK");
	}

	public static void checkI(final int i) {
		System.out.println(i);
	}

	public void checkLL(final long l1, final long l2) {
		System.out.println(l1 + "-" + l2);
	}

	public static void main(final String[] args) {
		checkI(42);
		Test test = new Test(Boolean.FALSE);
		test.check();
		test.checkLL(1L, 2L);
	}
}

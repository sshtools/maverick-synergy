package com.sshtools.common.policy;

import com.sshtools.common.permissions.Policy;

/*-
 * #%L
 * Base API
 * %%
 * Copyright (C) 2002 - 2024 JADAPTIVE Limited
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

/**
 * Represents various class loader related policy.
 * 
 * Note, will be made <code>final</code> at version 3.3.0, and will only be able to
 * be constructed using the {@link ClassLoaderPolicyBuilder}.
 * 
 * TODO make final at 3.3.0+
 */
public class ClassLoaderPolicy implements Policy {

	private ClassLoader classLoader = null;
	/**
	 * Build a new {@link ClassLoaderPolicy}.
	 */
	public final static class ClassLoaderPolicyBuilder {
		private ClassLoader classLoader = ClassLoaderPolicy.class.getClassLoader();

		private ClassLoaderPolicyBuilder() { }
		
		/**
		 * Create a new {@link ClassLoaderPolicyBuilder} that will be used to configure
		 * and create a {@link ClassLoaderPolicy}.
		 * 
		 * @return builder
		 */
		public static ClassLoaderPolicyBuilder create() {
			return new ClassLoaderPolicyBuilder(); 
		}
		
		/**
		 * Set a custom class loader
		 * 
		 * @param classLoader class loader
		 */
		public ClassLoaderPolicyBuilder withClassLoader(ClassLoader classLoader) {
			this.classLoader = classLoader;
			return this;
		}
		
		/**
		 * Build a new {@link KeyExchangePolicy} given the configuration of this builder.
		 * 
		 * @return policy
		 */
		public ClassLoaderPolicy build() {
			return new ClassLoaderPolicy(this);
		}
	}

	/**
	 * Construct a new policy
	 */
	@Deprecated(since = "3.2.0", forRemoval = true)
	public ClassLoaderPolicy() {
		this.classLoader = ClassLoaderPolicy.class.getClassLoader();
	}

	private ClassLoaderPolicy(ClassLoaderPolicyBuilder bldr) {
		this.classLoader = bldr.classLoader;
	}

	/**
	 * Get the class loader
	 * 
	 * @return class loader
	 */
	public ClassLoader getClassLoader() {
		return classLoader;
	}

	/**
	 * Set the class loader
	 * 
	 * @param classLoader class loader
	 * @deprecated will become immutable, use {@link KeyExchangePolicy}.
	 */
	@Deprecated(since = "3.2.0", forRemoval = true)
	public void setClassLoader(ClassLoader classLoader) {
		this.classLoader = classLoader;
	}

	@Override
	public final Class<? extends Policy> type() {
		return ClassLoaderPolicy.class;
	}
}

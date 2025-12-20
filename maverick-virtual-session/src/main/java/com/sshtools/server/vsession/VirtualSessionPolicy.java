package com.sshtools.server.vsession;

/*-
 * #%L
 * Virtual Sessions
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

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import com.sshtools.common.permissions.Policy;
import com.sshtools.common.policy.KeyExchangePolicy;
import com.sshtools.common.policy.KeyExchangePolicy.KeyExchangePolicyBuilder;
/**
 * Represents various key virtual session related policy.
 * 
 * Note, will be made <code>final</code> at version 3.3.0, and will only be able to
 * be constructed using the {@link KeyExchangePolicyBuilder}.
 * 
 * TODO make final at 3.3.0+
 */
public class VirtualSessionPolicy implements Policy {
	
	/**
	 * Build a new {@link VirtualSessionPolicy}.
	 */
	public final static class VirtualSessionPolicyBuilder {

		private Supplier<String> welcomeText = () -> "Maverick Synergy\r\nVirtual Shell ${version}";
		private Optional<String> shellCommand = Optional.empty();
		private List<String> shellArguments = new ArrayList<>();
		private Map<String, String> shellEnvironment = new HashMap<>();
		private Optional<Path> shellDirectory = Optional.empty();
		private Optional<Supplier<String>> bannerText = Optional.empty();
		private boolean disableBanner;

		private VirtualSessionPolicyBuilder() { }
		
		/**
		 * Create a new {@link VirtualSessionPolicyBuilder} that will be used to configure
		 * and create a {@link VirtualSessionPolicy}.
		 * 
		 * @return builder
		 */
		public static VirtualSessionPolicyBuilder create() {
			return new VirtualSessionPolicyBuilder(); 
		}
		
		/**
		 * Whether to disable the banner.
		 * 
		 * @param disableBuilder
		 * @return this for chaining
		 */
		public VirtualSessionPolicyBuilder withDisableBanner(boolean disableBanner) {
			this.disableBanner = disableBanner;
			return this;
		}
		
		/**
		 * Disable the banner.
		 * 
		 * @return this for chaining
		 */
		public VirtualSessionPolicyBuilder withDisabledBanner() {
			return withDisableBanner(true);
		}

		/**
		 * Set the welcome text
		 * 
		 * @param welcomeText the static welcome text
		 * @return this for chaining
		 */
		public VirtualSessionPolicyBuilder withWelcomeText(String welcomeText) {
			return withWelcomeText(() -> welcomeText);
		}
		
		/**
		 * Set a suuplier of welcome text
		 * 
		 * @param welcomeText the welcome text supplier
		 * @return this for chaining
		 */
		public VirtualSessionPolicyBuilder withWelcomeText(Supplier<String> welcomeText) {
			this.welcomeText = welcomeText;
			return this;
		}

		/**
		 * Set the banner text
		 * 
		 * @param bannerText the static banner text
		 * @return this for chaining
		 */
		public VirtualSessionPolicyBuilder withBannerText(String bannerText) {
			return withBannerText(() -> bannerText);
		}
		
		/**
		 * Set a suuplier of welcome text
		 * 
		 * @param welcomeText the welcome text supplier
		 * @return this for chaining
		 */
		public VirtualSessionPolicyBuilder withBannerText(Supplier<String> bannerText) {
			this.bannerText = Optional.of(bannerText);
			return this;
		}

		/**
		 * Set the directory the shell starts in
		 * 
		 * @param shellDirectory shell directory
		 * @return this for chaining
		 */
		public VirtualSessionPolicyBuilder withShellDirectory(String shellDirectory) {
			return withShellDirectory(Paths.get(shellDirectory));
		}

		/**
		 * Set the directory the shell starts in
		 * 
		 * @param shellDirectory shell directory
		 * @return this for chaining
		 */
		public VirtualSessionPolicyBuilder withShellDirectory(File shellDirectory) {
			return withShellDirectory(shellDirectory);
		}

		/**
		 * Set the directory the shell starts in
		 * 
		 * @param shellDirectory shell directory
		 * @return this for chaining
		 */
		public VirtualSessionPolicyBuilder withShellDirectory(Path shellDirectory) {
			this.shellDirectory = Optional.of(shellDirectory);
			return this;
		}

		/**
		 * Set the shell command (OS dependent). By default, the native shell will be used.
		 * 
		 * @param shellCommand shell command
		 * @return this for chaining
		 */
		public VirtualSessionPolicyBuilder withShellCommand(String shellCommand) {
			this.shellCommand = Optional.of(shellCommand);
			return this;
		}

		/**
		 * Set the shell arguments.
		 * 
		 * @param shellArguments shell arguments 
		 * @return this for chaining
		 */
		public VirtualSessionPolicyBuilder withShellArguments(String... shellArguments) {
			return withShellArguments(Arrays.asList(shellArguments));
		}

		/**
		 * Set the shell arguments.
		 * 
		 * @param shellArguments shell arguments 
		 * @return this for chaining
		 */
		public VirtualSessionPolicyBuilder withShellArguments(Collection<String> shellArguments) {
			this.shellArguments.clear();
			return addShellArguments(shellArguments);
		}

		/**
		 * Add shell arguments.
		 * 
		 * @param shellArguments shell arguments to add 
		 * @return this for chaining
		 */
		public VirtualSessionPolicyBuilder addShellArguments(String... shellArguments) {
			return addShellArguments(Arrays.asList(shellArguments));
		}

		/**
		 * Add shell arguments.
		 * 
		 * @param shellArguments shell arguments to add 
		 * @return this for chaining
		 */
		public VirtualSessionPolicyBuilder addShellArguments(Collection<String> shellArguments) {
			this.shellArguments.addAll(shellArguments);
			return this;
		}

		/**
		 * Set the shell environment.
		 * 
		 * @param shellEnvironment shell environment 
		 * @return this for chaining
		 */
		public VirtualSessionPolicyBuilder withShellEnvironment(Map<String, String> shellEnvironment) {
			this.shellEnvironment.clear();
			return addShellEnvironment(shellEnvironment);
		}

		/**
		 * Add to the shell environment.
		 * 
		 * @param shellEnvironment shell environment 
		 * @return this for chaining
		 */
		public VirtualSessionPolicyBuilder addShellEnvironment(Map<String, String> shellEnvironment) {
			this.shellEnvironment.putAll(shellEnvironment);
			return this;
		}
		
		/**
		 * Configure this policy from an exists policy
		 * 
		 * @param policy policy
		 * @return this for chaining
		 */
		public VirtualSessionPolicyBuilder fromPolicy(VirtualSessionPolicy policy) {
			this.welcomeText = policy.welcomeText;
			this.shellCommand = policy.shellCommand;
			this.shellArguments.clear();
			this.shellArguments.addAll(policy.shellArguments);
			this.shellEnvironment.clear();
			this.shellEnvironment.putAll(policy.shellEnvironment);
			this.shellDirectory = policy.shellDirectory;
			return this;
		}
		
		/**
		 * Build a new {@link KeyExchangePolicy} given the configuration of this builder.
		 * 
		 * @return policy
		 */
		public VirtualSessionPolicy build() {
			return new VirtualSessionPolicy(this);
		}
	}

	/* TODO make all of these private + final, remove all deprecated setters at 3.3.x+ */
	private Supplier<String> welcomeText;
	private Optional<Supplier<String>> bannerText = Optional.empty();
	private Optional<String> shellCommand;
	private List<String> shellArguments;
	private Map<String, String> shellEnvironment;
	private Optional<Path> shellDirectory;
	private boolean disableBanner;

	/**
	 * Construct a new policy
	 */
	@Deprecated(since = "3.2.0", forRemoval = true)
	public VirtualSessionPolicy() {
		welcomeText = () -> "Maverick Synergy\r\nVirtual Shell ${version}";
		shellCommand = null;
		shellArguments = new ArrayList<>();
		shellEnvironment = new HashMap<>();
		shellDirectory = Optional.empty();
	}

	/**
	 * Construct a new policy
	 */
	@Deprecated(since = "3.2.0", forRemoval = true)
	public VirtualSessionPolicy(String welcomeText) {
		this();
		this.welcomeText = () -> welcomeText;
	}

	private VirtualSessionPolicy(VirtualSessionPolicyBuilder bldr) {
		welcomeText = bldr.welcomeText;
		shellCommand = bldr.shellCommand;
		shellArguments = Collections.unmodifiableList(new ArrayList<>(bldr.shellArguments));
		shellEnvironment = Collections.unmodifiableMap(new HashMap<>(bldr.shellEnvironment));
		shellDirectory = bldr.shellDirectory;
		disableBanner = bldr.disableBanner;
		bannerText = bldr.bannerText;
	}

	/**
	 * Get the welcome text 
	 * 
	 * @return welcome text
	 */
	public String getWelcomeText() {
		return welcomeText.get();
	}

	/**
	 * Set the welcome text 
	 * 
	 * @param welcomeText welcome text
	 * @deprecated will become immutable, use {@link VirtualSessionPolicyBuilder}.
	 */
	@Deprecated(since = "3.2.0", forRemoval = true)
	public void setWelcomeText(String welcomeText) {
		this.welcomeText = () -> welcomeText;
	}

	/**
	 * Get the shell command (OS dependent).
	 * 
	 * @return shell command
	 */
	public Optional<String> shellCommand() {
		return shellCommand;
	}

	/**
	 * Get the shell command (OS dependent).
	 * 
	 * @return shell command
	 * @deprecated see {@link #shellDirectory()}.
	 */
	public String getShellCommand() {
		return shellCommand.orElse(null);
	}

	/**
	 * Get the shell command arguments
	 * 
	 * @return shell command arguments
	 */
	public Collection<String> getShellArguments() {
		return shellArguments;
	}

	/**
	 * Set the shell command (OS dependent).
	 * 
	 * @param shellCommand shell command
	 * @deprecated will become immutable, use {@link VirtualSessionPolicyBuilder}.
	 */
	@Deprecated(since = "3.2.0", forRemoval = true)
	public void setShellCommand(String shellCommand) {
		this.shellCommand = Optional.ofNullable(shellCommand);
	}

	/**
	 * Get the environment variables available to the shell.
	 * 
	 * @return shell environment
	 */
	public Map<String, String> getShellEnvironment() {
		return shellEnvironment;
	}

	/**
	 * Set the environment variables available to the shell.
	 * 
	 * @param shellEnvironment shell environment
	 * @deprecated will become immutable, use {@link VirtualSessionPolicyBuilder}.
	 */
	@Deprecated(since = "3.2.0", forRemoval = true)
	public void setShellEnvironment(Map<String, String> shellEnvironment) {
		this.shellEnvironment = shellEnvironment;
	}

	/**
	 * Get the directory the shell starts in
	 * 
	 * @return shell directory or null if none
	 */
	public Optional<Path> shellDirectory() {
		return shellDirectory;
	}

	/**
	 * Get the directory the shell starts in
	 * 
	 * @return shell directory or null if none
	 * @deprecated see {@link #shellDirectory()}.
	 */
	@Deprecated(since = "3.2.0", forRemoval = true)
	public File getShellDirectory() {
		return shellDirectory.map(Path::toFile).orElse(null);
	}

	/**
	 * Set the directory the shell starts in
	 * 
	 * @param shellDirectory shell directory
	 * @deprecated will become immutable, use {@link VirtualSessionPolicyBuilder}.
	 */
	@Deprecated(since = "3.2.0", forRemoval = true)
	public void setShellDirectory(File shellDirectory) {
		this.shellDirectory = shellDirectory == null ? Optional.empty() : Optional.of(shellDirectory.toPath());
	}

	/**
	 * Get the whether the banner is disabled
	 * 
	 * @return disable banner
	 */
	public boolean isDisableBanner() {
		return disableBanner;
	}

	/**
	 * Set the whether the banner is disabled
	 * 
	 * @param disableBanner disable banner
	 * @deprecated will become immutable, use {@link VirtualSessionPolicyBuilder}.
	 */
	@Deprecated(since = "3.2.0", forRemoval = true)
	public void setDisableBanner(boolean disableBanner) {
		this.disableBanner = disableBanner;
	}

	/**
	 * Set the shell arguments
	 * 
	 * @param bannerText banner text
	 * @deprecated will become immutable, use {@link VirtualSessionPolicyBuilder}.
	 */
	@Deprecated(since = "3.2.0", forRemoval = true)
	public void setShellArguments(List<String> shellArguments) {
		this.shellArguments = shellArguments;
	}

	/**
	 * Get the banner text
	 * 
	 * @return banner text
	 */
	public String getBannerText() {
		return bannerText.map(Supplier::get).orElse(null);
	}

	/**
	 * Set the whether the banner text
	 * 
	 * @param bannerText banner text
	 * @deprecated will become immutable, use {@link VirtualSessionPolicyBuilder}.
	 */
	@Deprecated(since = "3.2.0", forRemoval = true)
	public void setBannerText(String bannerText) {
		this.bannerText = Optional.of(() ->bannerText);
	}

	@Override
	public Class<? extends Policy> type() {
		return VirtualSessionPolicy.class;
	}
}

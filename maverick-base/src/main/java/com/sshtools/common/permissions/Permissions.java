package com.sshtools.common.permissions;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

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
 * 
 */

/**
 * TODO make abstract at 3.3.0+
 */
public class Permissions {
	
	@FunctionalInterface
	public interface PermissionCheck {
		<CTX> boolean check(CTX connection, Permission permission, Object... args);
	}
	
	public interface Permission {
		String name();

		@Deprecated(since = "3.2.0", forRemoval = true)
		int nativeMask();
	}
	
	protected static abstract class AbstractPermissionBuilder<P extends Permission, BLDR extends AbstractPermissionBuilder<P, BLDR>> {
		protected Set<Permission> permissions = new LinkedHashSet<>();
		private Optional<PermissionCheck> permissionCheck = Optional.empty();
		
		/**
		 * Plug-in an alternative mechanism for checking a permission. When not set,
		 * the default algorithm will simply check whether the provided permission
		 * is in the list.
		 * 
		 * @param check check
		 * @return this for chaining
		 */
		@SuppressWarnings("unchecked")
		public BLDR withPermissionCheck(PermissionCheck permissionCheck) {
			this.permissionCheck = Optional.of(permissionCheck);
			return (BLDR)this;
		}
		
		/**
		 * Set one or more required permissions (clears existing permissions).
		 * 
		 * @param permissions permissions to add
		 * @return this for chaining
		 */
		@SuppressWarnings("unchecked")
		public BLDR withPermissions(Collection<P> permissions) {
			this.permissions.clear();
			addPermissions(permissions);
			return (BLDR)this;
		}
		
		/**
		 * Set one or more required permissions (clears existing permissions).
		 * 
		 * @param permissions permissions to add
		 * @return this for chaining
		 */
		public BLDR withPermissions(@SuppressWarnings("unchecked") P... permissions) {
			return withPermissions(Set.of(permissions));
		}

		/**
		 * Add one or more required permissions.
		 * 
		 * @param permissions permissions to add
		 * @return this for chaining
		 */
		public BLDR addPermissions(@SuppressWarnings("unchecked") P... permissions) {
			return addPermissions(Set.of(permissions));
		}

		/**
		 * Add one or more required mechanisms.
		 * 
		 * @param mechanisms mechanisms to add
		 * @return this for chaining
		 */
		@SuppressWarnings("unchecked")
		public BLDR addPermissions(Collection<P> permissions) {
			this.permissions.addAll(permissions);
			return (BLDR)this;
		}

		/**
		 * Remove one or more required permissions.
		 * 
		 * @param permissions permissions to remove
		 * @return this for chaining
		 */
		public BLDR removePermissions(@SuppressWarnings("unchecked") P... permissions) {
			return removePermissions(Set.of(permissions));
		}

		/**
		 * Remove one or more required mechanisms.
		 * 
		 * @param mechanisms mechanisms to remove
		 * @return this for chaining
		 */
		@SuppressWarnings("unchecked")
		public BLDR removePermissions(Collection<P> permissions) {
			this.permissions.removeAll(permissions);
			return (BLDR)this;
		}
	}

	protected long permissions;
	private final Optional<PermissionCheck> permissionCheck;
	
	protected Permissions(AbstractPermissionBuilder<?, ?> bldr) {
		bldr.permissions.forEach(p -> permissions |= p.nativeMask());
		permissionCheck = bldr.permissionCheck;
	}

	
	/**
	 * Construct basic permission.
	 * 
	 * @deprecated Use concrete {@link AbstractPermissionBuilder}.
	 */
	public Permissions() {
		super();
		permissionCheck = Optional.empty();
	}
	
	/**
	 * Construct basic permission.
	 * 
	 * @param permission permission
	 * 
	 * @deprecated Use concrete {@link AbstractPermissionBuilder}.
	 */
	@Deprecated(since = "3.2.0", forRemoval = true)
	public Permissions(long permissions) {
		this.permissions = permissions;
		permissionCheck = Optional.empty();
	}
	
	/**
	 * @param permission
	 * @deprecated Use {@link AbstractPermissionBuilder}.
	 */
	@Deprecated(since = "3.2.0", forRemoval = true)
	public void add(int permission) {
		permissions |= permission;
	}

	/**
	 * @param permission
	 * @deprecated Use {@link AbstractPermissionBuilder}.
	 */
	@Deprecated(since = "3.2.0", forRemoval = true)
	public void remove(int permission) {
		permissions &= ~permission;
	}

	/**
	 * @param permission
	 */
	@Deprecated(since = "3.2.0", forRemoval = true)
	public boolean check(int permission) {
		return (permissions & permission) == permission;
	}

	
	/**
	 * Check whether any permission has been applied.
	 * 
	 * @param permission permission
	 * @return permission applied
	 */
	public final <CTX> boolean checkAny(CTX ctx, Permission[] permissions, Object... args) {
		for(var p : permissions) {
			if(check(ctx, p, args))
				return true;
		}
		return false;
	}
	
	/**
	 * Check whether this permission has been applied.
	 * 
	 * @param permission permission
	 * @return permission applied
	 */
	public final <CTX> boolean check(CTX ctx, Permission permission, Object... args) {
		if(permissionCheck.isPresent())
			return permissionCheck.get().check(ctx, permission, args);
		else
			return (permissions & permission.nativeMask()) != 0;
	}
	
	/**
	 * The type this permission set will be registered as.
	 * 
	 * @return type
	 */
	public Class<? extends Permissions> type() {
		return getClass();
	}

}

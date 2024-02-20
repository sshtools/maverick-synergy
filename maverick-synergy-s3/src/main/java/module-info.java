/*-
 * #%L
 * S3 File System
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

module com.sshtools.synergy.s3 {
  requires transitive com.sshtools.maverick.base;
  requires transitive software.amazon.awssdk.awscore;
  requires transitive software.amazon.awssdk.core;
  requires transitive software.amazon.awssdk.services.s3;
  requires transitive software.amazon.awssdk.utils;
  requires transitive software.amazon.awssdk.regions;
  requires transitive software.amazon.awssdk.auth;
  exports com.sshtools.synergy.s3;
}

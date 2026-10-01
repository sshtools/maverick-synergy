package com.sshtools.common.files.vfs.tests;

/*-
 * #%L
 * Virtual File System Tests
 * %%
 * Copyright (C) 2002 - 2026 JADAPTIVE Limited
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

import static org.junit.Assert.fail;
import static org.junit.Assert.assertEquals;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.Test;

import com.sshtools.common.files.ReadOnlyFileFactory;
import com.sshtools.common.files.direct.NioFileFactory.NioFileFactoryBuilder;
import com.sshtools.common.files.vfs.VirtualFile;
import com.sshtools.common.files.vfs.VirtualFileFactory;
import com.sshtools.common.files.vfs.VirtualMappedFile;
import com.sshtools.common.files.vfs.VirtualMountTemplate;

/**
 * Security regression tests for mount selection with non-canonical paths.
 *
 * These tests intentionally encode the secure behavior and are expected to fail
 * on vulnerable builds until canonicalization is fixed.
 */
public class VirtualFileFactoryCanonicalPathRegressionTests {

    @Test
    public void shouldDenyWriteForDoubleSlashVariant() throws Exception {
        assertResolvesToChildMount("//shared/x.txt");
    }

    @Test
    public void shouldDenyWriteForDotSegmentVariant() throws Exception {
        assertResolvesToChildMount("/./shared/x.txt");
    }

    @Test
    public void shouldDenyWriteForDotDotVariant() throws Exception {
        assertResolvesToChildMount("/../shared/x.txt");
    }

    @Test
    public void shouldDenyWriteForRelativeVariant() throws Exception {
        assertResolvesToChildMount("./shared/x.txt");
    }

    @Test
    public void shouldDenyWriteForBackslashVariant() throws Exception {
        assertResolvesToChildMount("/a\\..\\shared\\x.txt");
    }

    private void assertResolvesToChildMount(String virtualPath) throws Exception {
        Path parentRoot = Files.createTempDirectory("vfs-parent-");
        Path sharedRoot = Files.createDirectories(parentRoot.resolve("shared"));

        var parentFactory = NioFileFactoryBuilder.create()
                .withHome(parentRoot.toFile())
                .withoutSandbox()
                .build();

        var sharedFactory = new ReadOnlyFileFactory(
                NioFileFactoryBuilder.create()
                        .withHome(sharedRoot.toFile())
                        .withoutSandbox()
                        .build());

        VirtualFileFactory factory = new VirtualFileFactory(
                new VirtualMountTemplate("/", parentRoot.toString(), parentFactory, false),
                new VirtualMountTemplate("/shared", sharedRoot.toString(), sharedFactory, false));

        VirtualFile target = factory.getFile(virtualPath);

        if (!(target instanceof VirtualMappedFile)) {
            fail("Expected VirtualMappedFile for path variant: " + virtualPath);
        }

        VirtualMappedFile mapped = (VirtualMappedFile) target;
        assertEquals("/shared", mapped.getParentMount().getMount());
    }
}

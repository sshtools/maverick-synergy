/*
 *    _           _             _   _
 *   (_) __ _  __| | __ _ _ __ | |_(_)_   _____
 *   | |/ _` |/ _` |/ _` | '_ \| __| \ \ / / _ \
 *   | | (_| | (_| | (_| | |_) | |_| |\ V /  __/
 *  _/ |\__,_|\__,_|\__,_| .__/ \__|_| \_/ \___|
 * |__/                  |_|
 *
 * This file is part of the Maverick Synergy Hotfixes Java SSH API
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 *
 * Copyright (C) 2002-2025 JADAPTIVE Limited - All Rights Reserved
 *
 * Use of this software may also be covered by third-party licenses depending on the choices you make about what features to use.
 *
 * Please visit the link below to see additional third-party licenses and copyrights
 *
 * https://www.jadaptive.com/app/manpage/en/article/1565029/What-third-party-dependencies-does-the-Maverick-Synergy-API-have
 */
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.sshtools.common.files.direct.NioFileFactory;
import com.sshtools.common.files.direct.NioFileFactory.NioFileFactoryBuilder;
import com.sshtools.common.files.vfs.VirtualFileFactory;
import com.sshtools.common.files.vfs.VirtualMount;
import com.sshtools.common.files.vfs.VirtualMountTemplate;
import com.sshtools.common.permissions.PermissionDeniedException;

/**
 * Unit tests for {@link VirtualMount}.
 */
public class VirtualMountTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private NioFileFactory nioFactory;
    private VirtualFileFactory vff;
    private VirtualMount defaultMount;
    private File baseDir;

    @Before
    public void setUp() throws IOException, PermissionDeniedException {
        baseDir = tempFolder.getRoot();
        nioFactory = NioFileFactoryBuilder.create()
                .withHome(baseDir)
                .withoutSandbox()
                .build();

        // Use "/" as the root mount — using a sub-path like "/home" would cause
        // VirtualFileFactory.rebuildMountCache() to look up a parent mount for "/" 
        // which does not exist, throwing FileNotFoundException.
        VirtualMountTemplate rootTemplate = new VirtualMountTemplate(
                "/", baseDir.getAbsolutePath(), nioFactory, false);
        vff = new VirtualFileFactory(rootTemplate);
        defaultMount = vff.getDefaultMount();
    }

    // ------------------------------------------------------------------
    // Cached
    // ------------------------------------------------------------------

    @Test
    public void isCached_defaultFalse() {
        assertFalse(defaultMount.isCached());
    }

    @Test
    public void setCached_true_isCachedTrue() {
        defaultMount.setCached(true);
        assertTrue(defaultMount.isCached());
    }

    @Test
    public void setCached_false_isCachedFalse() {
        defaultMount.setCached(true);
        defaultMount.setCached(false);
        assertFalse(defaultMount.isCached());
    }

    // ------------------------------------------------------------------
    // ReadOnly
    // ------------------------------------------------------------------

    @Test
    public void isReadOnly_defaultFalse() {
        assertFalse(defaultMount.isReadOnly());
    }

    @Test
    public void setReadOnly_true_isReadOnlyTrue() {
        defaultMount.setReadOnly(true);
        assertTrue(defaultMount.isReadOnly());
    }

    @Test
    public void setReadOnly_false_isReadOnlyFalse() {
        defaultMount.setReadOnly(true);
        defaultMount.setReadOnly(false);
        assertFalse(defaultMount.isReadOnly());
    }

    // ------------------------------------------------------------------
    // CreateMountFolder
    // ------------------------------------------------------------------

    @Test
    public void isCreateMountFolder_false_whenConstructedWithFalse() {
        assertFalse(defaultMount.isCreateMountFolder());
    }

    @Test
    public void isCreateMountFolder_true_whenConstructedWithTrue()
            throws IOException, PermissionDeniedException {
        File subDir = tempFolder.newFolder("mntB");
        VirtualMountTemplate tmpl = new VirtualMountTemplate(
                "/", subDir.getAbsolutePath(), nioFactory, true);
        VirtualFileFactory f2 = new VirtualFileFactory(tmpl);
        assertTrue(f2.getDefaultMount().isCreateMountFolder());
    }

    // ------------------------------------------------------------------
    // LastModified
    // ------------------------------------------------------------------

    @Test
    public void lastModified_defaultZero() {
        assertEquals(0L, defaultMount.lastModified());
    }

    @Test
    public void setLastModified_changesValue() {
        defaultMount.setLastModified(1234567890L);
        assertEquals(1234567890L, defaultMount.lastModified());
    }

    // ------------------------------------------------------------------
    // getTemplate
    // ------------------------------------------------------------------

    @Test
    public void getTemplate_returnsNonNull() {
        assertNotNull(defaultMount.getTemplate());
    }

    @Test
    public void getTemplate_mountPathMatchesTemplate() {
        assertEquals("/", defaultMount.getTemplate().getMount());
    }

    // ------------------------------------------------------------------
    // getMount (virtual path)
    // ------------------------------------------------------------------

    @Test
    public void getMount_returnsVirtualPath() {
        assertEquals("/", defaultMount.getMount());
    }

    // ------------------------------------------------------------------
    // isParentOf / isChildOf
    // ------------------------------------------------------------------

    @Test
    public void isParentOf_trueWhenDirectParent()
            throws IOException, PermissionDeniedException {
        VirtualMountTemplate parentTmpl = new VirtualMountTemplate(
                "/home", baseDir.getAbsolutePath(), nioFactory, false);
        VirtualMountTemplate childTmpl = new VirtualMountTemplate(
                "/home/user", baseDir.getAbsolutePath(), nioFactory, false);
        VirtualMount parentMount = new VirtualMount(parentTmpl, vff, nioFactory, false, 0L);
        VirtualMount childMount  = new VirtualMount(childTmpl, vff, nioFactory, false, 0L);
        assertTrue(parentMount.isParentOf(childMount));
    }

    @Test
    public void isChildOf_trueWhenDirectChild()
            throws IOException, PermissionDeniedException {
        VirtualMountTemplate parentTmpl = new VirtualMountTemplate(
                "/home", baseDir.getAbsolutePath(), nioFactory, false);
        VirtualMountTemplate childTmpl = new VirtualMountTemplate(
                "/home/user", baseDir.getAbsolutePath(), nioFactory, false);
        VirtualMount parentMount = new VirtualMount(parentTmpl, vff, nioFactory, false, 0L);
        VirtualMount childMount  = new VirtualMount(childTmpl, vff, nioFactory, false, 0L);
        assertTrue(childMount.isChildOf(parentMount));
    }

    @Test
    public void isParentOf_falseForUnrelatedMount()
            throws IOException, PermissionDeniedException {
        VirtualMountTemplate homeTmpl = new VirtualMountTemplate(
                "/home", baseDir.getAbsolutePath(), nioFactory, false);
        VirtualMountTemplate varTmpl = new VirtualMountTemplate(
                "/var", baseDir.getAbsolutePath(), nioFactory, false);
        VirtualMount homeMount = new VirtualMount(homeTmpl, vff, nioFactory, false, 0L);
        VirtualMount varMount  = new VirtualMount(varTmpl, vff, nioFactory, false, 0L);
        assertFalse(homeMount.isParentOf(varMount));
    }

    // ------------------------------------------------------------------
    // getResolvePath
    // ------------------------------------------------------------------

    @Test
    public void getResolvePath_childPath_appendsRelativePart()
            throws IOException, PermissionDeniedException {
        VirtualMountTemplate tmpl = new VirtualMountTemplate(
                "/home", baseDir.getAbsolutePath(), nioFactory, false);
        VirtualMount mount = new VirtualMount(tmpl, vff, nioFactory, false, 0L);
        String resolved = mount.getResolvePath("/home/docs");
        assertTrue("Resolved path should start with real root",
                resolved.startsWith(mount.getRoot()));
        assertTrue("Resolved path should contain 'docs'",
                resolved.contains("docs"));
    }

    @Test
    public void getResolvePath_exactMount_returnsRealPath()
            throws IOException, PermissionDeniedException {
        VirtualMountTemplate tmpl = new VirtualMountTemplate(
                "/home", baseDir.getAbsolutePath(), nioFactory, false);
        VirtualMount mount = new VirtualMount(tmpl, vff, nioFactory, false, 0L);
        String resolved = mount.getResolvePath("/home");
        // getResolvePath adds trailing slash when path == mount; startsWith is safe
        assertTrue("Resolved path should start with real root",
                resolved.startsWith(mount.getRoot()));
    }
}

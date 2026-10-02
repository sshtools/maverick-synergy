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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.sshtools.common.files.AbstractFile;
import com.sshtools.common.files.AbstractFileFactory;
import com.sshtools.common.files.direct.NioFileFactory;
import com.sshtools.common.files.direct.NioFileFactory.NioFileFactoryBuilder;
import com.sshtools.common.files.vfs.VirtualFile;
import com.sshtools.common.files.vfs.VirtualFileFactory;
import com.sshtools.common.files.vfs.VirtualMappedFile;
import com.sshtools.common.files.vfs.VirtualMount;
import com.sshtools.common.files.vfs.VirtualMountTemplate;
import com.sshtools.common.permissions.PermissionDeniedException;

public class VfsWhitespaceAndGuardRegressionTests {

    private Path parentRoot;
    private Path childRoot;
    private RecordingFactory parentRecorder;
    private RecordingFactory childRecorder;
    private VirtualFileFactory factory;

    @Before
    public void setup() throws Exception {
        parentRoot = Files.createTempDirectory("vfs-ws-parent-");
        childRoot = Files.createTempDirectory("vfs-ws-child-");

        parentRecorder = new RecordingFactory(nio(parentRoot));
        childRecorder = new RecordingFactory(nio(childRoot));

        factory = new VirtualFileFactory(
                new VirtualMountTemplate("/", parentRoot.toString(), parentRecorder, false),
                new VirtualMountTemplate("/child", childRoot.toString(), childRecorder, false));
    }

    @Test
    public void whitespaceInvariant_exactDotDotHasParentSemantics() throws Exception {
        parentRecorder.clear();
        childRecorder.clear();

        VirtualFile file = factory.getFile("/child/..");
        assertEquals("/", file.getMount().getMount());
        assertEquals("/", factory.getMount("/child/..").getMount());
    }

    @Test
    public void whitespaceInvariant_dotDotSpaceIsLiteralAndMustStayOnChildMount() throws Exception {
        parentRecorder.clear();
        childRecorder.clear();

        VirtualFile file = factory.getFile("/child/.. ");
        assertTrue("Expected mapped file for literal '.. ' component", file instanceof VirtualMappedFile);
        VirtualMappedFile mapped = (VirtualMappedFile) file;
        assertEquals("/child", mapped.getParentMount().getMount());
        assertEquals("/child", factory.getMount("/child/.. ").getMount());
        assertAnyRequestedPathContains(childRecorder.requestedPaths, ".. ");
    }

    @Test
    public void whitespaceInvariant_dotDotDoubleSpaceIsLiteralAndMustStayOnChildMount() throws Exception {
        parentRecorder.clear();
        childRecorder.clear();

        VirtualFile file = factory.getFile("/child/..  ");
        assertTrue("Expected mapped file for literal '..  ' component", file instanceof VirtualMappedFile);
        VirtualMappedFile mapped = (VirtualMappedFile) file;
        assertEquals("/child", mapped.getParentMount().getMount());
        assertEquals("/child", factory.getMount("/child/..  ").getMount());
        assertAnyRequestedPathContains(childRecorder.requestedPaths, "..  ");
    }

    @Test
    public void whitespaceInvariant_dotSpaceIsLiteralAndMustStayOnChildMount() throws Exception {
        parentRecorder.clear();
        childRecorder.clear();

        VirtualFile file = factory.getFile("/child/. ");
        assertTrue("Expected mapped file for literal '. ' component", file instanceof VirtualMappedFile);
        VirtualMappedFile mapped = (VirtualMappedFile) file;
        assertEquals("/child", mapped.getParentMount().getMount());
        assertEquals("/child", factory.getMount("/child/. ").getMount());
        assertAnyRequestedPathContains(childRecorder.requestedPaths, ". ");
    }

    @Test
    public void toActualPathGuard_acceptsInMountRoot() throws Exception {
        VirtualMount childMount = childMount();
        new VirtualMappedFile("/child", childMount, factory);
    }

    @Test
    public void toActualPathGuard_acceptsInMountChild() throws Exception {
        VirtualMount childMount = childMount();
        new VirtualMappedFile("/child/x", childMount, factory);
    }

    @Test
    public void toActualPathGuard_acceptsInMountNestedChild() throws Exception {
        VirtualMount childMount = childMount();
        new VirtualMappedFile("/child/sub/x", childMount, factory);
    }

    @Test
    public void toActualPathGuard_rejectsOutOfMountRoot() throws Exception {
        assertRejectedForChildMount("/");
    }

    @Test
    public void toActualPathGuard_rejectsOutOfMountOther() throws Exception {
        assertRejectedForChildMount("/other");
    }

    @Test
    public void toActualPathGuard_rejectsOutOfMountOtherChild() throws Exception {
        assertRejectedForChildMount("/other/x");
    }

    @Test
    public void toActualPathGuard_rejectsMountPrefixBoundaryChildish() throws Exception {
        assertRejectedForChildMount("/childish/x");
    }

    @Test
    public void toActualPathGuard_rejectsCanonicalizedOutOfMountTraversal() throws Exception {
        assertRejectedForChildMount("/child/../other/x");
    }

    private void assertRejectedForChildMount(String virtualPath) throws Exception {
        VirtualMount childMount = childMount();
        try {
            new VirtualMappedFile(virtualPath, childMount, factory);
            fail("Expected FileNotFoundException for out-of-mount path: " + virtualPath);
        }
        catch (FileNotFoundException expected) {
            assertTrue(expected.getMessage().contains("could not be found")
                    || expected.getMessage().contains("Path"));
        }
    }

    private VirtualMount childMount() throws Exception {
        VirtualFile file = factory.getFile("/child/anchor");
        assertTrue("Expected mapped file for '/child/anchor'", file instanceof VirtualMappedFile);
        return ((VirtualMappedFile) file).getParentMount();
    }

    private static NioFileFactory nio(Path home) {
        return NioFileFactoryBuilder.create().withHome(home.toFile()).withoutSandbox().build();
    }

    private static void assertAnyRequestedPathContains(List<String> requested, String fragment) {
        for (String p : requested) {
            if (p.contains(fragment)) {
                return;
            }
        }
        fail("Expected at least one backing-store request path containing [" + fragment + "] but got " + requested);
    }

    private static class RecordingFactory implements AbstractFileFactory<AbstractFile> {

        private final NioFileFactory delegate;
        private final List<String> requestedPaths = new ArrayList<>();

        RecordingFactory(NioFileFactory delegate) {
            this.delegate = delegate;
        }

        @Override
        public AbstractFile getFile(String path) throws PermissionDeniedException, IOException {
            requestedPaths.add(path);
            return delegate.getFile(path);
        }

        void clear() {
            requestedPaths.clear();
        }
    }
}

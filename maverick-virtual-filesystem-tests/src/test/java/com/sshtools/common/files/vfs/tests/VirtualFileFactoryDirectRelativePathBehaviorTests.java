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

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.Before;
import org.junit.Test;

import com.sshtools.common.files.direct.NioFileFactory;
import com.sshtools.common.files.direct.NioFileFactory.NioFileFactoryBuilder;
import com.sshtools.common.files.vfs.VirtualFile;
import com.sshtools.common.files.vfs.VirtualFileFactory;
import com.sshtools.common.files.vfs.VirtualMappedFile;
import com.sshtools.common.files.vfs.VirtualMountTemplate;

public class VirtualFileFactoryDirectRelativePathBehaviorTests {

    private Path defaultRoot;
    private Path rootMountRoot;
    private Path xMountRoot;
    private String defaultRootStr;
    private String rootMountRootStr;
    private String xMountRootStr;
    private VirtualFileFactory factory;

    @Before
    public void setup() throws Exception {
        defaultRoot = Files.createTempDirectory("vfs-direct-default-");
        rootMountRoot = Files.createTempDirectory("vfs-direct-root-");
        xMountRoot = Files.createTempDirectory("vfs-direct-x-");

        defaultRootStr = defaultRoot.toString().replace('\\', '/');
        rootMountRootStr = rootMountRoot.toString().replace('\\', '/');
        xMountRootStr = xMountRoot.toString().replace('\\', '/');

        factory = new VirtualFileFactory(
                new VirtualMountTemplate("/a/b", defaultRootStr, nio(defaultRoot), false),
                new VirtualMountTemplate("/", rootMountRootStr, nio(rootMountRoot), false),
                new VirtualMountTemplate("/x", xMountRootStr, nio(xMountRoot), false));
    }

    @Test
    public void directRelativeInputs_areResolvedByVirtualFileFactoryBoundary() throws Exception {
        System.out.println("==== DIRECT VFS RELATIVE-PATH DIAGNOSTIC ====");
        System.out.println("MOUNT default=/a/b root=<DEFAULT_ROOT>");
        System.out.println("MOUNT /=<ROOT_MOUNT_ROOT>");
        System.out.println("MOUNT /x=<X_MOUNT_ROOT>");

        assertCase("../x", "/a/b/x", "/a/b", "/a/b/x", "<DEFAULT_ROOT>/x");
        assertCase("./x", "/a/b/x", "/a/b", "/a/b/x", "<DEFAULT_ROOT>/x");
        assertCase("x", "/a/b/x", "/a/b", "/a/b/x", "<DEFAULT_ROOT>/x");
        assertCase("x/../y", "/a/b/y", "/a/b", "/a/b/y", "<DEFAULT_ROOT>/y");
        assertCase("../../x", "/a/b/x", "/a/b", "/a/b/x", "<DEFAULT_ROOT>/x");
        assertCase("x/../../y", "/a/b/y", "/a/b", "/a/b/y", "<DEFAULT_ROOT>/y");
        assertCase("x/y/../../../z", "/a/b/z", "/a/b", "/a/b/z", "<DEFAULT_ROOT>/z");
        assertCase("../../../../../../x", "/a/b/x", "/a/b", "/a/b/x", "<DEFAULT_ROOT>/x");
    }

    @Test
    public void directBoundary_defaultMountAB_getFileDotDotX_resolvesToABX() throws Exception {
        VirtualFile vf = factory.getFile("../x");
        assertTrue("Expected VirtualMappedFile for '../x'", vf instanceof VirtualMappedFile);
        VirtualMappedFile mapped = (VirtualMappedFile) vf;

        assertEquals("/a/b", mapped.getParentMount().getMount());
        assertEquals("/a/b/x", mapped.getAbsolutePath());
        assertEquals("<DEFAULT_ROOT>/x", normalize(mapped.resolveFile().getAbsolutePath()));
    }

    private void assertCase(String input,
            String expectedCanonical,
            String expectedMount,
            String expectedVirtualPath,
            String expectedBackingPath) throws Exception {

        String canonical = invokeCanonicalisePath(factory, input);
        VirtualFile vf = factory.getFile(input);
        assertTrue("Expected VirtualMappedFile for input " + input + " but got " + vf.getClass().getName(),
                vf instanceof VirtualMappedFile);

        VirtualMappedFile mapped = (VirtualMappedFile) vf;
        String mount = mapped.getParentMount().getMount();
        String virtualPath = mapped.getAbsolutePath();
        String backingPath = normalize(mapped.resolveFile().getAbsolutePath());

        assertEquals(expectedCanonical, canonical);
        assertEquals(expectedMount, mount);
        assertEquals(expectedVirtualPath, virtualPath);
        assertEquals(expectedBackingPath, backingPath);

        System.out.println("CASE=" + printable(input));
        System.out.println("  originalInput=" + printable(input));
        System.out.println("  canonicalisePath(input)=" + printable(canonical));
        System.out.println("  getFileReturnedType=" + vf.getClass().getSimpleName());
        System.out.println("  selectedVirtualMount=" + mount);
        System.out.println("  resultingVirtualPath=" + virtualPath);
        System.out.println("  resultingBackingPath=" + backingPath);
    }

    private static String invokeCanonicalisePath(VirtualFileFactory factory, String input) throws Exception {
        Method m = VirtualFileFactory.class.getDeclaredMethod("canonicalisePath", String.class);
        m.setAccessible(true);
        return (String) m.invoke(factory, input);
    }

    private NioFileFactory nio(Path home) {
        return NioFileFactoryBuilder.create().withHome(home.toFile()).withoutSandbox().build();
    }

    private String normalize(String value) {
        return value.replace('\\', '/')
                .replace(defaultRootStr, "<DEFAULT_ROOT>")
                .replace(rootMountRootStr, "<ROOT_MOUNT_ROOT>")
                .replace(xMountRootStr, "<X_MOUNT_ROOT>");
    }

    private static String printable(String value) {
        return value.replace(" ", "[SPACE]");
    }
}
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
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import org.junit.Assume;
import org.junit.Test;

import com.sshtools.common.files.AbstractFile;
import com.sshtools.common.files.ReadOnlyFileFactory;
import com.sshtools.common.files.direct.NioFileFactory;
import com.sshtools.common.files.direct.NioFileFactory.NioFileFactoryBuilder;
import com.sshtools.common.files.vfs.VirtualFile;
import com.sshtools.common.files.vfs.VirtualFileFactory;
import com.sshtools.common.files.vfs.VirtualMappedFile;
import com.sshtools.common.files.vfs.VirtualMountTemplate;

public class VfsSecurityArchitectureBehaviorTests {

    @Test
    public void relativePath_contract_anchorAndExcessTraversal() throws Exception {
        Path defaultRoot = Files.createTempDirectory("vfs-default-root-");
        Path otherRoot = Files.createTempDirectory("vfs-other-root-");

        VirtualFileFactory factory = new VirtualFileFactory(
                new VirtualMountTemplate("/a/b", defaultRoot.toString(), nio(defaultRoot, false), false),
            new VirtualMountTemplate("/", defaultRoot.toString(), nio(defaultRoot, false), false),
                new VirtualMountTemplate("/x", otherRoot.toString(), nio(otherRoot, false), false));

        assertRelativeResult(factory, "../x", "/a/b/x", "/a/b");
        assertRelativeResult(factory, "./x", "/a/b/x", "/a/b");
        assertRelativeResult(factory, "x/../y", "/a/b/y", "/a/b");
        assertRelativeResult(factory, "../../x", "/a/b/x", "/a/b");
        assertRelativeResult(factory, "x/../../y", "/a/b/y", "/a/b");
        assertRelativeResult(factory, "x/y/../../../z", "/a/b/z", "/a/b");
        assertRelativeResult(factory, "../../../../../../x", "/a/b/x", "/a/b");
    }

    @Test
    public void caseSensitiveRouting_nonOverlappingBackingStores() throws Exception {
        Path parentRoot = Files.createTempDirectory("vfs-parent-");
        Path sharedRoot = Files.createTempDirectory("vfs-shared-");

        Files.writeString(sharedRoot.resolve("x.txt"), "shared-content", StandardCharsets.UTF_8);
        Files.createDirectories(parentRoot.resolve("Shared"));
        Files.writeString(parentRoot.resolve("Shared").resolve("x.txt"), "parent-content", StandardCharsets.UTF_8);

        VirtualFileFactory factory = new VirtualFileFactory(
                new VirtualMountTemplate("/", parentRoot.toString(), nio(parentRoot, false), false),
                new VirtualMountTemplate("/shared", sharedRoot.toString(), nio(sharedRoot, false), false));

        VirtualMappedFile lower = asMapped(factory.getFile("/shared/x.txt"));
        assertEquals("/shared", lower.getParentMount().getMount());
        assertEquals("shared-content", readString(lower.resolveFile()));

        VirtualMappedFile mixed = asMapped(factory.getFile("/Shared/x.txt"));
        assertEquals("/", mixed.getParentMount().getMount());

        String physical = normalize(mixed.resolveFile().getAbsolutePath());
        assertTrue("Expected parent-root backing path", physical.startsWith(normalize(parentRoot.toString())));
        assertFalse("Non-overlapping shared root must not be selected via mount routing",
                physical.startsWith(normalize(sharedRoot.toString())));
        assertEquals("parent-content", readString(mixed.resolveFile()));
    }

    @Test
    public void caseSensitiveRouting_overlappingStore_virtualMountSelectionStaysRoot() throws Exception {
        Path parentRoot = Files.createTempDirectory("vfs-parent-overlap-");
        Path childLower = Files.createDirectories(parentRoot.resolve("shared"));
        Files.writeString(childLower.resolve("x.txt"), "child-data", StandardCharsets.UTF_8);

        VirtualFileFactory factory = new VirtualFileFactory(
                new VirtualMountTemplate("/", parentRoot.toString(), nio(parentRoot, false), false),
                new VirtualMountTemplate("/shared", childLower.toString(), nio(childLower, false), false));

        VirtualMappedFile mixed = asMapped(factory.getFile("/Shared/x.txt"));
        assertEquals("/", mixed.getParentMount().getMount());

        boolean aliasVisible = Files.exists(parentRoot.resolve("Shared").resolve("x.txt"));
        if (aliasVisible) {
            assertEquals("child-data", readString(mixed.resolveFile()));
            assertTrue(normalize(mixed.resolveFile().getAbsolutePath())
                    .startsWith(normalize(parentRoot.resolve("Shared").toString())));
        }
        else {
            Assume.assumeTrue("Physical aliasing through case-folding is not available on this filesystem", false);
        }
    }

    @Test
    public void symlinkContainment_nioFactory_existingSymlink_sandboxOn() throws Exception {
        assumeSymlinkSupported();

        Path sandboxRoot = Files.createTempDirectory("vfs-sandbox-on-");
        Path outsideRoot = Files.createTempDirectory("vfs-outside-on-");
        Files.writeString(outsideRoot.resolve("secret.txt"), "outside-secret", StandardCharsets.UTF_8);

        createDirectorySymlink(sandboxRoot.resolve("link"), outsideRoot);

        NioFileFactory sandboxed = nio(sandboxRoot, true);
        AbstractFile file = sandboxed.getFile("link/secret.txt");

        assertEquals("outside-secret", readString(file));
    }

    @Test
    public void symlinkContainment_nioFactory_existingSymlink_sandboxOff() throws Exception {
        assumeSymlinkSupported();

        Path sandboxRoot = Files.createTempDirectory("vfs-sandbox-off-");
        Path outsideRoot = Files.createTempDirectory("vfs-outside-off-");
        Files.writeString(outsideRoot.resolve("secret.txt"), "outside-secret", StandardCharsets.UTF_8);

        createDirectorySymlink(sandboxRoot.resolve("link"), outsideRoot);

        NioFileFactory notSandboxed = nio(sandboxRoot, false);
        AbstractFile file = notSandboxed.getFile("link/secret.txt");

        assertEquals("outside-secret", readString(file));
    }

    @Test
    public void symlinkCreationViaApi_nioFactory_sandboxOn() throws Exception {
        assumeSymlinkSupported();

        Path sandboxRoot = Files.createTempDirectory("vfs-api-link-on-");
        Path outsideRoot = Files.createTempDirectory("vfs-api-target-on-");
        Files.writeString(outsideRoot.resolve("secret.txt"), "outside-secret", StandardCharsets.UTF_8);

        NioFileFactory sandboxed = nio(sandboxRoot, true);
        AbstractFile link = sandboxed.getFile("api-link");

        boolean denied = false;
        try {
            link.symlinkFrom(outsideRoot.toString());
        }
        catch (Exception ex) {
            denied = true;
        }

        assertTrue("Sandboxed API symlink creation to outside absolute target should be denied", denied);
    }

    @Test
    public void symlinkCreationViaApi_nioFactory_sandboxOff() throws Exception {
        assumeSymlinkSupported();

        Path sandboxRoot = Files.createTempDirectory("vfs-api-link-off-");
        Path outsideRoot = Files.createTempDirectory("vfs-api-target-off-");
        Files.writeString(outsideRoot.resolve("secret.txt"), "outside-secret", StandardCharsets.UTF_8);

        NioFileFactory notSandboxed = nio(sandboxRoot, false);
        AbstractFile link = notSandboxed.getFile("api-link");
        link.symlinkFrom(outsideRoot.toString());

        assertEquals("outside-secret", readString(notSandboxed.getFile("api-link/secret.txt")));
    }

    @Test
    public void vfsSymlinkRouting_withAndWithoutChildMount_behaviourComparison() throws Exception {
        assumeSymlinkSupported();

        Path parentRoot = Files.createTempDirectory("vfs-parent-only-");
        Path outsideRoot = Files.createTempDirectory("vfs-outside-only-");
        Files.writeString(outsideRoot.resolve("secret.txt"), "outside-secret", StandardCharsets.UTF_8);
        createDirectorySymlink(parentRoot.resolve("link"), outsideRoot);

        VirtualFileFactory rootOnly = new VirtualFileFactory(
                new VirtualMountTemplate("/", parentRoot.toString(), nio(parentRoot, false), false));

        VirtualMappedFile rootOnlyLink = asMapped(rootOnly.getFile("/link/secret.txt"));
        assertEquals("/", rootOnlyLink.getParentMount().getMount());
        String rootOnlyContent = readString(rootOnlyLink.resolveFile());

        VirtualFileFactory withChild = new VirtualFileFactory(
                new VirtualMountTemplate("/", parentRoot.toString(), nio(parentRoot, false), false),
                new VirtualMountTemplate("/shared", outsideRoot.toString(), nio(outsideRoot, false), false));

        VirtualMappedFile withChildLink = asMapped(withChild.getFile("/link/secret.txt"));
        assertEquals("/", withChildLink.getParentMount().getMount());
        String withChildContent = readString(withChildLink.resolveFile());

        assertEquals(rootOnlyContent, withChildContent);
        assertEquals("outside-secret", withChildContent);
    }

    @Test
    public void reporterScenario_symlinkIntoChild_withAndWithoutSharedMount_identicalResolution() throws Exception {
        assumeSymlinkSupported();

        Path parentRoot = Files.createTempDirectory("vfs-parent-reporter-");
        Path childRoot = Files.createTempDirectory("vfs-child-reporter-");
        Files.writeString(childRoot.resolve("x.txt"), "child-data", StandardCharsets.UTF_8);
        createDirectorySymlink(parentRoot.resolve("link"), childRoot);

        VirtualFileFactory withoutShared = new VirtualFileFactory(
                new VirtualMountTemplate("/", parentRoot.toString(), nio(parentRoot, false), false));
        VirtualMappedFile noShared = asMapped(withoutShared.getFile("/link/x.txt"));
        assertEquals("/", noShared.getParentMount().getMount());
        String noSharedContent = readString(noShared.resolveFile());

        VirtualFileFactory withShared = new VirtualFileFactory(
                new VirtualMountTemplate("/", parentRoot.toString(), nio(parentRoot, false), false),
                new VirtualMountTemplate("/shared", childRoot.toString(),
                        new ReadOnlyFileFactory(nio(childRoot, false)), false));
        VirtualMappedFile withSharedFile = asMapped(withShared.getFile("/link/x.txt"));
        assertEquals("/", withSharedFile.getParentMount().getMount());
        String withSharedContent = readString(withSharedFile.resolveFile());

        assertEquals("child-data", noSharedContent);
        assertEquals(noSharedContent, withSharedContent);
    }

    private static void assertRelativeResult(VirtualFileFactory factory, String input,
            String expectedVirtualPath, String expectedMount) throws Exception {
        VirtualMappedFile mapped = asMapped(factory.getFile(input));
        assertEquals(expectedVirtualPath, mapped.getAbsolutePath());
        assertEquals(expectedMount, mapped.getParentMount().getMount());
    }

    private static NioFileFactory nio(Path home, boolean sandbox) {
        NioFileFactoryBuilder builder = NioFileFactoryBuilder.create().withHome(home.toFile());
        if (!sandbox) {
            builder.withoutSandbox();
        }
        return builder.build();
    }

    private static VirtualMappedFile asMapped(VirtualFile file) {
        assertTrue("Expected VirtualMappedFile but got " + file.getClass().getName(), file instanceof VirtualMappedFile);
        return (VirtualMappedFile) file;
    }

    private static String readString(AbstractFile file) throws Exception {
        try (InputStream in = file.getInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static String normalize(String p) {
        return p.replace('\\', '/');
    }

    private static void assumeSymlinkSupported() {
        Path base = null;
        try {
            base = Files.createTempDirectory("vfs-symlink-check-");
            Path target = Files.createDirectory(base.resolve("target-" + UUID.randomUUID()));
            Path link = base.resolve("link-" + UUID.randomUUID());
            Files.createSymbolicLink(link, target);
            Assume.assumeTrue(Files.isSymbolicLink(link));
        }
        catch (UnsupportedOperationException | SecurityException | IOException ex) {
            Assume.assumeNoException("Symlink operations are unavailable on this host", ex);
        }
    }

    private static void createDirectorySymlink(Path link, Path target) throws Exception {
        try {
            Files.createSymbolicLink(link, target);
        }
        catch (FileAlreadyExistsException ex) {
            Files.delete(link);
            Files.createSymbolicLink(link, target);
        }
    }
}

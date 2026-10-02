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

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
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
import com.sshtools.common.files.vfs.VirtualMountFile;
import com.sshtools.common.files.vfs.VirtualMountTemplate;
import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.util.FileUtils;

public class VfsResolutionDiagnosticsTest {

    private Path parentRoot;
    private Path childRoot;
    private String parentRootStr;
    private String childRootStr;
    private RecordingFactory parentRecorder;
    private RecordingFactory childRecorder;
    private VirtualFileFactory factory;

    @Before
    public void setup() throws Exception {
        parentRoot = Files.createTempDirectory("vfs-diag-parent-");
        childRoot = Files.createTempDirectory("vfs-diag-child-");
        parentRootStr = parentRoot.toString().replace('\\', '/');
        childRootStr = childRoot.toString().replace('\\', '/');

        parentRecorder = new RecordingFactory(nio(parentRoot));
        childRecorder = new RecordingFactory(nio(childRoot));

        factory = new VirtualFileFactory(
                new VirtualMountTemplate("/", parentRoot.toString(), parentRecorder, false),
                new VirtualMountTemplate("/child", childRoot.toString(), childRecorder, false));
    }

    @Test
    public void traceWhitespaceCasesThroughFullFactoryFlow() throws Exception {
        System.out.println("==== VFS DIAGNOSTIC: WHITESPACE CASES (FULL FLOW) ====");
        System.out.println("TOKENS: PARENT_ROOT=" + parentRootStr);
        System.out.println("TOKENS: CHILD_ROOT=" + childRootStr);

        traceFactoryFlowCase("/child/..");
        traceFactoryFlowCase("/child/.. ");
        traceFactoryFlowCase("/child/..  ");
        traceFactoryFlowCase("/child/. ");
    }

    @Test
    public void traceToActualPathGuardCasesForChildMountMappedFile() throws Exception {
        System.out.println("==== VFS DIAGNOSTIC: toActualPath GUARD CASES (DIRECT VirtualMappedFile) ====");
        System.out.println("TOKENS: PARENT_ROOT=" + parentRootStr);
        System.out.println("TOKENS: CHILD_ROOT=" + childRootStr);

        VirtualMappedFile childMapped = childMappedAnchor();
        VirtualMount childMount = childMapped.getParentMount();
        String securemount = childMount.getRoot();
        String canonical2Raw = invokeStaticCanonicalise(securemount);
        String canonical2Operand = ensureTrailingSlash(canonical2Raw);

        String[] cases = new String[] {
                "/child",
                "/child/x",
                "/child/sub/x",
                "/",
                "/other",
                "/other/x",
                "/childish/x",
                "/child/../other/x"
        };

        for (String input : cases) {
            String candidate = expectedToActualCandidate(input, childMount);
            String canonicalRaw = invokeStaticCanonicalise(candidate);
            String canonicalOperand = ensureTrailingSlash(canonicalRaw);
            boolean startsWith = canonicalOperand.startsWith(canonical2Operand);

            System.out.println("CASE=" + printable(input));
            System.out.println("  originalVirtualPath=" + printable(input));
            System.out.println("  canonical=" + normalize(canonicalRaw));
            System.out.println("  canonical2=" + normalize(canonical2Raw));
            System.out.println("  startsWith.left=" + normalize(canonicalOperand));
            System.out.println("  startsWith.right=" + normalize(canonical2Operand));
            System.out.println("  startsWith.result=" + startsWith);

            try {
                String translated = invokeToActualPath(childMapped, input);
                System.out.println("  translatedBackingPath=" + normalize(translated));
                System.out.println("  exception=<none>");
            }
            catch (InvocationTargetException ex) {
                Throwable cause = ex.getCause();
                System.out.println("  translatedBackingPath=<rejected>");
                System.out.println("  exception=" + cause.getClass().getName() + ": " + normalize(cause.getMessage()));
            }

            String productionFlowMount;
            String productionFlowType;
            try {
                VirtualFile viaFactory = factory.getFile(input);
                productionFlowMount = viaFactory.getMount().getMount();
                productionFlowType = viaFactory.getClass().getSimpleName();
            }
            catch (Exception ex) {
                productionFlowMount = "<none>";
                productionFlowType = "EXCEPTION: " + ex.getClass().getSimpleName();
            }

            System.out.println("  productionFlow.type=" + productionFlowType);
            System.out.println("  productionFlow.mount=" + productionFlowMount);
        }
    }

    private void traceFactoryFlowCase(String input) throws Exception {
        parentRecorder.clear();
        childRecorder.clear();

        String canonicalVirtualPath = invokeCanonicalisePath(factory, input);
        String pathPassedToVirtualMappedFile = canonicalVirtualPath;
        if (!"/".equals(pathPassedToVirtualMappedFile)) {
            pathPassedToVirtualMappedFile = FileUtils.removeTrailingSlash(pathPassedToVirtualMappedFile);
        }

        VirtualFile vf = factory.getFile(input);
        String selectedMount = vf.getMount().getMount();

        String toActualResult;
        String finalBackingPath;
        if (vf instanceof VirtualMappedFile) {
            toActualResult = invokeToActualPath((VirtualMappedFile) vf, pathPassedToVirtualMappedFile);
            finalBackingPath = ((VirtualMappedFile) vf).resolveFile().getAbsolutePath();
        }
        else if (vf instanceof VirtualMountFile) {
            toActualResult = "<not-invoked: VirtualMountFile-returned>";
            finalBackingPath = ((VirtualMountFile) vf).resolveFile().getAbsolutePath();
            pathPassedToVirtualMappedFile = "<not-invoked: mount-cache-hit>";
        }
        else {
            toActualResult = "<unknown-type>";
            finalBackingPath = "<unknown-type>";
        }

        System.out.println("CASE=" + printable(input));
        System.out.println("  INPUT=" + printable(input));
        System.out.println("  CANONICAL_VIRTUAL_PATH=" + printable(canonicalVirtualPath));
        System.out.println("  SELECTED_VIRTUAL_MOUNT=" + selectedMount);
        System.out.println("  PATH_PASSED_TO_VirtualMappedFile=" + printable(pathPassedToVirtualMappedFile));
        System.out.println("  RESULT_OF_toActualPath=" + normalize(toActualResult));
        System.out.println("  FINAL_BACKING_PATH=" + normalize(finalBackingPath));
        System.out.println("  RETURNED_TYPE=" + vf.getClass().getSimpleName());
        System.out.println("  PARENT_REQUESTS=" + normalize(parentRecorder.requestedPaths.toString()));
        System.out.println("  CHILD_REQUESTS=" + normalize(childRecorder.requestedPaths.toString()));
    }

    private VirtualMappedFile childMappedAnchor() throws Exception {
        VirtualFile file = factory.getFile("/child/anchor");
        return (VirtualMappedFile) file;
    }

    private static String expectedToActualCandidate(String virtualPath, VirtualMount parentMount) {
        String adjusted = virtualPath;

        if (adjusted.equals("")) {
            adjusted = parentMount.getMount();
        }
        else if (adjusted.startsWith("./")) {
            adjusted = adjusted.replaceFirst("\\./", FileUtils.addTrailingSlash(parentMount.getMount()));
        }
        else if (!adjusted.startsWith("/")) {
            adjusted = FileUtils.addTrailingSlash(parentMount.getMount()) + adjusted;
        }

        if (adjusted.length() > parentMount.getMount().length()) {
            return FileUtils.addTrailingSlash(parentMount.getRoot())
                    + FileUtils.removeStartingSlash(adjusted.substring(parentMount.getMount().length()));
        }

        return parentMount.getRoot();
    }

    private static String ensureTrailingSlash(String value) {
        return value.endsWith("/") ? value : value + "/";
    }

    private static String printable(String value) {
        return value.replace(" ", "[SPACE]");
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        return printable(value.replace('\\', '/')
                .replace(childRootStr, "<CHILD_ROOT>")
                .replace(parentRootStr, "<PARENT_ROOT>"));
    }

    private static String invokeCanonicalisePath(VirtualFileFactory factory, String input) throws Exception {
        Method m = VirtualFileFactory.class.getDeclaredMethod("canonicalisePath", String.class);
        m.setAccessible(true);
        return (String) m.invoke(factory, input);
    }

    private static String invokeToActualPath(VirtualMappedFile file, String input) throws Exception {
        Method m = VirtualMappedFile.class.getDeclaredMethod("toActualPath", String.class);
        m.setAccessible(true);
        return (String) m.invoke(file, input);
    }

    private static String invokeStaticCanonicalise(String input) throws Exception {
        Method m = VirtualMappedFile.class.getDeclaredMethod("canonicalise", String.class);
        m.setAccessible(true);
        return (String) m.invoke(null, input);
    }

    private static NioFileFactory nio(Path home) {
        return NioFileFactoryBuilder.create().withHome(home.toFile()).withoutSandbox().build();
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
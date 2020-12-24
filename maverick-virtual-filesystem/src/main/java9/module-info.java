module com.sshtools.common.files.vfs {
	requires transitive com.sshtools.common.util;
	requires transitive com.sshtools.common.base;
	requires com.sshtools.common.logger;
	requires transitive commons.vfs2;
	exports com.sshtools.common.files.vfs;
}
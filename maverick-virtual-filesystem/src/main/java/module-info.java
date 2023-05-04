module com.sshtools.common.files.vfs {
	requires com.sshtools.common.util;
	requires transitive com.sshtools.maverick.base;
	requires com.sshtools.common.logger;
	requires transitive commons.vfs2;
	exports com.sshtools.common.files.vfs;
}
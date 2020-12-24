module com.sshtools.synergy.fuse.drive {
	exports com.sshtools.fuse;
	exports com.sshtools.fuse.fs;
	requires transitive com.sshtools.synergy.client;
	requires transitive jnr.fuse;
}
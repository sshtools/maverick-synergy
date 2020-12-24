module com.sshtools.common.publickey.bc {
	requires org.bouncycastle.pkix;
	requires transitive com.sshtools.common.base;
	requires org.bouncycastle.provider;
	exports com.sshtools.common.publickey.bc;
}
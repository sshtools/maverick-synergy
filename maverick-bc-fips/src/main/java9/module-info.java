module com.sshtools.common.publickey.bcfips {
	requires transitive com.sshtools.common.base;
	requires transitive bc.fips;
	requires transitive bcpkix.fips;
	exports com.sshtools.common.publickey.bcfips;
	
}
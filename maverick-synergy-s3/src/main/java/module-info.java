
module com.sshtools.synergy.s3 {
  requires transitive com.sshtools.maverick.base;
  requires transitive software.amazon.awssdk.awscore;
  requires transitive software.amazon.awssdk.core;
  requires transitive software.amazon.awssdk.services.s3;
  requires transitive software.amazon.awssdk.utils;
  requires transitive software.amazon.awssdk.regions;
  requires transitive software.amazon.awssdk.auth;
  exports com.sshtools.synergy.s3;
}
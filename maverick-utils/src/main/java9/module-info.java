module com.sshtools.common.util {
	exports com.sshtools.common.util;
	requires java.xml;
	
	/* TODO: Is this required? It means mavertick-utils is going to pull in JMX (now separate). For 
	 *  	 a minimal dependencies API is this worth it?
	 */
	requires java.management;
	
}
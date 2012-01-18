(defproject pallet-demos "1.0.0-SNAPSHOT"
  :description "Demonstration of various pallet features."
  :dependencies [
		[org.clojure/clojure "1.2.0"]
		[org.clojure/clojure-contrib "1.2.0"]
		 [org.cloudhoist/pallet "0.6.1"]
                 [org.cloudhoist/java "0.5.1"]
                 [org.cloudhoist/automated-admin-user "0.5.0"]]
		
  :repositories {
		"sonatype"
               "http://oss.sonatype.org/content/repositories/releases"
               "sonatype-snapshots"
               "http://oss.sonatype.org/content/repositories/snapshots"
		}
  :dev-dependencies [
		     [org.cloudhoist/pallet-lein "0.4.0"]
		     [swank-clojure/swank-clojure "1.2.1"]
                     [org.jclouds/jclouds-all "1.0.0"]
                     [org.jclouds.driver/jclouds-jsch "1.0.0"]
                     [org.jclouds.driver/jclouds-log4j "1.0.0"]
                     [log4j/log4j "1.2.14"]
                     [vmfest/vmfest "0.2.2"]])

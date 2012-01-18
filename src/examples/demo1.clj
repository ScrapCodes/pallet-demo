;;1)Demonstrating admin user with specified private and public key.
;;2)Creating a user without the help of admin-user and thus without sudoers access.
;;3)Demonstrating script run as that user.

(ns examples.demo1
 (:use [pallet.extensions :only (def-phase-fn)]
       [pallet.core :only (defnode)]
       [pallet.core :only (converge)]
       [pallet.phase :only (phase-fn)]
       [pallet.action.package :only (package)]
       [pallet.crate.automated-admin-user :only (automated-admin-user)])

 (:require [pallet.script.lib :as lib]
              [pallet.stevedore :as stevedore]
              [pallet.compute :as compute]
              [pallet.session :as session]
              [pallet.action.exec-script :as exec-script]
              [pallet.action.file :as file]
              [pallet.action.remote-file :as remote-file]
              [pallet.action.user :as user]
              [pallet.script :as script]
              [clojure.string :as string]
              [clojure.contrib.logging :as log]
              [pallet.crate.ssh-key :as ssh-key]
              [pallet.crate.java :as java]))

;;Other ways of spoiling namespace with extra stuff
(use '[pallet.resource :only [phase]])

;;Declaring Globals
(def my-user "myuser")
(def my-home "/home/myuser/")
(def my-group "myuser")


;;Some cool stuff borrowed from apache hadoop crate
(defn format-exports
  "Formats `export` lines for inclusion in a shell script."
  [& kv-pairs]
  (string/join
   (for [[k v] (partition 2 kv-pairs)]
     (format "export %s=%s\n" (name k) v))))

;; Generate the following ssh keys using the ssh-keygen utility and use the -C option
(def cluster-user
                (pallet.utils/make-user "myuser" :password "test123" :public-key-path "/path/id_rsa.pub" :private-key-path "/path/id_rsa"))

(def-phase-fn create-user
   "This is just another way of creating a user with no sudo access. Checkout sudoer crate for that matter"
   []
   (user/group "seconduser" :system true)
   (user/user "seconduser"
              :group "seconduser"
              :system true
              :create-home true
	      :password "test123"
              :shell :bash)
   (remote-file/remote-file (format "/home/%s/.bash_profile" "seconduser")
                            :owner "seconduser"
                            :group "seconduser"
                            :literal true
                            :content (format-exports
                                      :JAVA_HOME (stevedore/script (~java/java-home))
                                      :PATH (format "$PATH:/home/seconduser/bin" ))))

;;Util borrowed from hadoop crate. Set up environment and user for execution.
(script/defscript as-user [user & command])
(script/defimpl as-user :default [user & command]
  (su -s "/bin/bash" ~user
      -c "\"" (str "export JAVA_HOME=$JAVA_HOME ;") ~@command "\""))

;;Run your custom scripts as any specified user. Surely this is not an ideal approach.
(def-phase-fn scriptrun 
 "Runs the specified script"
 []
 (remote-file/remote-file (format "/home/%s/collector-downloader.sh" "seconduser")
			  :owner "seconduser"
			  :group "seconduser"
			  :literal true
			  :mode "0755" ;; There is ofcourse a better way to do this.
			  :content (stevedore/script ("curl --user username:password http://collect/collector.sh -o /home/seconduser/collector.sh ; chmod +x /home/seconduser/collector.sh")))
 (exec-script/exec-script 
   (~as-user "seconduser"
	("/home/seconduser/collector-downloader.sh")))
 (exec-script/exec-script
    (~as-user "seconduser"
	("/home/seconduser/collector.sh"))))

;;This is where we specify the phases.	
(defnode collector {:os-family :ubuntu :os-version-matches "10.04" :smallest true}
 :bootstrap (phase-fn (package "curl") (automated-admin-user "myuser" (:public-key-path cluster-user ))(java/java :jdk))
 :configure (phase-fn (create-user)(scriptrun)))

;;Specify your ec2 credentials here or use alternate approach
(def ec2-service (pallet.compute/compute-service "aws-ec2" :identity "MY_EC2_IDENTITY" :credential "MY_EC2_SECRET"))

;;Call this as (start_demonodes NO_OF_NODES_TO_START
(defn start_demonodes [n ] 
    (converge {collector n} :compute ec2-service :user cluster-user))

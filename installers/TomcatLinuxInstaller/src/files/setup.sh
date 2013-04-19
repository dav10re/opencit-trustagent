#!/bin/bash
# WARNING:
# *** do NOT use TABS for indentation, use SPACES
# *** TABS will cause errors in some linux distributions

# detect the packages we have to install
TOMCAT_PACKAGE=`ls -1 apache-tomcat*.tgz 2>/dev/null | tail -n 1`

# FUNCTION LIBRARY, VERSION INFORMATION, and LOCAL CONFIGURATION
if [ -f functions ]; then . functions; else echo "Missing file: functions"; exit 1; fi

# SCRIPT EXECUTION
if no_java ${JAVA_REQUIRED_VERSION:-1.6}; then echo "Cannot find Java ${JAVA_REQUIRED_VERSION:-1.6} or later"; exit 1; fi
tomcat_install $TOMCAT_PACKAGE

# the Tomcat "endorsed" folder is not present by default, we have to create it.
mkdir ${TOMCAT_HOME}/endorsed
cp jackson-core-asl.jar ${TOMCAT_HOME}/endorsed/
cp jackson-mapper-asl.jar ${TOMCAT_HOME}/endorsed/
cp jackson-xc.jar ${TOMCAT_HOME}/endorsed/

# on installations configured to use mysql, the customer is responsible for 
# providing the java mysql connector before starting the mt wilson installer.
# due to its GPLv2 license we cannot integrate it in any way with what we
# distribute so it cannot be even considered that our product is "based on"
# or is a "derivative work" of mysql.
# here is what the customer is supposed to execute before installing mt wilson:
# # mkdir -p /opt/intel/cloudsecurity/setup-console
# # cp mysql-connector-java-5.1.x.jar /opt/intel/cloudsecurity/setup-console
# so now we check to see if it's there, and copy it to TOMCAT so the apps
# can use it:
mysqlconnector_files=`ls -1 /opt/intel/cloudsecurity/setup-console/* | grep -i mysql`
if [[ -n "$mysqlconnector_files" ]]; then
  cp $mysqlconnector_files ${TOMCAT_HOME}/endorsed/
fi

# Add the manager role give access to the tomcat user in the tomcat-users.xml
cd $TOMCAT_CONF
mv tomcat-users.xml tomcat-users.xml.old
sed 's/<\/tomcat-users>/\n  <role rolename="manager"\/>\n  <user username="tomcat" password="tomcat" roles="manager"\/>\n<\/tomcat-users>/g' tomcat-users.xml.old > tomcat-users.xml
rm  -f tomcat-users.xml.old
#chown -R root:tomcat6 tomcat-users.xml

# release the connectors!
cat server.xml | sed '{/<!--*/ {N; /<Connector port=\"8080\"/ {N; }}}' | sed '{/-->/ {N; /<!-- A \"Connector\" using the shared thread pool-->/ {N; }}}' | sed '{/<!--*/ {N; /<Connector port=\"8443\"/ {D; }}}' | sed '{/-->/ {N;N; /<!-- Define an AJP 1.3 Connector on port 8009 -->/ {D; }}}' > server_temp.xml
mv server_temp.xml server.xml

tomcat_restart

echo "Restarting Tomcat..."

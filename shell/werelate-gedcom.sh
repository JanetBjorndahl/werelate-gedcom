#!/bin/bash
java -mx768m -Dfile.encoding=UTF-8 -server\
 -classpath ../classes:../conf:../data:\
../lib/commons-httpclient-3.1.jar:../lib/mail.jar:../lib/log4j-1.3alpha-7.jar:../lib/icu4j_3_4.jar:../lib/commons-codec-1.3.jar:\
../lib/commons-cli-1.0.jar:../lib/mysql-connector-java-5.0.4-bin.jar:../lib/commons-logging.jar\
 org.werelate.gedcom.Uploader -p ../conf/upload.properties -u


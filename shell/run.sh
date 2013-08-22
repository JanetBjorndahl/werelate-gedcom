java -Dfile.encoding=UTF-8 \
-classpath ../classes:\
../lib/commons-httpclient-3.0.jar:\
../lib/commons-cli-1.0.jar:\
../lib/icu4j_3_4.jar:\
../lib/xom-1.1b5.jar:\
../lib/mysql-connector-java-5.0.4-bin.jar:\
../lib/commons-codec-1.3.jar:\
../lib/log4j-1.3alpha-7.jar:\
../lib/junit.jar:\
../lib/commons-logging.jar \
"$@"

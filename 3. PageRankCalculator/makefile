# Customize these paths for your environment.
# -----------------------------------------------------------
hadoop.root=/home/ubuntuvikas/Programs/hadoop-2.7.3
local.input=input
jar.name=target/PageRankCalculator-0.0.1-SNAPSHOT.jar
job.name=pkg.Driver.Driver
local.output=output/

# Compiles code and builds jar.
jar:
	mvn package

# Removes local output directory.
clean-local-output:
	mvn clean
	rm -rf ${local.output}

# Runs standalone
# Make sure Hadoop  is set up (in /etc/hadoop files) for standalone operation (not pseudo-cluster).
# https://hadoop.apache.org/docs/current/hadoop-project-dist/hadoop-common/SingleCluster.html#Standalone_Operation
local: clean-local-output jar
	${hadoop.root}/bin/hadoop jar ${jar.name} ${local.input} ${local.output} ${local.output}


# Load docker image with Java
FROM openjdk:11
# Copy source files to image
COPY nl.tudelft.bartcox /home/rmi
# Copy resource files needed for execution
COPY resources/addresses_docker.txt /home/rmi/src/addresses.txt
COPY resources/my.policy /home/rmi/src/my.policy
WORKDIR /home/rmi/src
# Make sure we can connect through a port to the outside
EXPOSE 8888
# Compile the java source file
RUN javac *.java
# Bundle class files into a jar file
RUN jar -cf RMIServer.jar *.class
# Execute code with custom security policy and inject process ID (RMI_ID) as CLI argument
CMD java -Xmx4096m -Djava.security.policy=my.policy -cp RMIServer.jar RMIServer $RMI_PID

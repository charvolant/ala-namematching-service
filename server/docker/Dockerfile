FROM ubuntu:focal

ARG ENV

ARG DEBIAN_FRONTEND=noninteractive

# Install initial dependencies
RUN apt-get -y update && \
        apt-get install -y -q add-apt-key software-properties-common

ARG APT_KEY_DONT_WARN_ON_DANGEROUS_USAGE=DontWarn

# Add the apt.gbif.es repo and gpg key
RUN echo "deb [arch=amd64] https://apt.gbif.es/ bionic main" > /etc/apt/sources.list.d/ala.list && \
        apt-key adv --keyserver keyserver.ubuntu.com --recv-keys F697D8D2ADB9E24A

# Install main dependencies
RUN apt-get install -y -q openjdk-8-jdk rsync tar curl

# If we want to change the default URL over time
RUN echo "ala-namematching-service ala-namematching-service/source string https://archives.ala.org.au/archives/nameindexes/latest/namematching-20200214.tgz" | debconf-set-selections
# SHA1 of the previous file
RUN echo "ala-namematching-service ala-namematching-service/sha1 string f9e42325db8d41ca0c8afeb6aeadf670978a13ac" | debconf-set-selections

# Update the next commented date to force a new docker hub build with an updated la-pipelines version
RUN apt-get -y update && \
        apt-get install -y -q ala-namematching-service  # 2020091401

# Uncomment and edit to use locally built debian package See debian/README.md for how to build these
#COPY "./ala-namematching-service_1.1.3-SNAPSHOT+0~2020528050446.1_all.deb" /tmp
#RUN dpkg -i /tmp/ala-namematching-service_1.1.3-SNAPSHOT+0~2020528050446.1_all.deb

EXPOSE 9179
EXPOSE 9180

CMD ["java", "-jar", "/data/ala-namematching-server.jar", "server", "/data/config.yml"]

FROM ubuntu:24.04
USER root

ENV TZ=Etc/UTC
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone
RUN DEBIAN_FRONTEND="noninteractive" apt-get update -y -qq \
  && apt-get install -y -qq \
    --no-install-recommends \
    openssh-server \
    openjdk-21-jdk-headless \
    curl \
    maven \
    git \
    build-essential \
    make \
  && rm -rf /var/lib/apt/lists/*

# Ubuntu 24.04 ships with a default user 'ubuntu' that already uses UID 1000
RUN userdel -r ubuntu || true
RUN useradd --password password --shell /bin/bash --uid 1000 jenkins \
  && mkdir /home/jenkins \
  && chown -R jenkins:jenkins /home/jenkins

COPY ssh /home/jenkins/.ssh
RUN chown -R jenkins:jenkins /home/jenkins/ \
  && chmod 700 /home/jenkins/.ssh \
  && chmod 600 /home/jenkins/.ssh/*
COPY ssh /root/.ssh
RUN chown -R root:root /root/ \
  && chmod 700 /root/.ssh \
  && chmod 600 /root/.ssh/*
RUN ssh-keygen -A
COPY ssh/sshd_config /etc/ssh/sshd_config

RUN mkdir -p  /var/run/sshd

RUN echo "password\npassword" | passwd root \
  && echo "password\npassword" | passwd jenkins

EXPOSE 22

ENV PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin:/usr/lib/jvm/java-21-openjdk-amd64/bin

ENV JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64

RUN echo "PATH=${PATH}" >> /etc/environment
RUN echo "JAVA_HOME=${JAVA_HOME}" >> /etc/environment

ENTRYPOINT []
CMD [ "/bin/sh", "-c", "/usr/sbin/sshd -e -D -p 22"]



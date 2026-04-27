FROM --platform=linux/amd64 gradle:8.10.1-jdk17

USER root

RUN apt-get update && apt-get install -y \
    wget \
    unzip \
    openssh-client \
    git \
    build-essential \
    ninja-build \
    && rm -rf /var/lib/apt/lists/*


ENV ANDROID_SDK_ROOT=/opt/android-sdk
ENV ANDROID_NDK_HOME=$ANDROID_SDK_ROOT/ndk/21.4.7075529
ENV PATH=$PATH:$ANDROID_SDK_ROOT/cmdline-tools/latest/bin:$ANDROID_SDK_ROOT/platform-tools:$ANDROID_NDK_HOME

RUN mkdir -p $ANDROID_SDK_ROOT/cmdline-tools && \
    wget https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip -O cmdline-tools.zip && \
    unzip cmdline-tools.zip -d $ANDROID_SDK_ROOT/cmdline-tools && \
    mv $ANDROID_SDK_ROOT/cmdline-tools/cmdline-tools $ANDROID_SDK_ROOT/cmdline-tools/latest && \
    rm cmdline-tools.zip


RUN yes | sdkmanager --licenses && \
    sdkmanager "platforms;android-34" \
               "build-tools;34.0.0" \
               "platform-tools" \
               "cmake;3.22.1"


RUN mkdir -p /opt/android-sdk/ndk && \
    cd /opt/android-sdk/ndk && \
    wget https://dl.google.com/android/repository/android-ndk-r21e-linux-x86_64.zip && \
    unzip android-ndk-r21e-linux-x86_64.zip && \
    mv android-ndk-r21e 21.4.7075529 && \
    rm android-ndk-r21e-linux-x86_64.zip


RUN mkdir -p /opt/android-sdk/cmake/3.10.2.4988404 && \
    cd /opt/android-sdk/cmake/3.10.2.4988404 && \
    wget https://github.com/Kitware/CMake/releases/download/v3.10.2/cmake-3.10.2-Linux-x86_64.tar.gz && \
    tar -xzf cmake-3.10.2-Linux-x86_64.tar.gz --strip-components=1 && \
    rm cmake-3.10.2-Linux-x86_64.tar.gz && \
    ln -sf /usr/bin/ninja /opt/android-sdk/cmake/3.10.2.4988404/bin/ninja

RUN apt-get update && \
    apt-get install -y curl unzip && \
    curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "awscliv2.zip" && \
    unzip awscliv2.zip && \
    ./aws/install && \
    rm -rf awscliv2.zip aws /var/lib/apt/lists/*

WORKDIR /home/gradle

# COPY . .
# echo "sdk.dir=/opt/android-sdk" > local.properties
# echo "ndk.dir=/opt/android-sdk/ndk/21.4.7075529" >> local.properties
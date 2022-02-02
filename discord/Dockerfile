FROM openjdk:15-jdk

# Unpack from our `./gradlew distTar` into the docker image
RUN mkdir /discord-bot
ADD build/distributions/discord-1.0-SNAPSHOT.tar /discord-bot

# Create dir for aws credentials, required to authenticate the bot to perform ec2 actions (start/stop etc)
RUN mkdir /root/.aws

WORKDIR /discord-bot/discord-1.0-SNAPSHOT

# Move the aws credentials to correct path
RUN mv aws_credentials /root/.aws/credentials

# Run the discord bot
CMD ["bin/discord"]
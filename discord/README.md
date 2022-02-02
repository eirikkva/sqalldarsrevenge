# Discord bots

A discord bot intended to start/stop a single openra game server

Next up:
- Create an IAM user in AWS for skallagrimson and use his IAM aws keys

## Build
 - Build with gradle `.\gradlew build`

## Test
 - The build can be tested by unzipping the `discord\build\distribution\discord-*.zip` and running `discord.bat` (+.env file)
 

## Deploy
- Rebuild docker image (`/discord/Dockerfile`) `docker build -t NAME_OF_BOT .`
- Authenticate, tag and push the image to `NAME_OF_BOT` ECR-registry in AWS
- SSH to EC2 instance, pull down latest image from ECR
- Review the .env file for desired values
- Run container based on image, with .env file values for the bot `docker run --env-file .env -d --name BOT_NAME IMAGE_ID`


## Todo

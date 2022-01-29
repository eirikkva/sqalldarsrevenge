package com.sqalldarsrevenge.discord.aws

import aws.sdk.kotlin.services.ec2.Ec2Client
import aws.sdk.kotlin.services.ec2.model.*
import aws.sdk.kotlin.services.ec2.model.Tag
import aws.sdk.kotlin.services.iam.IamClient
import aws.sdk.kotlin.services.iam.model.*
import com.sqalldarsrevenge.discord.com.sqalldarsrevenge.discord.aws.AWS_REGION
import kotlin.system.exitProcess

/**
 *
 * Steps to perform for each new bot
 * 1. Create the iam user for the bot
 * 2. Add the user to the discord bot group
 * 3. Create the EC2 instance for it, with the bot tagged as the 'OwnerBot' and the correct port openings
 *
 * Install docker on the ec2 instance, upload the built bot image and run the container
 *
 */

suspend fun main(args:Array<String>) {
    if (args.size != 3) {
        exitProcess(0)
    }

    val botName = args[0]
    val policyArn = args[1]
    val amiId = args[2]

    createBotIamUser(botName)
    addBotUserToDiscordGroup(botName)
    val ec2InstanceExists = botHasExistingEc2Instance(botName)
    if(ec2InstanceExists) {
        println("Ec2 instance exists, see details from DescribeInstanceResponse")
    } else {
        println("No ec2 instance for bot exists, creating..")
        createEC2Instance(botName, amiId)
    }
}

suspend fun createBotIamUser(botName: String) {
    val request = CreateUserRequest {
        userName = botName
    }

    IamClient { region = AWS_REGION }.use { iam ->
        try {
            val response = iam.createUser(request)
            println("User created! :) \n")
            println(response.toString())

        } catch (e: EntityAlreadyExistsException) {
            println("User '${botName}' already exists, will not create")
        }
    }

}

suspend fun addBotUserToDiscordGroup(botName: String){
    val request = AddUserToGroupRequest {
        groupName = "DiscordBots"
        userName= botName
    }

    IamClient { region = AWS_REGION }.use { iam ->
        val response = iam.addUserToGroup(request)
        println(response.toString())
    }
}

suspend fun botHasExistingEc2Instance(botName: String): Boolean{
    val request = DescribeInstancesRequest {
        filters = listOf(Filter{
            name="tag:OwnerBot"
            values = listOf(botName)
        })
    }

    return Ec2Client { region = AWS_REGION}.use { ec2 ->
        val response = ec2.describeInstances(request)
        println(response.toString())
        return response.reservations?.isNotEmpty() == true
    }
}


suspend fun createEC2Instance(botName: String, amiId: String): String? {
    val request = RunInstancesRequest {
        imageId = amiId
        instanceType = InstanceType.T3Micro
        maxCount = 1
        minCount = 1
    }

    Ec2Client { region = AWS_REGION }.use { ec2 ->
        val response = ec2.runInstances(request)
        val instanceId = response.instances?.get(0)?.instanceId
        val tag = Tag{
            key = "OwnerBot"
            value = botName
        }

        val requestTags = CreateTagsRequest {
            resources = listOf(instanceId.toString())
            tags = listOf(tag)
        }
        ec2.createTags(requestTags)
        println("Successfully started EC2 Instance $instanceId based on AMI $amiId")

        return instanceId
    }
}



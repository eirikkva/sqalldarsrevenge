package com.sqalldarsrevenge.discord.aws

import aws.sdk.kotlin.services.iam.IamClient
import aws.sdk.kotlin.services.iam.model.*
import com.sqalldarsrevenge.discord.com.sqalldarsrevenge.discord.aws.AWS_REGION

/**
 * One-time setup of bot infrastructure
 *
 * 1. Create discord bot group
 * 2. Create the iam policy
 * 3. Attach the iam policy to the group
 *
 */

suspend fun main() {
    createDiscordBotsGroup()
    val policyArn = createBotOwnerPolicyForEc2Instances()
    if(policyArn != null) {
        attachBotOwnerPolicyToDiscordBotsGroup(policyArn)
    }
}

suspend fun createDiscordBotsGroup() {
    val request = CreateGroupRequest {
        groupName = "DiscordBots"
    }

    IamClient { region = AWS_REGION }.use { iam ->
        val response = iam.createGroup(request)
        println(response.toString())
    }
}

suspend fun createBotOwnerPolicyForEc2Instances(): String? {
    val request = CreatePolicyRequest {
        policyName = "OpenRAEc2InstanceWithBotOwner"
        description = "Enables aws users that are tagged as the OwnerBot to start and stop the EC2 instance. Intended for bots"
        policyDocument = """
            {
                "Version": "2012-10-17",
                "Statement": [
                    {
                        "Effect": "Allow",
                        "Action": [
                            "ec2:StartInstances",
                            "ec2:StopInstances"
                        ],
                        "Resource": "arn:aws:ec2:*:*:instance/*",
                        "Condition": {
                            "StringEquals": {
                                "aws:ResourceTag/OwnerBot": "${'$'}{aws:username}"
                            }
                        }
                    },
                    {
                        "Effect": "Allow",
                        "Action": "ec2:DescribeInstances",
                        "Resource": "*"
                    }
                ]
            }
        """.trimIndent()
    }

    return IamClient { region = AWS_REGION }.use { iam ->
        return try {
            val response = iam.createPolicy(request)
            println("Policy created! :) \n")
            println(response.policy?.toString())
            response.policy?.arn

        } catch (e: EntityAlreadyExistsException) {
            println("Policy '${request.policyName}' already exists, will not create")
            null
        }
    }
}

suspend fun attachBotOwnerPolicyToDiscordBotsGroup(arn: String) {
    val request = AttachGroupPolicyRequest {
        groupName = "DiscordBots"
        policyArn = arn
    }

    IamClient { region = AWS_REGION }.use { iam ->
        val response = iam.attachGroupPolicy(request)
        println(response.toString())
    }
}
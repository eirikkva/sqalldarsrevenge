package com.sqalldarsrevenge.discord.aws

import aws.sdk.kotlin.services.ec2.Ec2Client
import aws.sdk.kotlin.services.ec2.model.*
import aws.smithy.kotlin.runtime.client.SdkLogMode
import kotlinx.coroutines.delay

class Ec2Instance(val id: String, val status: String?, val ip: String?)

class InstanceManager(private val awsRegion: String = "eu-north-1", private val instanceId: String) {

    suspend fun startInstance(): DescribeInstancesResponse {
        val startRequest = StartInstancesRequest() {
            instanceIds = listOf(instanceId)
        }
        val statusRequest = DescribeInstanceStatusRequest() {
            instanceIds = listOf(instanceId)
            includeAllInstances = true
        }

        return Ec2Client { region = awsRegion }.use { ec2 ->
            val start = ec2.startInstances(startRequest)
            var status = ec2.describeInstanceStatus(statusRequest)
            var currentState = getStateFromStatusResponse(status)?.name?.value ?: "unknown"

            while (currentState != "running" && currentState != "unknown") {
                delay(3000)
                status = ec2.describeInstanceStatus(statusRequest)
                currentState = getStateFromStatusResponse(status)?.name?.value ?: "unknown"
                println(currentState)
            }
            println("Server is now running!")

            return describeInstance()
        }
    }

    suspend fun stopInstance(): StopInstancesResponse {
        val request = StopInstancesRequest() {
            instanceIds = listOf(instanceId)
        }
        return Ec2Client { region = awsRegion }.use { ec2 ->
            val stop = ec2.stopInstances(request)
            println("Server is now stopping")
            return stop
        }
    }

    suspend fun rebootInstance(): RebootInstancesResponse {
        val request = RebootInstancesRequest() { instanceIds = listOf(instanceId) }
        return Ec2Client { region = awsRegion }.use { ec2 -> return ec2.rebootInstances(request) }
    }

    suspend fun checkInstance(): InstanceState? {
        val request = DescribeInstanceStatusRequest() {
            instanceIds = listOf(instanceId)
            includeAllInstances = true
        }
        return Ec2Client {
            region = awsRegion
            sdkLogMode = SdkLogMode.LogRequestWithBody + SdkLogMode.LogResponseWithBody
        }.use { ec2 ->
            val status = ec2.describeInstanceStatus(request)
            return getStateFromStatusResponse(status)
        }

    }

    suspend fun describeInstance(): DescribeInstancesResponse {
        val request = DescribeInstancesRequest() {
            instanceIds = listOf(instanceId)
        }
        return Ec2Client { region = awsRegion }.use { ec2 ->
            return ec2.describeInstances(request)
        }
    }

    suspend fun describeEc2Instance(): Ec2Instance {
        val request = DescribeInstancesRequest() {
            instanceIds = listOf(instanceId)
        }
        return Ec2Client { region = awsRegion }.use { ec2 ->
            return mapInstanceDescriptionToEc2Instance(ec2.describeInstances(request))
        }
    }

    private fun mapInstanceDescriptionToEc2Instance(description: DescribeInstancesResponse): Ec2Instance {
        val ip =
            description.reservations?.map { r -> r.instances?.find { i -> i.instanceId == instanceId }?.networkInterfaces?.first()?.association?.publicIp }
                ?.first()

        return Ec2Instance(instanceId, null, ip)

    }

    private fun getStateFromStatusResponse(instanceStatusResponse: DescribeInstanceStatusResponse): InstanceState? {
        return instanceStatusResponse.instanceStatuses?.first()?.instanceState
    }

}
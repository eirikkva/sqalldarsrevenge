# OpenRA Server

## How this is configured

1. An AWS EC2 Amazon Linux instance is created (Free tier - micro)
2. Install, start and enable docker to start after reboot
    ```
   sudo yum update -y
   sudo amazon-linux-extras install docker
   sudo service docker start
   sudo systemctl enable docker
   # Add ec2-user in docker group to execute Docker commands without using sudo
   sudo usermod -a -G docker ec2-user
   # Verify no sudo
   docker info
   ```
3. The openRA image is pulled down (https://github.com/rmoriz/openra-dockerfile)
4. A container named `openra` is created by running:
    `` docker run -d -p 1234:1234 -e Name="SERVER NAME" -e Mod=ra -e ListenPort="1234" -e AdvertiseOnline="True" -e Password="SOME_PASSWORD" -e RecordReplays="True" --name openra rmoriz/openra``
5. Then docker and the container is configured to run on startup by
   - Creating a `docker_boot.service`:
      ```
     [Unit]
      Description=docker boot
      After=docker.service 
     [Service]
     Type=simple
     Restart=always
     RestartSec=1
     User=ec2-user
     ExecStart=/usr/bin/docker start openra
     [Install]
     WantedBy=multi-user.target
     ```
   
   - Enabling start after reboot and starting it
     ```
     sudo systemctl enable docker_boot.service
     sudo systemctl start docker_boot.service
     ```

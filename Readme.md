In this project we will transmit some data from One node to another using a 
reliable data transfer protocol.

To give a good analogy of nodes, we have BaseStation node and a Lander Node.
We need to get the data from Lander to our base station. The Lander can be considered as a server
and BaseStation as client. 

The base station can have 2 commands which it can send to lander.

1) ls : this will get the list of files available at lander to base station.
2) get <filename> this will get the file from lander to base station.

###Steps to execute the program.

1) Build the lander.
docker build -f Lander.Dockerfile -t lander . 

2) Build the base station
docker build -f BaseStation.Dockerfile -t basestation .

3)Create NodeNetwork (Do it once)
docker network create --subnet=172.18.0.0/16 nodenet

4) Run the lander.
docker run -it -p 8080:8080 -v <Path you want to make available for docker container>:/var/log/data --cap-add=NET_ADMIN --net nodenet --ip 172.18.0.21 lander

5) Run the base station.
docker run -it -p 8081:8080 -v <Path you want to make available for docker container>:/var/log/data --cap-add=NET_ADMIN --net nodenet --ip 172.18.0.22 basestation 172.18.0.21

you can provide either of 2 commands.
1) ls
2 get <filename> 
Note: the file must be available at Server to list all the files run ls command.


Note: The server will always remain on.
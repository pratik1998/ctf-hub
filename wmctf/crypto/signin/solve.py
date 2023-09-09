import socket

HOST = '1.13.101.243'
PORT = 25228

s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
s.connect((HOST, PORT))

# Read data until we get the challenge
data  = ""
while True:
    data += s.recv(1024).decode('utf-8')
    print(data)

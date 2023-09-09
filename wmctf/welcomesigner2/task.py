from Crypto.Util.number import *
from Crypto.Cipher import AES
from hashlib import md5
import random

flag = b"WMCTF{Welcome_to_the_World_of_Crypto!}"
# flag = b"7"
def pad(message):
    return message + b"\x00"*((16-len(message)%16)%16)


def myfastexp(m,d,N,j,N_):
    A = 1
    B = m
    d = bin(d)[2:][::-1]
    n = len(d)
    N = N
    for i in range(n):
        print(f"A: {A}, B: {B}, N: {N}, N_: {N_}, i: {i}, d[i]: {d[i]}")
        if d[i] == '1':
            A = A * B % N
        #  a fault occurs j steps before the end of the exponentiation
        if i >= n-1-j:
            N = N_
        B = B**2 % N
    return A


def encrypt(message,key):
    key = bytes.fromhex(md5(str(key).encode()).hexdigest())
    enc = AES.new(key,mode=AES.MODE_ECB)
    c   = enc.encrypt(pad(message))
    return c


def introduce_fault(n, byte, index):
    n_ = n ^ (int(byte)<<int(index))
    return n_

def get_inverse(leaked_data, message, length, n, n_):
    print(f"leaked_data: {leaked_data}, message: {message}, length: {length}, n: {n}, n_: {n_}")
    B = message
    inverse_data = 1
    for i in range(length):
        if i + len(leaked_data) >= length-2:
            n = n_
        if i + len(leaked_data) > length-1:
            if leaked_data[i - length + len(leaked_data)] == '1':
                inverse_data = (inverse_data * inverse(B, n)) % n
                print(f"In Inverse: i: {i}, B: {B}, inverse_data: {inverse_data}, n: {n}, n_: {n_}")
        print(f"i: {i}, B: {B}, inverse_data: {inverse_data}, n: {n}, n_: {n_}")
        B = B**2 % n
    return inverse_data

def leaked_mod(leaked_data, generator, n_):
    print(f"leaked_data: {leaked_data}, generator: {generator}, n_: {n_}")
    inverse_data = 1
    for i in range(len(leaked_data)):
        generator = generator**2 % n_
        if leaked_data[i] == '1':
           inverse_data = (inverse_data * inverse(generator, n_)) % n_
    return inverse_data


border = "|"
print(border*75)
print(border, "Hi all, I have another algorithm that can quickly calculate powers. ", border)
print(border, "But still there's something wrong with it. Your task is to get      ", border)
print(border, "its private key,and decrypt the cipher to cat the flag ^-^          ", border)
print(border*75)


while True:
# generate
    p = getPrime(512)
    q = getPrime(512)
    # p = 19
    # q = 23
    n = p*q
    e = 17
    if GCD(e,(p-1)*(q-1)) == 1:
        d = inverse(e,(p-1)*(q-1))
        n_ = n
        print(border,f"n = {n}")
        print(border,f"p = {p}")
        print(border,f"q = {q}")
        print(border,f"d = {d}, len(d) = {len(bin(d)[2:])}")
        print(border,f"public key = ({e},{n})")
        break
n_ = n
# msg = bytes_to_long(b"\x07")
msg = bytes_to_long(b"Welcome_come_to_WMCTF")
print(f"message: {msg}, N: {n}")
sig = pow(msg,d,n)
assert sig == myfastexp(msg,d,n,0,n_)
CHANGE = True
# while True:
#     try:
#         ans = input("| Options: \n|\t[G]et data \n|\t[S]ignatrue \n|\t[F]ault injection \n|\t[Q]uit\n").lower().strip()
        
#         if ans == 'f':
#             if CHANGE:
#                 print(border,"You have one chance to change one byte of N. ")
#                 temp,index = input("bytes, and index:").strip().split(",")
#                 assert 0<= int(temp) <=255
#                 assert 0<= int(index) <= 1023 
#                 n_ = n ^ (int(temp)<<int(index))
#                 print(border,f"[+] update: n_ -> \"{n_}\"")
#                 CHANGE = False
#             else:
#                 print(border,"Greedy...")
#         elif ans == 'g':
#             print(border,f"n = {n}")
#             print(border,f"flag_ciphertext = {encrypt(flag,d).hex()}")
#         elif ans == 's':
#             index = input("Where your want to interfere:").strip()
#             sig_ = myfastexp(msg,d,n,int(index),n_)
#             print(border,f"signature of \"Welcome_come_to_WMCTF\" is {sig_}")
#         elif ans == 'q':
#             quit()
#     except Exception as e:
#         print(border,"Err...")
#         quit()

print(f"isPrime(n): {isPrime(n)}")
n_ = introduce_fault(n, 4, 0)
for index in range(1024):
    for byte in range(64):
        n_ = introduce_fault(n, byte, index)
        print(f"n_: {n_} byte {byte} at {index}")
        if isPrime(n_):
            break
print(f"n_: {n_}")
assert isPrime(n_)

ans = ""
length = 1024
for i in range(length):
    print(f"Leaking index: {i}")
    sig1 = myfastexp(msg, d, n, i, n_)
    print(f"sig1: {sig1}")
    sig2 = myfastexp(msg, d, n, i+1, n_)
    print(f"sig2: {sig2}")
    generator = 1
    if length - i - 2 > 0:
        generator = pow(msg, 2**(length - i - 2), n)
    print(f"generator: {generator}")
    inverse_data1 = leaked_mod(ans, generator * generator % n, n_)
    print(f"inverse_data1: {inverse_data1}")
    inverse_data2 = leaked_mod(ans, generator * generator % n_, n_)
    print(f"inverse_data2: {inverse_data2}")
    print(f"sig1 * inverse_data1 % n_: {sig1 * inverse_data1}")
    print(f"sig2 * inverse_data2 % n_: {sig2 * inverse_data2}")
    if sig1 * inverse_data1 % n_ == sig2 * inverse_data2 % n_:
        ans = '0' + ans
    else:
        ans = '1' + ans
    print(f"leaked_key: {ans}")
print(f"d: {d}")
print(f"leaked key: {ans}")

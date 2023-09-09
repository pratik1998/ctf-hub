from Crypto.PublicKey import RSA
from Crypto.Util.number import bytes_to_long
from binascii import hexlify

from secret import flag

from math import gcd

def egcd(a, b):
  if a == 0:
    return (b, 0, 1)
  else:
    g, y, x = egcd(b % a, a)
    return (g, x - (b // a) * y, y)

def modinv(a, m):
  g, x, y = egcd(a, m)
  if g != 1:
    raise ValueError('Modular inverse does not exist.')
  else:
    return x % m

def attack(c1, c2, e1, e2, N):
  exponent_gcd = gcd(e1, e2)
  if exponent_gcd != 1:
    raise ValueError("Exponents e1 and e2 must be coprime", exponent_gcd)
  s1 = modinv(e1,e2)
  s2 = (gcd(e1,e2) - e1 * s1) / e2
  temp = modinv(c2, N)
  m1 = pow(c1,s1,N)
  m2 = pow(temp,-s2,N)
  return (m1 * m2) % N

key = RSA.generate(2048)
print("Key:", key.e)
print("N:", key.n)

key1 = RSA.import_key(open('key1.pem','rb').read())
key2 = RSA.import_key(open('key2.pem','rb').read())

print("Key1:", key1.e)
print("N:", key1.n)
print("Key2:", key2.e)
print("N:", key2.n)
print("Message:", bytes_to_long(flag))

c1 = pow(bytes_to_long(flag), key1.e, key1.n)
c2 = pow(bytes_to_long(flag), key2.e, key2.n)

print("GCD:", egcd(key1.e, key1.n))
print("GCD:", egcd(key2.e, key2.n))

# d1 = modinv(key1.e, key1.n)
# d2 = modinv(key2.e, key2.n)

# message1 = pow(c1, d1, key1.n)
# message2 = pow(c2, d2, key2.n)
# print("Message1:", message1)
# print("Message2:", message2)

writer = open('new_ciphers','w')
writer.write('%d\n%d' % (c1, c2))

# print('Plaintext:', attack(c1, c2, key1.e, key2.e, key1.n))

writer.close()


# Key1: ENO{L13333333333
# Key2: 7_super_duper_ok
# Key3: _lolnullconctf!_
# Key4: you_solved_it!!}
# Key: ENO{L133333333337_super_duper_ok_lolnullconctf!_you_solved_it!!}
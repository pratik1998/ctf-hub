#!/usr/bin/env python3

from Crypto.Cipher import AES
import os, sys
import collections

# img_in = open(sys.argv[1], "rb").read()
# img_in += b'\00' * (16 - (len(img_in) % 16))
# cipher = AES.new(os.urandom(16), AES.MODE_ECB)
# img_out = cipher.encrypt(img_in)
# open(sys.argv[1] + ".enc", "wb+").write(img_out)

# Path: bmpass/decrypt.py
img_out = open("flag.bmp.enc", "rb").read()
count = collections.defaultdict(int)
for i in range(0, int(len(img_out)/16)):
    count[img_out[i*16:(i+1)*16]] += 1
for k, v in count.items():
    if v > 50:
        print(k, v)
# print(len(img_out))

# ENO{I_c4N_s33_tHr0ugh_3ncrYpt10n}
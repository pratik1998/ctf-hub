from Crypto.Util.number import *

N = 136041789523519894898576794725012761543729722271406024757357999896674266657064488507875299351478021443830817852071392427294939801433109398446052143231033249782006877267485706053661124763078574717728198180757454357516155435454698228900460940490672631839080722488134466066601149101929854044755499735365033200271
e = 17
cipher_text = "16118508703f6b2122ebfa8a593efd7d7f90f1c774f512dd48bf2e4e078f153daefbcdec1f128eb6223450e2caa5549c"

def get_index_and_fault_byte(msg, n):
    for index in range(1024):
        for byte in range(256):
            n_ = n ^ (int(byte)<<int(index))
            # print(f"n_: {n_}")
            if msg  % n_ == 1:
                return index, byte
    return None, None


message = bytes_to_long(b"Welcome_come_to_WMCTF")
print(f"message: {message}, N: {N}")
for i in range(1, 100):
    new_msg = pow(message, i, N)
    print(f"i: {i}, message: {new_msg}")
    index, byte = get_index_and_fault_byte(new_msg * message, N)
    if index is not None:
        break
print(f"index: {index}, byte: {byte}")
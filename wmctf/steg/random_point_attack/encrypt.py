# Encrypt data using Steganography to protect against the random point attack
#
# This script is a modified version of the script from the Steganography

import numpy as np
from PIL import Image

class Solution:
  def encrypt(self, img, key):
      img = Image.open(img)
      width, height = img.size
      pixel_map = img.load()

      key += '$' # Add a delimiter to the key
      key_index = 0
      for y in range(height):
          for x in range(width):
              pixel = list(pixel_map[x, y])
              if key_index < len(key):
                  new_pixel = [ord(key[key_index])] * len(pixel)
                  pixel_map[x, y] = tuple(new_pixel)
                  key_index += 1
              else:
                  break
      return img
  
  def encrypt_chat_gpt(self, img, key):
      img = Image.open(img)
      width, height = img.size
      pixel_map = img.load()

      char_index = 0

      for y in range(height):
          for x in range(width):
              if char_index < len(key):
                  pixel = list(pixel_map[x, y])
                  for color_channel in range(3):  # RGB channels
                      pixel[color_channel] &= 0xFE  # Clear the least significant bit
                      print(key[char_index])
                      pixel[color_channel] |= int(format(ord(key[char_index]), '08b')) & 0x01
                      char_index += 1
                  pixel_map[x, y] = tuple(pixel)
              else:
                  break
          if char_index >= len(key):
              break

  def decrypt(self, img):
      img = Image.open(img)
      width, height = img.size
      pixel_map = img.load()
      key = ''
      for y in range(height):
          for x in range(width):
              pixel = list(pixel_map[x, y])
              char_value = np.bincount(pixel).argmax()
              if char_value == 36: # '$'
                  return key
              key += chr(char_value)
      return key
  
  def decrypt_chat_gpt(self, img):
      img = Image.open(img)
      width, height = img.size
      pixel_map = img.load()

      decoded_message = ""

      for y in range(height):
          for x in range(width):
              pixel = pixel_map[x, y]
              char_value = 0
              for color_channel in range(3):  # RGB channels
                  char_value |= (pixel[color_channel] & 0x01) << color_channel
              decoded_message += chr(char_value)
      
      return decoded_message
    

def main():
    img = 'flag.png'
    key = 'flag{this_is_a_fake_flag}'
    solution = Solution()
    img = solution.encrypt(img, key)
    encrypted_img = 'encrypted.png'
    img.save(encrypted_img)
    key = solution.decrypt(encrypted_img)
    print(key)

if __name__ == '__main__':
    main()